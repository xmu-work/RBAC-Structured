package com.iam.platform.interfaces;

import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.openapi.OpenApiAuthenticator;
import com.iam.platform.interfaces.openapi.OpenApiAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 开放 API（/open/**）认证拦截器。
 *
 * <p>携带 {@code X-App-Id} 时按接入方签名认证（可访问任意 userId）；
 * 否则回退为普通用户令牌认证，仅允许访问本人（见 {@link OpenApiAuth#requireUserScope}）。
 */
@Component
@RequiredArgsConstructor
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_APP_ID = "X-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final OpenApiAuthenticator openApiAuthenticator;
    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String appId = request.getHeader(HEADER_APP_ID);
        if (appId == null || appId.isBlank()) {
            Long userId = authService.authenticate(bearerToken(request.getHeader("Authorization")));
            request.setAttribute(AuthInterceptor.ATTR_USER_ID, userId);
            request.setAttribute(OpenApiAuth.ATTR_MODE, OpenApiAuth.MODE_SELF);
            return true;
        }

        openApiAuthenticator.verify(appId,
                request.getHeader(HEADER_TIMESTAMP),
                request.getHeader(HEADER_NONCE),
                request.getHeader(HEADER_SIGNATURE));
        request.setAttribute(OpenApiAuth.ATTR_MODE, OpenApiAuth.MODE_SYSTEM);
        request.setAttribute(OpenApiAuth.ATTR_APP_ID, appId);
        return true;
    }

    private String bearerToken(String authorization) {
        return AuthInterceptor.bearerToken(authorization);
    }
}