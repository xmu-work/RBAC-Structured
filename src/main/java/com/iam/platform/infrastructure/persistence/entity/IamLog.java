package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一日志实体（iam_log）。
 *
 * <p>普通操作日志和权限变更日志共用一张只追加表，通过 {@code log_type} 区分。
 * 权限变更日志额外保存变更前后快照，便于完整追溯。
 */
@Data
@Entity
@Table(name = "iam_log")
public class IamLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /** ACTION：普通操作；PERMISSION_CHANGE：权限变更 */
    @Column(name = "log_type", nullable = false, length = 32)
    private String logType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 128)
    private String username;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "operate_time", nullable = false)
    private LocalDateTime operateTime;

    @Column(name = "success", nullable = false, columnDefinition = "tinyint")
    private Integer success;

    @Column(name = "target", length = 255)
    private String target;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "system_code", length = 64)
    private String systemCode;

    @Column(name = "detail", columnDefinition = "json")
    private String detail;

    @Column(name = "before_snapshot", columnDefinition = "json")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", columnDefinition = "json")
    private String afterSnapshot;

    @Column(name = "decision_id", length = 64)
    private String decisionId;

    @Column(name = "request_id", length = 64)
    private String requestId;
}
