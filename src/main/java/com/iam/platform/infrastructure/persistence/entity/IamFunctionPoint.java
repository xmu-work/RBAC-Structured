package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 功能点实体（iam_function_point）。
 *
 * <p>约 359 个最小可授权单元。api_path + api_method 是后端鉴权依据。
 */
@Data
@Entity
@Table(name = "iam_function_point")
public class IamFunctionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Column(name = "point_code", nullable = false, length = 64)
    private String pointCode;

    @Column(name = "point_name", nullable = false, length = 128)
    private String pointName;

    /** 操作取值集合（12 种之一） */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "api_path", length = 255)
    private String apiPath;

    @Column(name = "api_method", length = 16)
    private String apiMethod;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint")
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
