package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-功能权限实体（iam_role_function）。
 *
 * <p>角色对功能点的操作授权。allowed_action 取 12 种操作取值之一。
 */
@Data
@Entity
@Table(name = "iam_role_function")
public class IamRoleFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "function_point_id", nullable = false)
    private Long functionPointId;

    /** 授予的操作（12 种取值之一） */
    @Column(name = "allowed_action", nullable = false, length = 64)
    private String allowedAction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}
