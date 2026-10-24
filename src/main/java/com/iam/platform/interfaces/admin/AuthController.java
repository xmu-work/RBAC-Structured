package com.iam.platform.interfaces.admin;

import com.iam.platform.application.auth.AuthService;
import com.iam.platform.application.auth.LoginRequest;
import com.iam.platform.application.auth.LoginResponse;
import com.iam.platform.common.result.Result;
import com.iam.platform.interfaces.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（管理端）。
 *
 * <p>对应 API：/api/v1/auth/*
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 账号密码登录。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 登出。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(AuthInterceptor.bearerToken(authorization));
        return Result.success();
    }

    /**
     * 令牌校验。
     */
    @GetMapping("/validate")
    public Result<Boolean> validate(@RequestHeader("Authorization") String authorization) {
        return Result.success(authService.validateToken(AuthInterceptor.bearerToken(authorization)));
    }
}
