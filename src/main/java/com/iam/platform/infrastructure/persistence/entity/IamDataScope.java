package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据范围字典实体（iam_data_scope）。
 *
 * <p>全公司 / 本分公司 / 本部门 / 本项目 / 本人。
 */
@Data
@Entity
@Table(name = "iam_data_scope")
public class IamDataScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "scope_code", nullable = false, length = 32)
    private String scopeCode;

    @Column(name = "scope_name", nullable = false, length = 64)
    private String scopeName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
