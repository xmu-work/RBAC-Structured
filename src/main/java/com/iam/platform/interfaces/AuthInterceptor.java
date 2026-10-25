package com.iam.platform.interfaces;

import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.permission.FunctionPermissionService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 认证与鉴权拦截器。
 *
 * <p>流程：取 Bearer 令牌 → 认证得到用户 ID（失败 401）→
 * 若当前 API（映射模板 + HTTP 方法）已注册为 ADMIN 系统的功能点，则判定用户是否拥有该功能点（无权 403）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求属性：当前登录用户 ID */
    public static final String ATTR_USER_ID = "iam.userId";

    private final AuthService authService;
    private final FunctionPermissionService functionPermissionService;
    private final DecisionService decisionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Long userId = authService.authenticate(bearerToken(request.getHeader("Authorization")));
        request.setAttribute(ATTR_USER_ID, userId);

        // ponytail: 未注册为功能点的 API 只要求登录（fail-open，便于初始化）；上线前应为全部管理端 API 注册功能点
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        functionPermissionService.findByApi(pattern, request.getMethod()).ifPresent(fp -> {
            if (!decisionService.hasPermission(userId, AuthService.ADMIN_SYSTEM_CODE, fp.getId())) {
                throw new BusinessException(ErrorCode.NO_FUNCTION_PERM, "无功能权限: " + fp.getPointName());
            }
        });
        return true;
    }

    /**
     * 从 Authorization 请求头中取出 Bearer 令牌，缺失时抛出 UNAUTHENTICATED。
     */
    public static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return authorization.substring(7);
    }
}
