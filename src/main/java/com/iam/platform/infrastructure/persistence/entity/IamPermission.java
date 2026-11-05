package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前端权限实体（iam_permission）。
 *
 * <p>菜单和按钮共用一张树表：MENU 节点负责路由与可见性，BUTTON 节点负责页面操作。
 * 权限不保存 HTTP 路径；后端 API 由 {@link IamApi} 独立建模并归属到本实体。
 */
@Data
@Entity
@Table(name = "iam_permission")
public class IamPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "permission_code", nullable = false, length = 64)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 128)
    private String permissionName;

    /** MENU 或 BUTTON */
    @Column(name = "permission_type", nullable = false, length = 16)
    private String permissionType;

    /** 菜单路由；按钮可为空 */
    @Column(name = "route_path", length = 255)
    private String routePath;

    /** 按钮或菜单要求的操作取值 */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint")
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
