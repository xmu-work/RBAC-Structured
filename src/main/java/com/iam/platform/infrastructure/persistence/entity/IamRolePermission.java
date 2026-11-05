package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-前端权限授权（iam_role_permission）。
 *
 * <p>角色只直接绑定菜单/按钮权限；后端 API 权限由 API 所属权限间接推导。
 */
@Data
@Entity
@Table(name = "iam_role_permission")
public class IamRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "allowed_action", nullable = false, length = 64)
    private String allowedAction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}
