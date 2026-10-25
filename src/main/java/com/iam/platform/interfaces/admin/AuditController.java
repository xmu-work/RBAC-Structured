package com.iam.platform.interfaces.admin;

import com.iam.platform.application.audit.AuditService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 审计日志查询控制器（管理端）。
 *
 * <p>对应 API：/api/v1/audit-logs/*
 */
@Slf4j
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * 分页查询审计日志。
     */
    @GetMapping
    public Result<Page<IamAuditLog>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) String systemCode,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        throw new UnsupportedOperationException("待实现");
    }
}
