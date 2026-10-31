-- ============================================================
-- RBAC0 初始化数据：管理后台系统、功能点、超级管理员
-- 在 schema.sql 之后执行：mysql -u root -p iam_platform < src/main/resources/db/seed.sql
--
-- 默认管理员：admin / Admin@123（上线后务必修改）
-- 注意：admin 的姓名密文依赖 application.yml 中的 crypto.aes-key，修改密钥后需重新生成
-- ============================================================

SET NAMES utf8mb4;
-- 以北京时间写入默认时间字段（MySQL 容器默认 UTC，否则种子数据的 created_at 会比应用写入的早 8 小时）
SET time_zone = '+08:00';

-- 1. 管理后台自身作为一个接入系统
INSERT INTO iam_system (id, system_code, system_name, status, description, is_deleted)
VALUES (1, 'ADMIN', '权限管理后台', 'ACTIVE', '本系统管理后台自身', 0);

INSERT INTO iam_module (id, system_id, module_code, module_name, sort_no, status, is_deleted)
VALUES (1, 1, 'MENU', '菜单', 1, 'ACTIVE', 0),
       (2, 1, 'API', '管理端接口', 2, 'ACTIVE', 0);

-- 2. 菜单功能点（ID 与前端 web/src/constants/permissions.ts 保持一致）
INSERT INTO iam_function_point (id, module_id, point_code, point_name, action, status, is_deleted) VALUES
(10001, 1, 'MENU_HOME',         '首页',     'VIEW', 'ACTIVE', 0),
(10002, 1, 'MENU_ORGANIZATION', '组织机构', 'VIEW', 'ACTIVE', 0),
(10003, 1, 'MENU_USER',         '用户管理', 'VIEW', 'ACTIVE', 0),
(10004, 1, 'MENU_ROLE',         '角色管理', 'VIEW', 'ACTIVE', 0),
(10005, 1, 'MENU_GRANT',        '授权管理', 'VIEW', 'ACTIVE', 0),
(10006, 1, 'MENU_PERMISSION',   '功能权限', 'VIEW', 'ACTIVE', 0),
(10007, 1, 'MENU_DATA_SCOPE',   '数据权限', 'VIEW', 'ACTIVE', 0),
(10008, 1, 'MENU_AUDIT',        '审计日志', 'VIEW', 'ACTIVE', 0);

