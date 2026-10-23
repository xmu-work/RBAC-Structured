package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限变更快照实体（iam_permission_change_log）。
 *
 * <p>只追加，记录变更前后 JSON 快照，支持追溯。
 */
@Data
@Entity
@Table(name = "iam_permission_change_log")
public class IamPermissionChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_id")
    private Long changeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id")
    private Long roleId;

    /** GRANT / REVOKE / SEAL / ROLE_UPDATE / SCOPE_UPDATE */
    @Column(name = "change_type", nullable = false, length = 32)
    private String changeType;

    @Column(name = "before_snapshot", columnDefinition = "json")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", columnDefinition = "json")
    private String afterSnapshot;

    @Column(name = "operator")
    private Long operator;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "remark", length = 500)
    private String remark;
}
