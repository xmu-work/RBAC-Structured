package com.iam.platform.application.auth;

import com.iam.platform.common.enums.AccountStatus;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.crypto.CryptoComponent;
import com.iam.platform.infrastructure.persistence.entity.IamToken;
import com.iam.platform.infrastructure.persistence.entity.IamUser;
import com.iam.platform.infrastructure.persistence.repository.IamTokenRepository;
import com.iam.platform.infrastructure.persistence.repository.IamUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * 统一认证应用服务（M01）。
 *
 * <p>登录认证、令牌签发/续期/吊销、防重放。
 * 对应 FR-31～FR-34。
 *
 * <p>结构化编程原则：过程内聚——登录→校验账号状态→签发令牌→记录日志，
 * 按固定时序编排，各步处理不同数据。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    /** 管理后台自身的系统编码，令牌归属于该系统 */
    public static final String ADMIN_SYSTEM_CODE = "ADMIN";

    private static final String TOKEN_ACTIVE = "ACTIVE";
    private static final String TOKEN_REVOKED = "REVOKED";

    private final IamUserRepository userRepository;
    private final IamTokenRepository tokenRepository;
    private final CryptoComponent cryptoComponent;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expire:7200}")
    private long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:604800}")
    private long refreshTokenExpire;

    @Value("${jwt.issuer:iam-platform}")
    private String issuer;

    /**
     * 账号密码登录。
     *
     * <p>流程：校验参数 → 查找账号并核对密码 → 校验状态 → 签发令牌 → 记录令牌与登录时间 → 返回。
     * 账号不存在与密码错误返回同一错误，避免账号枚举。
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "账号和密码不能为空");
        }

        IamUser user = userRepository.findByUserNo(request.getUsername())
                .filter(u -> u.getIsDeleted() == 0)
                .filter(u -> cryptoComponent.verifyPassword(request.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "账号或密码错误"));

        validateAccountStatus(user.getAccountStatus());

        String accessToken = generateToken(user.getId(), user.getUserNo(), accessTokenExpire);
        saveToken(user.getId(), accessToken, request.getClientIp());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(accessTokenExpire);
        response.setUserId(String.valueOf(user.getId()));
        response.setDisplayName(cryptoComponent.decrypt(user.getNameEnc()));
        response.setAccountStatus(user.getAccountStatus());
        log.info("登录成功: userId={}", user.getId());
        return response;
    }

    /**
     * 登出，吊销令牌。令牌不存在时视为已登出。
     */
    @Transactional
    public void logout(String accessToken) {
        tokenRepository.findByTokenHash(cryptoComponent.hash(accessToken)).ifPresent(this::revoke);
    }

    /**
     * 校验令牌有效性。
     */
    public boolean validateToken(String token) {
        try {
            authenticate(token);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * 认证令牌并返回用户 ID。
     *
     * <p>流程：校验签名与有效期 → 校验令牌未被吊销。
     *
     * @throws BusinessException TOKEN_INVALID / TOKEN_EXPIRED / TOKEN_REVOKED
     */
    public Long authenticate(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey()).requireIssuer(issuer).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        IamToken record = tokenRepository.findByTokenHash(cryptoComponent.hash(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        if (record.getRevoked() == 1) {
            throw new BusinessException(ErrorCode.TOKEN_REVOKED);
        }
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 吊销用户的全部有效令牌（账号停用/封存时调用）。
     */
    @Transactional
    public void revokeUserTokens(Long userId) {
        tokenRepository.findByUserIdAndStatus(userId, TOKEN_ACTIVE).forEach(this::revoke);
    }

    /**
     * 自助修改密码：校验原密码 → 校验新密码强度 → 更新哈希 → 吊销全部旧令牌。
     *
     * <p>对应《目前项目的问题》P0-7：项目原先没有改密接口，种子管理员使用固定初始密码。
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "原密码和新密码不能为空");
        }
        validatePasswordStrength(newPassword);

        IamUser user = userRepository.findById(userId)
                .filter(u -> u.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在: " + userId));
        if (!cryptoComponent.verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "原密码错误");
        }

        user.setPasswordHash(cryptoComponent.hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        revokeUserTokens(userId);
        log.info("修改密码: userId={}", userId);
    }

    private void validatePasswordStrength(String password) {
        boolean strong = password.length() >= 8 && password.length() <= 64
                && password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
        if (!strong) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码需为 8-64 位并同时包含字母和数字");
        }
    }

    private void validateAccountStatus(String status) {
        if (AccountStatus.SEALED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.ACCOUNT_SEALED);
        }
        if (!AccountStatus.NORMAL.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private String generateToken(Long userId, String userNo, long expireSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("userNo", userNo)
                .issuer(issuer)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(signingKey())
                .compact();
    }

    private void saveToken(Long userId, String accessToken, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        IamToken token = new IamToken();
        token.setTokenId(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setSystemCode(ADMIN_SYSTEM_CODE);
        token.setTokenHash(cryptoComponent.hash(accessToken));
        token.setIssuedAt(now);
        token.setExpireAt(now.plusSeconds(accessTokenExpire));
        token.setRevoked(0);
        token.setStatus(TOKEN_ACTIVE);
        token.setClientIp(clientIp);
        token.setCreatedAt(now);
        tokenRepository.save(token);
    }

    private void revoke(IamToken token) {
        token.setRevoked(1);
        token.setRevokedAt(LocalDateTime.now());
        token.setStatus(TOKEN_REVOKED);
        tokenRepository.save(token);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
