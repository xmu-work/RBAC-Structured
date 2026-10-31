package com.iam.platform.interfaces.admin;

import com.iam.platform.application.audit.AuditService;
import com.iam.platform.common.result.PageResult;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamAuditLog;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 审计日志查询控制器（管理端）。
 *
 * <p>对应 API：/api/v1/audit-logs/*
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * 分页查询审计日志。
     */
    @GetMapping
    public Result<PageResult<IamAuditLog>> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) String systemCode,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Page<IamAuditLog> result = auditService.queryLogs(
                PageRequest.of(Math.max(page, 1) - 1, size), userId, systemCode, startTime, endTime);
        return Result.success(PageResult.of(result.getTotalElements(), page, size, result.getContent()));
    }
}