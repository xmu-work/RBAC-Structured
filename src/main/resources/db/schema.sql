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
    created_by     BIGINT        NULL     COMMENT '创建人',
    updated_by     BIGINT        NULL     COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_org_parent (parent_id),
    KEY idx_org_ancestors (ancestors)
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
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id          BIGINT        NOT NULL COMMENT '角色 A',
    excluded_role_id BIGINT        NOT NULL COMMENT '互斥角色 B',
    description      VARCHAR(255)  NULL     COMMENT '互斥原因（如会计与出纳）',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by       BIGINT        NULL     COMMENT '创建人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_excl (role_id, excluded_role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色互斥（双向落库）';

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
    app_secret   VARCHAR(128) NULL     COMMENT '接入方签名密钥（HMAC）',
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
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    scope_code  VARCHAR(32)   NOT NULL COMMENT '编码：ALL / BRANCH / DEPT / PROJECT / SELF',
    scope_name  VARCHAR(64)   NOT NULL COMMENT '名称：全公司/本分公司/本部门/本项目/本人',
    description VARCHAR(255)  NULL     COMMENT '说明',
    sort_no     INT           NOT NULL DEFAULT 0 COMMENT '排序',
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / DISABLED',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_scope_code (scope_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据范围字典';

-- ----------------------------------------------------------
-- 11. 角色-数据范围关联表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_role_data_scope (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id        BIGINT        NOT NULL COMMENT '角色',
    scope_type     VARCHAR(32)   NOT NULL COMMENT '数据范围类型',
    org_id         BIGINT        NULL     COMMENT '绑定组织（本分公司/本部门时使用）',
    project_id     BIGINT        NULL     COMMENT '绑定项目（本项目时使用，指向 org_type=PROJECT 节点）',
    effective_time DATETIME      NULL     COMMENT '生效时间（临时授权）',
    expire_time    DATETIME      NULL     COMMENT '失效时间',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by     BIGINT        NULL     COMMENT '授权人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_scope (role_id, scope_type, org_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据范围授权';

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
    log_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    user_id      BIGINT        NULL     COMMENT '操作人 ID（系统事件可空）',
    username     VARCHAR(128)  NULL     COMMENT '操作人用户名快照（脱敏后）',
    action       VARCHAR(128)  NOT NULL COMMENT '操作内容/动作',
    operate_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    success      TINYINT       NOT NULL COMMENT '操作成功与否（1 成功 / 0 失败）',
    target       VARCHAR(255)  NULL     COMMENT '操作对象（如角色 ID、功能点 ID）',
    ip           VARCHAR(64)   NULL     COMMENT '来源 IP',
    system_code  VARCHAR(64)   NULL     COMMENT '所属系统',
    detail       JSON          NULL     COMMENT '扩展详情（请求参数摘要、拒绝原因等）',
    decision_id  VARCHAR(64)   NULL     COMMENT '鉴权决策 ID，关联决策链路',
    PRIMARY KEY (log_id),
    KEY idx_log_time (operate_time),
    KEY idx_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志（只追加、永久保存）';

-- ----------------------------------------------------------
-- 14. 权限变更日志表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS iam_permission_change_log (
    change_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT        NULL     COMMENT '受影响用户（角色级变更可空）',
    role_id         BIGINT        NULL     COMMENT '受影响角色（用户级变更可空）',
    change_type     VARCHAR(32)   NOT NULL COMMENT '类型：GRANT / REVOKE / SEAL / ROLE_UPDATE / SCOPE_UPDATE',
    before_snapshot JSON          NULL     COMMENT '变更前快照',
    after_snapshot  JSON          NULL     COMMENT '变更后快照',
    operator        BIGINT        NULL     COMMENT '操作人',
    changed_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    remark          VARCHAR(500)  NULL     COMMENT '备注',
    PRIMARY KEY (change_id),
    KEY idx_pcl_user (user_id),
    KEY idx_pcl_time (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限变更快照（只追加）';

SET FOREIGN_KEY_CHECKS = 1;
