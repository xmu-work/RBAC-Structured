package com.iam.platform.interfaces.openapi;

import com.iam.platform.application.api.ApiService;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.decision.PermissionCheckResult;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 开放 API 控制器（供接入子系统/网关调用）。
 *
 * <p>后端按 API 独立鉴权：{@code /authorize} 接收 API ID，内部映射到前端权限，
 * 避免把前端按钮 ID 当作后端接口 ID。
 */
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenApiController {

    private static final int MAX_BATCH_APIS = 200;

    private final DecisionService decisionService;
    private final ApiService apiService;

    @GetMapping("/authorize")
    public Result<Boolean> authorize(HttpServletRequest request,
                                     @RequestParam Long userId,
                                     @RequestParam String systemCode,
                                     @RequestParam Long apiId) {
        OpenApiAuth.requireUserScope(request, userId);
        IamApi api = apiService.getApi(apiId);
        return Result.success(api.getPermissionId() != null
                && decisionService.hasPermission(userId, systemCode, api.getPermissionId()));
    }

    @PostMapping("/authorize/batch")
    public Result<PermissionCheckResult> authorizeBatch(HttpServletRequest request,
                                                        @RequestBody BatchCheckRequest body) {
        OpenApiAuth.requireUserScope(request, body.getUserId());
        if (body.getApiIds() == null || body.getApiIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "API ID 列表不能为空");
        }
        if (body.getApiIds().size() > MAX_BATCH_APIS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "批量 API 数量超过上限 " + MAX_BATCH_APIS);
        }
        PermissionCheckResult result = new PermissionCheckResult();
        for (Long apiId : body.getApiIds()) {
            IamApi api = apiService.getApi(apiId);
            boolean allowed = api.getPermissionId() != null
                    && decisionService.hasPermission(body.getUserId(), body.getSystemCode(), api.getPermissionId());
            result.getResults().put(apiId, allowed);
        }
        return Result.success(result);
    }

    @GetMapping("/permissions")
    public Result<Set<Long>> permissions(HttpServletRequest request,
                                         @RequestParam Long userId,
                                         @RequestParam String systemCode) {
        OpenApiAuth.requireUserScope(request, userId);
        return Result.success(decisionService.getUserPermissions(userId, systemCode));
    }

    @Data
    public static class BatchCheckRequest {
        private Long userId;
        private String systemCode;
        private List<Long> apiIds;
    }
}
