package com.iam.platform.application.auth;

import lombok.Data;

/**
 * 登录响应 DTO。
 */
@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String userId;
    private String displayName;
    private String accountStatus;
}
