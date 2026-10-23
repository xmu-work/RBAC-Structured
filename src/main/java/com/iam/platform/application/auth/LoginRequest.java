package com.iam.platform.application.auth;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录请求 DTO。
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String captchaId;
    private String captchaCode;
    private String clientIp;
}
