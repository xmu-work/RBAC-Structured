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
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id      BIGINT        NULL     COMMENT '上级组织；根节点为 NULL',
    ancestors      VARCHAR(500)  NOT NULL DEFAULT '0' COMMENT '祖先路径，逗号分隔 ID，如 0,1,12，便于子树查询',
    org_code       VARCHAR(64)   NOT NULL COMMENT '组织编码',
    org_name       VARCHAR(128)  NOT NULL COMMENT '组织名称',
    org_type       VARCHAR(16)   NOT NULL COMMENT '类型：HQ 总公司 / BRANCH 分公司 / DEPT 部门 / PROJECT 项目部',
    org_level      TINYINT       NOT NULL COMMENT '层级：1 总公司 / 2 分公司 / 3 部门 / 4 项目部',
    leader_user_id BIGINT        NULL     COMMENT '负责人',
    sort_no        INT           NOT NULL DEFAULT 0 COMMENT '同级排序',
    status         VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 正常 / DISABLED 停用',
    is_deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标记',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version        BIGINT        NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by     BIGINT        NULL     COMMENT '创建人',
    updated_by     BIGINT        NULL     COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_org_parent (parent_id),
    KEY idx_org_ancestors (ancestors),
    CONSTRAINT fk_org_parent FOREIGN KEY (parent_id) REFERENCES iam_org (id),
    CONSTRAINT chk_org_type CHECK (org_type IN ('HQ', 'BRANCH', 'DEPT', 'PROJECT')),
    CONSTRAINT chk_org_level CHECK (org_level BETWEEN 1 AND 4)
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
    version        BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by     BIGINT       NULL,
    updated_by     BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_user_no (user_no),
    KEY idx_user_org (org_id),
    KEY idx_user_status (account_status),
    CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES iam_org (id),
    CONSTRAINT chk_user_status CHECK (account_status IN ('NORMAL', 'DISABLED', 'SEALED', 'LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ----------------------------------------------------------
-- 3. 角色表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_type   VARCHAR(16)  NOT NULL COMMENT '本项目固定为 FUNC=功能角色',
    parent_id   BIGINT       NULL     COMMENT '父角色（RBAC1 继承，RBAC0 不使用）',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500) NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version     BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by  BIGINT       NULL,
    updated_by  BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_role_parent (parent_id),
    CONSTRAINT fk_role_parent FOREIGN KEY (parent_id) REFERENCES iam_role (id),
    CONSTRAINT chk_role_type CHECK (role_type = 'FUNC')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ----------------------------------------------------------
-- 4. 用户-角色关联表
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
    version        BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_ur_role (role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES iam_user (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES iam_role (id),
    CONSTRAINT chk_user_role_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_user_role_period CHECK (expire_time IS NULL OR effective_time IS NULL OR expire_time > effective_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- ----------------------------------------------------------
-- 5. 多系统接入表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_system (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    system_code  VARCHAR(64)  NOT NULL COMMENT '子系统编码',
    system_name  VARCHAR(128) NOT NULL COMMENT '子系统名称',
    callback_url VARCHAR(500) NULL,
    public_key   TEXT         NULL,
    app_secret   VARCHAR(128) NULL     COMMENT '接入方签名密钥（HMAC）',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    description  VARCHAR(500) NULL,
    is_deleted   TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version      BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_code (system_code),
    CONSTRAINT chk_system_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接入子系统';

-- ----------------------------------------------------------
-- 6. 前端菜单/按钮权限树
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    system_id       BIGINT       NOT NULL,
    parent_id       BIGINT       NULL COMMENT '父菜单；顶级菜单为 NULL',
    permission_code VARCHAR(64)  NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    permission_type VARCHAR(16)  NOT NULL COMMENT 'MENU 菜单 / BUTTON 按钮',
    route_path      VARCHAR(255) NULL COMMENT '前端菜单路由',
    action          VARCHAR(64)  NOT NULL COMMENT '权限要求的操作',
    sort_no         INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (system_id, permission_code),
    KEY idx_permission_parent (parent_id),
    CONSTRAINT fk_permission_system FOREIGN KEY (system_id) REFERENCES iam_system (id),
    CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES iam_permission (id),
    CONSTRAINT chk_permission_type CHECK (permission_type IN ('MENU', 'BUTTON')),
    CONSTRAINT chk_permission_action CHECK (action IN (
        'NONE', 'VIEW', 'ALL', 'VIEW_EDIT', 'VIEW_ADD', 'VIEW_APPROVE',
        'VIEW_EXPORT', 'VIEW_EDIT_APPROVE', 'VIEW_ADD_EDIT', 'VIEW_EDIT_EXPORT',
        'VIEW_SELF', 'VIEW_EDIT_SELF'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前端菜单/按钮权限树';

-- ----------------------------------------------------------
-- 7. 后端 API 目录
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_api (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    system_id     BIGINT       NOT NULL,
    permission_id BIGINT       NULL COMMENT '所属前端权限；未映射时鉴权默认拒绝',
    api_code      VARCHAR(64)  NOT NULL,
    api_name      VARCHAR(128) NOT NULL,
    api_path      VARCHAR(255) NOT NULL COMMENT 'Controller 映射模板，如 /users/{userId}',
    http_method   VARCHAR(16)  NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    is_deleted    TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_code (system_id, api_code),
    UNIQUE KEY uk_api_request (api_path, http_method),
    KEY idx_api_permission (permission_id),
    CONSTRAINT fk_api_system FOREIGN KEY (system_id) REFERENCES iam_system (id),
    CONSTRAINT fk_api_permission FOREIGN KEY (permission_id) REFERENCES iam_permission (id),
    CONSTRAINT chk_api_method CHECK (http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后端 API';

-- ----------------------------------------------------------
-- 8. 角色-前端权限关联表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role_permission (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    role_id        BIGINT      NOT NULL,
    permission_id  BIGINT      NOT NULL,
    allowed_action VARCHAR(64) NOT NULL COMMENT '授予的操作（12 种之一）',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_rp_permission (permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES iam_role (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES iam_permission (id),
    CONSTRAINT chk_role_permission_action CHECK (allowed_action IN (
        'NONE', 'VIEW', 'ALL', 'VIEW_EDIT', 'VIEW_ADD', 'VIEW_APPROVE',
        'VIEW_EXPORT', 'VIEW_EDIT_APPROVE', 'VIEW_ADD_EDIT', 'VIEW_EDIT_EXPORT',
        'VIEW_SELF', 'VIEW_EDIT_SELF'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-前端权限关联';

-- ----------------------------------------------------------
-- 9. 令牌表
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
    KEY idx_token_user_status (user_id, status),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES iam_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='令牌';

-- ----------------------------------------------------------
-- 10. 统一日志表（普通操作日志 + 权限变更日志）
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_log (
    log_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    log_type     VARCHAR(32)   NOT NULL COMMENT 'ACTION 普通操作 / PERMISSION_CHANGE 权限变更',
    user_id      BIGINT        NULL     COMMENT '操作人 ID（系统事件可空）',
    username     VARCHAR(128)  NULL     COMMENT '操作人用户名快照（脱敏后）',
    action       VARCHAR(128)  NOT NULL COMMENT '操作内容/动作',
    operate_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    success      TINYINT       NOT NULL COMMENT '操作成功与否（1 成功 / 0 失败）',
    target       VARCHAR(255)  NULL     COMMENT '操作对象（如角色 ID、功能点 ID）',
    ip           VARCHAR(64)   NULL     COMMENT '来源 IP',
    system_code  VARCHAR(64)   NULL     COMMENT '所属系统',
    detail       JSON          NULL     COMMENT '扩展详情（请求参数摘要、拒绝原因等）',
    before_snapshot JSON       NULL     COMMENT '权限变更前快照',
    after_snapshot  JSON       NULL     COMMENT '权限变更后快照',
    decision_id  VARCHAR(64)   NULL     COMMENT '鉴权决策 ID，关联决策链路',
    request_id   VARCHAR(64)   NULL     COMMENT '请求追踪 ID',
    PRIMARY KEY (log_id),
    KEY idx_log_time (operate_time),
    KEY idx_log_user (user_id),
    KEY idx_log_type_time (log_type, operate_time),
    KEY idx_log_system (system_code),
    KEY idx_log_request (request_id),
    CONSTRAINT chk_log_type CHECK (log_type IN ('ACTION', 'PERMISSION_CHANGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一日志（只追加、永久保存）';

SET FOREIGN_KEY_CHECKS = 1;
