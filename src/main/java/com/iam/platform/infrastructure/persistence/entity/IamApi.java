package com.iam.platform.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后端 API 实体（iam_api）。
 *
 * <p>API 是服务端最终鉴权对象，归属于一个前端权限。一个菜单/按钮权限可对应多个 API，
 * 且鉴权拦截器按 HTTP 方法 + 映射模板精确定位 API。
 */
@Data
@Entity
@Table(name = "iam_api")
public class IamApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    /**
     * 所属前端权限。未映射时保持为空，运行期按 fail-closed 拒绝。
     */
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "api_code", nullable = false, length = 64)
    private String apiCode;

    @Column(name = "api_name", nullable = false, length = 128)
    private String apiName;

    @Column(name = "api_path", nullable = false, length = 255)
    private String apiPath;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "tinyint")
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
