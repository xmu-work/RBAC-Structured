package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体（iam_audit_log）。
 *
 * <p>只追加、永久保存、不可删除。记录每次后端鉴权与操作。
 */
@Data
@Entity
@Table(name = "iam_audit_log")
public class IamAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 128)
    private String username;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "operate_time", nullable = false)
    private LocalDateTime operateTime;

    @Column(name = "success", nullable = false)
    private Integer success;

    @Column(name = "target", length = 255)
    private String target;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "system_code", length = 64)
    private String systemCode;

    @Column(name = "detail", columnDefinition = "json")
    private String detail;

    @Column(name = "decision_id", length = 64)
    private String decisionId;
}
