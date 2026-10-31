package com.iam.platform.interfaces.openapi;

import com.iam.platform.application.decision.DecisionResult;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.DataScopeView;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.application.decision.ResourceContext;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
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
 * <p>提供令牌校验、功能权限判定、数据范围与带资源上下文的鉴权。
 * 接入方签名可查询任意用户；用户令牌仅可查询本人（见 {@link OpenApiAuth#requireUserScope}）。
 */
@Slf4j
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenApiController {

    /** 单次批量鉴权功能点数量上限 */
    private static final int MAX_BATCH_FUNCTION_POINTS = 200;

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
        if (body.getFunctionPointIds() != null && body.getFunctionPointIds().size() > MAX_BATCH_FUNCTION_POINTS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "批量功能点数量超过上限 " + MAX_BATCH_FUNCTION_POINTS);
        }
        return Result.success(decisionService.checkPermissions(
                body.getUserId(), body.getSystemCode(), body.getFunctionPointIds()));
    }

    /**
     * 带资源上下文的功能+数据权限判定（本人/本部门/本项目）。
     */
    @PostMapping("/authorize/resource")
    public Result<DecisionResult> authorizeResource(HttpServletRequest request,
                                                    @RequestBody ResourceCheckRequest body) {
        OpenApiAuth.requireUserScope(request, body.getUserId());
        ResourceContext context = new ResourceContext(
                body.getTargetUserId(), body.getTargetOrgId(), body.getTargetProjectId());
        return Result.success(decisionService.decide(
                body.getUserId(), body.getSystemCode(), body.getFunctionPointId(), context));
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

    /**
     * 查询用户在某子系统下的有效数据范围。
     */
    @GetMapping("/data-scope")
    public Result<List<DataScopeView>> dataScope(HttpServletRequest request,
                                                 @RequestParam Long userId,
                                                 @RequestParam String systemCode) {
        OpenApiAuth.requireUserScope(request, userId);
        return Result.success(decisionService.resolveDataScopes(userId, systemCode));
    }

    @Data
    public static class BatchCheckRequest {
        private Long userId;
        private String systemCode;
        private List<Long> functionPointIds;
    }

    @Data
    public static class ResourceCheckRequest {
        private Long userId;
        private String systemCode;
        private Long functionPointId;
        private Long targetUserId;
        private Long targetOrgId;
        private Long targetProjectId;
    }
}