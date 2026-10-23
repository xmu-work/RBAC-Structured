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
    user_no        VARCHAR(64)  NOT NULL COMMENT '账号',
    name_enc       VARBINARY(255) NOT NULL COMMENT '姓名密文',
    name_hash      VARCHAR(128) NOT NULL COMMENT '姓名哈希（检索用）',
    phone_enc      VARBINARY(255) NULL     COMMENT '手机号密文',
    phone_hash     VARCHAR(128) NULL     COMMENT '手机号哈希',
    org_id         BIGINT       NULL     COMMENT '所属组织ID',
    post           VARCHAR(64)  NULL     COMMENT '岗位',
    account_status VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED/SEALED/LOCKED',
    leave_time     DATETIME     NULL     COMMENT '离职时间',
    last_login_at  DATETIME     NULL     COMMENT '最近登录时间',
    is_deleted     TINYINT      DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_no (user_no),
    KEY idx_org (org_id),
    KEY idx_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ----------------------------------------------------------
-- 3. 角色表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_type   VARCHAR(32)  NOT NULL COMMENT 'FUNCTION/DATA_POST',
    description VARCHAR(256) NULL,
    status      VARCHAR(16)  DEFAULT 'ACTIVE',
    is_deleted  TINYINT      DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
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
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    role_id    BIGINT   NOT NULL,
    granted_by VARCHAR(64) NULL,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_at  DATETIME NULL COMMENT '授权过期时间',
    is_deleted TINYINT  DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- ----------------------------------------------------------
-- 6. 接入子系统表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_system (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    system_code VARCHAR(64)  NOT NULL COMMENT '子系统编码',
    system_name VARCHAR(128) NOT NULL COMMENT '子系统名称',
    description VARCHAR(256) NULL,
    status      VARCHAR(16)  DEFAULT 'ACTIVE',
    is_deleted  TINYINT      DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_code (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接入子系统';

-- ----------------------------------------------------------
-- 7. 模块表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_module (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    system_code VARCHAR(64)  NOT NULL,
    module_code VARCHAR(64)  NOT NULL,
    module_name VARCHAR(128) NOT NULL,
    parent_id   BIGINT       NULL,
    status      VARCHAR(16)  DEFAULT 'ACTIVE',
    is_deleted  TINYINT      DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_system (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块';

-- ----------------------------------------------------------
-- 8. 功能点表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_function_point (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    system_code     VARCHAR(64)  NOT NULL,
    module_id       BIGINT       NOT NULL,
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(32)  NOT NULL COMMENT 'MENU/BUTTON/API',
    http_method     VARCHAR(16)  NULL,
    uri             VARCHAR(256) NULL,
    status          VARCHAR(16)  DEFAULT 'ACTIVE',
    is_deleted      TINYINT      DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (system_code, permission_code),
    KEY idx_module (module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能点';

-- ----------------------------------------------------------
-- 9. 角色-功能关联表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role_function (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    role_id         BIGINT       NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    granted_by      VARCHAR(64)  NULL,
    granted_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT      DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_perm (permission_code)
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
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    token_id   VARCHAR(64)  NOT NULL COMMENT '令牌唯一标识',
    user_id    BIGINT       NOT NULL,
    system_code VARCHAR(64) NOT NULL,
    token_hash VARCHAR(128) NOT NULL COMMENT '令牌哈希',
    issued_at  DATETIME     NOT NULL,
    expire_at  DATETIME     NOT NULL,
    revoked    TINYINT      DEFAULT 0,
    revoked_at DATETIME     NULL,
    status     VARCHAR(16)  DEFAULT 'ACTIVE',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_hash (token_hash),
    KEY idx_user (user_id)
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