-- 3. 管理端 API 功能点：鉴权拦截器按 (api_path, api_method) 匹配，未拥有者返回 403
INSERT INTO iam_function_point (id, module_id, point_code, point_name, action, api_path, api_method, status, is_deleted) VALUES
(20001, 2, 'USER_LIST',          '查询用户列表',     'VIEW', '/users',                      'GET',    'ACTIVE', 0),
(20002, 2, 'USER_CREATE',        '创建用户',         'ALL',  '/users',                      'POST',   'ACTIVE', 0),
(20003, 2, 'USER_GET',           '查询用户详情',     'VIEW', '/users/{userId}',             'GET',    'ACTIVE', 0),
(20004, 2, 'USER_STATUS',        '变更账号状态',     'ALL',  '/users/{userId}/status',      'PUT',    'ACTIVE', 0),
(20005, 2, 'ROLE_LIST',          '查询角色列表',     'VIEW', '/roles',                      'GET',    'ACTIVE', 0),
(20006, 2, 'ROLE_CREATE',        '新建角色',         'ALL',  '/roles',                      'POST',   'ACTIVE', 0),
(20007, 2, 'ROLE_GET',           '查询角色详情',     'VIEW', '/roles/{roleId}',             'GET',    'ACTIVE', 0),
(20008, 2, 'GRANT_CREATE',       '授予角色',         'ALL',  '/roles/grants',               'POST',   'ACTIVE', 0),
(20009, 2, 'GRANT_REVOKE',       '撤销角色',         'ALL',  '/roles/grants',               'DELETE', 'ACTIVE', 0),
(20010, 2, 'GRANT_LIST',         '查询用户角色',     'VIEW', '/roles/grants/{userId}',      'GET',    'ACTIVE', 0),
(20011, 2, 'SYSTEM_LIST',        '查询子系统',       'VIEW', '/systems',                    'GET',    'ACTIVE', 0),
(20012, 2, 'SYSTEM_CREATE',      '注册子系统',       'ALL',  '/systems',                    'POST',   'ACTIVE', 0),
(20013, 2, 'MODULE_CREATE',      '注册模块',         'ALL',  '/systems/modules',            'POST',   'ACTIVE', 0),
(20014, 2, 'FP_CREATE',          '注册功能点',       'ALL',  '/systems/function-points',    'POST',   'ACTIVE', 0),
(20015, 2, 'ROLE_FP_BIND',       '绑定角色功能权限', 'ALL',  '/systems/role-functions',     'POST',   'ACTIVE', 0),
(20016, 2, 'ROLE_FP_UNBIND',     '解绑角色功能权限', 'ALL',  '/systems/role-functions',     'DELETE', 'ACTIVE', 0),
(20017, 2, 'ROLE_FP_LIST',       '查询角色功能权限', 'VIEW', '/systems/role-functions/{roleId}', 'GET', 'ACTIVE', 0),
(20018, 2, 'ORG_TREE',           '查询组织树',       'VIEW', '/orgs',                       'GET',    'ACTIVE', 0),
(20019, 2, 'ORG_CREATE',         '新建组织',         'ALL',  '/orgs',                       'POST',   'ACTIVE', 0),
(20020, 2, 'ORG_GET',            '查询组织详情',     'VIEW', '/orgs/{orgId}',               'GET',    'ACTIVE', 0),
(20021, 2, 'ORG_CHILDREN',       '查询下级组织',     'VIEW', '/orgs/{orgId}/children',      'GET',    'ACTIVE', 0),
(20022, 2, 'ORG_MOVE',           '移动组织节点',     'ALL',  '/orgs/{orgId}/move',          'PUT',    'ACTIVE', 0),
(20023, 2, 'DATA_SCOPE_CREATE',  '创建数据范围',     'ALL',  '/systems/data-scopes',        'POST',   'ACTIVE', 0),
(20024, 2, 'DATA_SCOPE_LIST',    '查询数据范围',     'VIEW', '/systems/data-scopes',        'GET',    'ACTIVE', 0),
(20025, 2, 'ROLE_SCOPE_BIND',    '绑定角色数据范围', 'ALL',  '/systems/role-data-scopes',   'POST',   'ACTIVE', 0),
(20026, 2, 'ROLE_EXCL_ADD',      '配置角色互斥',     'ALL',  '/roles/{roleId}/exclusions',  'POST',   'ACTIVE', 0),
(20027, 2, 'ROLE_EXCL_LIST',     '查询角色互斥',     'VIEW', '/roles/{roleId}/exclusions',  'GET',    'ACTIVE', 0);

-- 4. 超级管理员角色，拥有以上全部功能点
INSERT INTO iam_role (id, role_code, role_name, role_type, status, description, is_deleted)
VALUES (1, 'SUPER_ADMIN', '超级管理员', 'FUNC', 'ACTIVE', '拥有管理后台全部功能权限', 0);

INSERT INTO iam_role_function (role_id, function_point_id, allowed_action)
SELECT 1, id, action FROM iam_function_point WHERE module_id IN (1, 2);

-- 5. 组织机构：总公司为根节点（管理员归属 org_id = 1），其下预置信息中心
INSERT INTO iam_org (id, parent_id, ancestors, org_code, org_name, org_type, org_level, sort_no, status, is_deleted) VALUES
(1, NULL, '0',   'HQ',      '总公司',   'HQ',   1, 0, 'ACTIVE', 0),
(2, 1,    '0,1', 'HQ_INFO', '信息中心', 'DEPT', 3, 1, 'ACTIVE', 0);

-- 6. 管理员账号 admin / Admin@123，授予超级管理员
INSERT INTO iam_user (id, user_no, password_hash, name_enc, name_hash, org_id, account_status, is_deleted)
VALUES (1, 'admin',
        'tixOSPFdKOrb2MmPToW+Lw==:C6/muNPbyI7jZnvTebxp3+ToGlVmk0/jm6wtw1pD/zg=',
        'B4OPGWRD4NL5lmpg7NhNXVGayADahKNOd8oyfZhK9BvI9y3A52MPGnhdtw==',
        '40d3d919f122836c04add2bf70ac897d6ca8fe5947ab539ccf5e161d817a1d19',
        1, 'NORMAL', 0);

INSERT INTO iam_user_role (user_id, role_id, effective_time, grant_reason, status)
VALUES (1, 1, NOW(), '系统初始化', 'ACTIVE');
