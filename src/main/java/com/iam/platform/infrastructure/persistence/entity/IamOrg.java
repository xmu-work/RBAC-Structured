package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织机构实体（iam_org）。
 *
 * <p>多级树：总公司→分公司→部门→项目经理部。
 * parent_id 自关联表示父子层级；ancestors 存祖先路径便于子树查询。
 */
@Data
@Entity
@Table(name = "iam_org")
public class IamOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "ancestors", nullable = false, length = 500)
    private String ancestors;

    @Column(name = "org_code", nullable = false, length = 64)
    private String orgCode;

    @Column(name = "org_name", nullable = false, length = 128)
    private String orgName;

    @Column(name = "org_type", nullable = false, length = 16)
    private String orgType;

    @Column(name = "org_level", nullable = false, columnDefinition = "tinyint")
    private Integer orgLevel;

    @Column(name = "leader_user_id")
    private Long leaderUserId;

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

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
