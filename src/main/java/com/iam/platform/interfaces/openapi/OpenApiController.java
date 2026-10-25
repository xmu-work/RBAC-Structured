package com.iam.platform.interfaces.openapi;

import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 开放 API 控制器（供接入子系统/网关调用）。
 *
 * <p>对应 API：/api/v1/open/*
 * <p>提供令牌校验、功能权限判定、权限列表查询。
 */
@Slf4j
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenApiController {

    private final DecisionService decisionService;

    /**
     * 功能权限判定（单点）。
     */
    @GetMapping("/authorize")
    public Result<Boolean> authorize(@RequestParam Long userId,
                                     @RequestParam String systemCode,
                                     @RequestParam Long functionPointId) {
        return Result.success(decisionService.hasPermission(userId, systemCode, functionPointId));
    }

    /**
     * 批量功能权限判定。
     */
    @PostMapping("/authorize/batch")
    public Result<PermissionCheckResult> authorizeBatch(@RequestBody BatchCheckRequest request) {
        return Result.success(decisionService.checkPermissions(
                request.getUserId(), request.getSystemCode(), request.getFunctionPointIds()));
    }

    /**
     * 查询用户在某子系统下已授权的功能点 ID 集合。
     */
    @GetMapping("/permissions")
    public Result<Set<Long>> permissions(@RequestParam Long userId,
                                         @RequestParam String systemCode) {
        return Result.success(decisionService.getUserFunctionPoints(userId, systemCode));
    }

    @Data
    public static class BatchCheckRequest {
        private Long userId;
        private String systemCode;
        private List<Long> functionPointIds;
    }
}
