package com.iam.platform.interfaces.openapi;

import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
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
 * 接入方签名可查询任意用户；用户令牌仅可查询本人（见 {@link OpenApiAuth#requireUserScope}）。
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
    public Result<Boolean> authorize(HttpServletRequest request,
                                     @RequestParam Long userId,
                                     @RequestParam String systemCode,
                                     @RequestParam Long functionPointId) {
        OpenApiAuth.requireUserScope(request, userId);
        return Result.success(decisionService.hasPermission(userId, systemCode, functionPointId));
    }

    /**
     * 批量功能权限判定。
     */
    @PostMapping("/authorize/batch")
    public Result<PermissionCheckResult> authorizeBatch(HttpServletRequest request,
                                                        @RequestBody BatchCheckRequest body) {
        OpenApiAuth.requireUserScope(request, body.getUserId());
        return Result.success(decisionService.checkPermissions(
                body.getUserId(), body.getSystemCode(), body.getFunctionPointIds()));
    }

    /**
     * 查询用户在某子系统下已授权的功能点 ID 集合。
     */
    @GetMapping("/permissions")
    public Result<Set<Long>> permissions(HttpServletRequest request,
                                         @RequestParam Long userId,
                                         @RequestParam String systemCode) {
        OpenApiAuth.requireUserScope(request, userId);
        return Result.success(decisionService.getUserFunctionPoints(userId, systemCode));
    }

    @Data
    public static class BatchCheckRequest {
        private Long userId;
        private String systemCode;
        private List<Long> functionPointIds;
    }
}