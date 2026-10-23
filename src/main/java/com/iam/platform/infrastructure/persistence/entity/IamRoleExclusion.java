package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色互斥实体（iam_role_exclusion）。
 *
 * <p>互斥角色不能同时授予同一用户（如会计与出纳操作互斥）。
 */
@Data
@Entity
@Table(name = "iam_role_exclusion")
public class IamRoleExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "excluded_role_id", nullable = false)
    private Long excludedRoleId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}
