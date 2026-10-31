package com.iam.platform.application.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.platform.infrastructure.persistence.entity.IamAuditLog;
import com.iam.platform.infrastructure.persistence.repository.IamAuditLogRepository;
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

/**
 * 审计日志应用服务（M09）。
 *
 * <p>统一记录操作日志、查询审计记录。
 * 对应 FR-27～FR-30、FR-44。
 *
 * <p>结构化编程原则：记录操作为线性写入流程，异步执行不阻塞主流程。
 * 操作人、来源一律取自服务端上下文（见 {@code AuthInterceptor#afterCompletion}），
 * 不接受客户端传入，避免审计归属被伪造。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AuditService {

    private final IamAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录操作日志（异步）。
     */
    @Async
    @Transactional
    public void record(Long userId, String username, String systemCode, String action, String target,
                       String detail, String clientIp, boolean success, String errorMsg) {
        IamAuditLog entry = new IamAuditLog();
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setSystemCode(systemCode);
        entry.setAction(action);
        entry.setTarget(target);
        entry.setDetail(buildDetail(detail, errorMsg));
        entry.setIp(clientIp);
        entry.setSuccess(success ? 1 : 0);
        entry.setOperateTime(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

    /**
     * 分页查询审计日志（按操作人/系统/时间段过滤，参数为空则不作为条件）。
     */
    public Page<IamAuditLog> queryLogs(Pageable pageable, Long userId, String systemCode,
                                       LocalDateTime startTime, LocalDateTime endTime) {
        Specification<IamAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (systemCode != null && !systemCode.isBlank()) {
                predicates.add(cb.equal(root.get("systemCode"), systemCode));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("operateTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("operateTime"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable);
    }

    /**
     * 查询用户操作历史。
     */
    public Page<IamAuditLog> getUserHistory(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    private String buildDetail(String detail, String errorMsg) {
        Map<String, String> map = new LinkedHashMap<>();
        if (detail != null && !detail.isBlank()) {
            map.put("detail", detail);
        }
        if (errorMsg != null && !errorMsg.isBlank()) {
            map.put("error", errorMsg);
        }
        if (map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("审计详情序列化失败: {}", e.getMessage());
            return null;
        }
    }
}