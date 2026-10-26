package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务接入系统实体（iam_system）。
 *
 * <p>18 套在跑业务系统登记。新增第 19 套系统时在此登记即可。
 */
@Data
@Entity
@Table(name = "iam_system")
public class IamSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "system_code", nullable = false, length = 64)
    private String systemCode;

    @Column(name = "system_name", nullable = false, length = 128)
    private String systemName;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "public_key", columnDefinition = "text")
    private String publicKey;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint")
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
