package com.iam.platform.interfaces.openapi;

import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.interfaces.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 开放 API 访问上下文与访问范围校验。
 *
 * <p>两种认证模式：
 * <ul>
 *   <li>接入方签名（{@code OpenApiAuthInterceptor} 校验通过）→ 可访问任意 userId；</li>
 *   <li>普通用户令牌 → 只能访问本人 userId（自助）。</li>
 * </ul>
 */
public final class OpenApiAuth {

    public static final String ATTR_MODE = "iam.open.mode";
    public static final String ATTR_APP_ID = "iam.open.appId";
    public static final String MODE_SYSTEM = "SYSTEM";
    public static final String MODE_SELF = "SELF";

    private OpenApiAuth() {
    }

    /**
     * 校验目标 userId 是否在当前调用者可访问范围内。
     * 接入方签名模式放行；用户令牌模式仅允许本人。
     */
    public static void requireUserScope(HttpServletRequest request, Long targetUserId) {
        if (MODE_SYSTEM.equals(request.getAttribute(ATTR_MODE))) {
            return;
        }
        Long currentUserId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        if (currentUserId == null || !currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.NO_FUNCTION_PERM, "开放 API 查询他人权限需接入方签名认证");
        }
    }
}