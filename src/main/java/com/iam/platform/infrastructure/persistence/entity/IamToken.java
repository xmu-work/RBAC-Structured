package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 令牌实体（iam_token）。
 *
 * <p>SSO 令牌，含吊销状态。封存/离职时吊销令牌，业务系统下次鉴权立即失败。
 */
@Data
@Entity
@Table(name = "iam_token")
public class IamToken {

    @Id
    @Column(name = "token_id", length = 64)
    private String tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "system_code", nullable = false, length = 64)
    private String systemCode;

    /** 令牌哈希（不存明文） */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "revoked", nullable = false)
    private Integer revoked;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** ACTIVE / EXPIRED / REVOKED */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
