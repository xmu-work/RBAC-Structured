package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体（iam_role）。
 *
 * <p>功能角色 / 岗位型数据角色。parent_id 表示角色继承（子角色继承父角色权限）。
 */
@Data
@Entity
@Table(name = "iam_role")
public class IamRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_code", nullable = false, length = 64)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 128)
    private String roleName;

    /** FUNC=功能角色 / DATA=岗位型数据角色 */
    @Column(name = "role_type", nullable = false, length = 16)
    private String roleType;

    /** 父角色（继承），根为 NULL */
    @Column(name = "parent_id")
    private Long parentId;

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

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
