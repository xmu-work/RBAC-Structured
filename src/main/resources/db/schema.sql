-- ============================================================
-- 统一应用软件权限管理系统（IAM）数据库建表脚本
-- MySQL 8.0+  utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------
-- 1. 组织机构表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_org (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    org_code     VARCHAR(64)  NOT NULL COMMENT '组织编码',
    org_name     VARCHAR(128) NOT NULL COMMENT '组织名称',
    org_type     VARCHAR(32)  NOT NULL COMMENT '组织类型：COMPANY/Branch/DEPARTMENT/PROJECT',
    org_level    INT          NOT NULL COMMENT '层级（1=总公司 2=分公司 3=部门 4=项目部）',
    parent_id    BIGINT       NULL     COMMENT '上级组织ID',
    ancestors    VARCHAR(512) NULL     COMMENT '祖先路径，逗号分隔',
    sort_no      INT          DEFAULT 0 COMMENT '排序号',
    status       VARCHAR(16)  DEFAULT 'ACTIVE' COMMENT '状态',
    is_deleted   TINYINT      DEFAULT 0 COMMENT '软删除',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构';

-- ----------------------------------------------------------
-- 2. 用户表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_no        VARCHAR(32)  NOT NULL COMMENT '账号（登录名）',
    password_hash  VARCHAR(128) NOT NULL COMMENT '密码 PBKDF2 哈希（盐:哈希）',
    name_enc       VARCHAR(512) NOT NULL COMMENT '姓名密文',
    name_hash      VARCHAR(64)  NULL     COMMENT '姓名哈希（检索用）',
    phone_enc      VARCHAR(512) NULL     COMMENT '手机号密文',
    phone_hash     VARCHAR(64)  NULL     COMMENT '手机号哈希',
    org_id         BIGINT       NOT NULL COMMENT '所属组织ID',
    post           VARCHAR(64)  NULL     COMMENT '岗位',
    account_status VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED/SEALED/LOCKED',
    leave_time     DATETIME     NULL     COMMENT '离职时间',
    last_login_at  DATETIME     NULL     COMMENT '最近登录时间',
    is_deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by     BIGINT       NULL,
    updated_by     BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_user_no (user_no),
    KEY idx_user_org (org_id),
    KEY idx_user_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ----------------------------------------------------------
-- 3. 角色表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_type   VARCHAR(16)  NOT NULL COMMENT 'FUNC=功能角色 / DATA=岗位型数据角色',
    parent_id   BIGINT       NULL     COMMENT '父角色（RBAC1 继承，RBAC0 不使用）',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500) NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_role_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ----------------------------------------------------------
