package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色绑定实体（iam_user_role）。
 *
 * <p>含临时授权有效期（effective_time / expire_time）。
 */
@Data
@Entity
@Table(name = "iam_user_role")
public class IamUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    /** 失效时间；NULL 表示长期 */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "grant_by")
    private Long grantBy;

    @Column(name = "grant_reason", length = 255)
    private String grantReason;

    /** ACTIVE / EXPIRED / REVOKED */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 乐观锁版本号 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
