package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号/员工实体（iam_user）。
 *
 * <p>10000 名员工统一账号。姓名、电话加密存储（name_enc/phone_enc），
 * 另存 hash 用于等值检索。account_status 对应封存（离职禁用但保留账号）。
 */
@Data
@Entity
@Table(name = "iam_user")
public class IamUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_no", nullable = false, length = 32)
    private String userNo;

    /** 登录密码 PBKDF2 哈希（格式：盐:哈希），不存明文 */
    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    /** 加密姓名（密文） */
    @Column(name = "name_enc", nullable = false, length = 512)
    private String nameEnc;

    /** 姓名 HMAC-SHA256，用于等值检索 */
    @Column(name = "name_hash", length = 64)
    private String nameHash;

    /** 加密电话（密文） */
    @Column(name = "phone_enc", length = 512)
    private String phoneEnc;

    /** 电话 HMAC-SHA256，用于等值检索 */
    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "post", length = 64)
    private String post;

    /** 状态：NORMAL/DISABLED/SEALED/LOCKED */
    @Column(name = "account_status", nullable = false, length = 16)
    private String accountStatus;

    @Column(name = "leave_time")
    private LocalDateTime leaveTime;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint")
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 乐观锁版本号 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
