package com.iam.platform.interfaces.admin;

import com.iam.platform.application.log.LogService;
import com.iam.platform.common.result.PageResult;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamLog;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 统一日志查询控制器。普通操作日志和权限变更日志均从 {@code /logs} 查询。
 */
@Validated
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    public Result<PageResult<IamLog>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        Page<IamLog> result = logService.queryLogs(
                PageRequest.of(Math.max(page, 1) - 1, size, Sort.by(Sort.Direction.DESC, "operateTime")),
                userId, systemCode, logType, startTime, endTime);
        return Result.success(PageResult.of(result.getTotalElements(), page, size, result.getContent()));
    }
}