-- 4. 角色互斥表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role_exclusion (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    role_id         BIGINT       NOT NULL COMMENT '角色ID',
    excluded_role_id BIGINT      NOT NULL COMMENT '互斥角色ID',
    description     VARCHAR(256) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_excluded (excluded_role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色互斥';

-- ----------------------------------------------------------
-- 5. 用户-角色关联表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_user_role (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    role_id        BIGINT       NOT NULL,
    effective_time DATETIME     NULL     COMMENT '生效时间',
    expire_time    DATETIME     NULL     COMMENT '失效时间，NULL 表示长期',
    grant_by       BIGINT       NULL     COMMENT '授权人',
    grant_reason   VARCHAR(255) NULL     COMMENT '授权原因',
    status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/REVOKED',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_ur_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- ----------------------------------------------------------
-- 6. 接入子系统表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_system (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    system_code  VARCHAR(64)  NOT NULL COMMENT '子系统编码',
    system_name  VARCHAR(128) NOT NULL COMMENT '子系统名称',
    callback_url VARCHAR(500) NULL,
    public_key   TEXT         NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    description  VARCHAR(500) NULL,
    is_deleted   TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_code (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接入子系统';

-- ----------------------------------------------------------
-- 7. 模块表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_module (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    system_id   BIGINT       NOT NULL,
    module_code VARCHAR(64)  NOT NULL,
    module_name VARCHAR(128) NOT NULL,
    sort_no     INT          NOT NULL DEFAULT 0,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_module_code (system_id, module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块';

-- ----------------------------------------------------------
-- 8. 功能点表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_function_point (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    module_id   BIGINT       NOT NULL,
    point_code  VARCHAR(64)  NOT NULL COMMENT '功能点编码',
    point_name  VARCHAR(128) NOT NULL COMMENT '功能点名称',
    action      VARCHAR(64)  NOT NULL COMMENT '操作取值（12 种之一）',
    api_path    VARCHAR(255) NULL     COMMENT 'API 映射模板，如 /users/{userId}',
    api_method  VARCHAR(16)  NULL     COMMENT 'HTTP 方法',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fp_code (module_id, point_code),
    UNIQUE KEY uk_fp_api (api_path, api_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能点';

-- ----------------------------------------------------------
-- 9. 角色-功能关联表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role_function (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    role_id           BIGINT      NOT NULL,
    function_point_id BIGINT      NOT NULL,
    allowed_action    VARCHAR(64) NOT NULL COMMENT '授予的操作（12 种之一）',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_fp (role_id, function_point_id),
    KEY idx_rf_fp (function_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-功能关联';

-- ----------------------------------------------------------
-- 10. 数据范围表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_data_scope (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    scope_code    VARCHAR(64)  NOT NULL,
    scope_name    VARCHAR(128) NOT NULL,
    scope_type    VARCHAR(32)  NOT NULL COMMENT 'ALL/BRANCH/DEPT/PROJECT/CUSTOM',
    custom_org_ids VARCHAR(512) NULL     COMMENT '自定义组织ID列表，逗号分隔',
    description   VARCHAR(256) NULL,
    is_deleted    TINYINT      DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scope_code (scope_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据范围';

-- ----------------------------------------------------------
-- 11. 角色-数据范围关联表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role_data_scope (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    role_id    BIGINT   NOT NULL,
    scope_id   BIGINT   NOT NULL,
    granted_by VARCHAR(64) NULL,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT  DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_scope (scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据范围关联';

-- ----------------------------------------------------------
-- 12. 令牌表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_token (
    token_id    VARCHAR(64)  NOT NULL COMMENT '令牌唯一标识',
    user_id     BIGINT       NOT NULL,
    system_code VARCHAR(64)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL COMMENT '令牌哈希（不存明文）',
    issued_at   DATETIME     NOT NULL,
    expire_at   DATETIME     NOT NULL,
    revoked     TINYINT      NOT NULL DEFAULT 0,
    revoked_at  DATETIME     NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    client_ip   VARCHAR(64)  NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_token_hash (token_hash),
    KEY idx_token_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='令牌';

-- ----------------------------------------------------------
-- 13. 审计日志表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NULL,
    user_no       VARCHAR(64)  NULL,
    system_code   VARCHAR(64)  NULL,
    action        VARCHAR(32)  NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(64)  NULL,
    resource_id   VARCHAR(64)  NULL,
    detail        TEXT         NULL,
    client_ip     VARCHAR(64)  NULL,
    success       TINYINT      DEFAULT 1,
    error_msg     VARCHAR(512) NULL,
    operated_at   DATETIME     NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_action (action),
    KEY idx_operated (operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

-- ----------------------------------------------------------
-- 14. 权限变更日志表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_permission_change_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    change_type   VARCHAR(32)  NOT NULL COMMENT 'GRANT/REVOKE/MODIFY',
    target_type   VARCHAR(32)  NOT NULL COMMENT 'USER_ROLE/ROLE_FUNC/ROLE_SCOPE',
    target_id     VARCHAR(64)  NULL,
    before_value  TEXT         NULL,
    after_value   TEXT         NULL,
    operator_id   BIGINT       NULL,
    operator_no   VARCHAR(64)  NULL,
    reason        VARCHAR(256) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限变更日志';

SET FOREIGN_KEY_CHECKS = 1;
