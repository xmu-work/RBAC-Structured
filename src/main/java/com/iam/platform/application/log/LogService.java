package com.iam.platform.application.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamLog;
import com.iam.platform.infrastructure.persistence.repository.IamLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一日志服务。
 *
 * <p>普通操作审计和权限变更日志统一写入 {@code iam_log}。权限变更使用独立的
 * {@code logType=PERMISSION_CHANGE}，并保存前后快照；普通请求使用 {@code ACTION}。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LogService {

    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_PERMISSION_CHANGE = "PERMISSION_CHANGE";
    private static final Set<String> LOG_TYPES = Set.of(TYPE_ACTION, TYPE_PERMISSION_CHANGE);

    private final IamLogRepository logRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录普通请求操作日志（异步，不阻塞业务请求）。
     */
    @Async
    @Transactional
    public void record(Long userId, String username, String systemCode, String action, String target,
                       String detail, String clientIp, boolean success, String errorMsg,
                       String requestId) {
        IamLog entry = new IamLog();
        entry.setLogType(TYPE_ACTION);
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setSystemCode(systemCode);
        entry.setAction(action);
        entry.setTarget(target);
        entry.setDetail(buildDetail(detail, errorMsg));
        entry.setIp(clientIp);
        entry.setSuccess(success ? 1 : 0);
        entry.setRequestId(requestId);
        entry.setOperateTime(LocalDateTime.now());
        logRepository.save(entry);
    }

    /**
     * 记录权限变更日志，保存前后快照。
     */
    @Transactional
    public void recordPermissionChange(Long operatorId, String systemCode, String action,
                                       String target, Object before, Object after,
                                       String clientIp, String requestId) {
        IamLog entry = new IamLog();
        entry.setLogType(TYPE_PERMISSION_CHANGE);
        entry.setUserId(operatorId);
        entry.setSystemCode(systemCode);
        entry.setAction(action);
        entry.setTarget(target);
        entry.setBeforeSnapshot(toJson(before));
        entry.setAfterSnapshot(toJson(after));
        entry.setIp(clientIp);
        entry.setSuccess(1);
        entry.setRequestId(requestId);
        entry.setOperateTime(LocalDateTime.now());
        logRepository.save(entry);
    }

    /**
     * 查询统一日志，可按操作人、系统、日志类型与时间范围筛选。
     */
    public Page<IamLog> queryLogs(Pageable pageable, Long userId, String systemCode, String logType,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        String normalizedType = logType == null || logType.isBlank() ? null : logType.toUpperCase();
        if (normalizedType != null && !LOG_TYPES.contains(normalizedType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "日志类型必须为 ACTION 或 PERMISSION_CHANGE");
        }
        Specification<IamLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (systemCode != null && !systemCode.isBlank()) {
                predicates.add(cb.equal(root.get("systemCode"), systemCode));
            }
            if (normalizedType != null) {
                predicates.add(cb.equal(root.get("logType"), normalizedType));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("operateTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("operateTime"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return logRepository.findAll(spec, pageable);
    }

    public Page<IamLog> getUserHistory(Long userId, Pageable pageable) {
        return logRepository.findByUserId(userId, pageable);
    }

    private String buildDetail(String detail, String errorMsg) {
        Map<String, String> map = new LinkedHashMap<>();
        if (detail != null && !detail.isBlank()) {
            map.put("detail", detail);
        }
        if (errorMsg != null && !errorMsg.isBlank()) {
            map.put("error", errorMsg);
        }
        return map.isEmpty() ? null : toJson(map);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("日志快照序列化失败: {}", e.getMessage());
            return null;
        }
    }
}
