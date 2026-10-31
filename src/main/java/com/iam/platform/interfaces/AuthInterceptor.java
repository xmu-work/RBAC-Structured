package com.iam.platform.interfaces;

import com.iam.platform.application.audit.AuditService;
import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.decision.DecisionService;
import com.iam.platform.application.permission.FunctionPermissionService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Set;

/**
 * 认证与鉴权拦截器。
 *
 * <p>默认拒绝（fail-closed）：除显式白名单外，请求对应的 API（映射模板 + HTTP 方法）
 * 必须已登记为功能点，且当前用户拥有该功能点，否则一律拒绝。
 *
 * <p>流程：取 Bearer 令牌 → 认证得到用户 ID（失败 401）→ 放行白名单 →
 * 查找功能点（查不到即拒绝）→ 判定用户是否拥有该功能点（无权 403）。
 *
 * <p>审计：写操作在 {@link #afterCompletion} 中按服务端上下文记录操作人，客户端无法伪造归属。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求属性：当前登录用户 ID */
    public static final String ATTR_USER_ID = "iam.userId";

    /** 仅需登录、无需功能点登记的接口（登出、令牌校验、自助改密） */
    private static final Set<String> LOGIN_ONLY_PATTERNS = Set.of("/auth/logout", "/auth/validate", "/auth/password");

    /** 开放 API 前缀：见 {@code OpenApiAuthInterceptor}，在此不做管理端功能点校验 */
    private static final String OPEN_API_PREFIX = "/open/";

    /** 非管理端基础设施路径（监控、健康检查、错误页），不参与功能点校验与审计 */
    private static final Set<String> INFRA_PREFIXES = Set.of("/actuator", "/druid", "/error", "/favicon.ico");

    private final AuthService authService;
    private final FunctionPermissionService functionPermissionService;
    private final DecisionService decisionService;
    private final AuditService auditService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Long userId = authService.authenticate(bearerToken(request.getHeader("Authorization")));
        request.setAttribute(ATTR_USER_ID, userId);

        String path = relativePath(request);
        if (isLoginOnly(path) || isInfra(path)) {
            return true;
        }

        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        IamFunctionPoint fp = functionPermissionService.findByApi(pattern, request.getMethod())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_FUNCTION_PERM,
                        "接口未登记为功能点，默认拒绝: " + request.getMethod() + " " + path));
        if (!decisionService.hasPermission(userId, AuthService.ADMIN_SYSTEM_CODE, fp.getId())) {
            throw new BusinessException(ErrorCode.NO_FUNCTION_PERM, "无功能权限: " + fp.getPointName());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod) || !isMutating(request.getMethod())) {
            return;
        }
        Long userId = (Long) request.getAttribute(ATTR_USER_ID);
        String path = relativePath(request);
        if (userId == null || isInfra(path)) {
            return;
        }
        boolean success = ex == null && response.getStatus() < 400;
        auditService.record(userId, null, AuthService.ADMIN_SYSTEM_CODE,
                request.getMethod() + " " + path,
                path,
                request.getQueryString(),
                request.getRemoteAddr(),
                success,
                ex == null ? null : ex.getMessage());
    }

    private String relativePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri == null) {
            return "";
        }
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
    }

    private boolean isLoginOnly(String path) {
        return path.startsWith(OPEN_API_PREFIX) || LOGIN_ONLY_PATTERNS.contains(path);
    }

    private boolean isInfra(String path) {
        return INFRA_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isMutating(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method);
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