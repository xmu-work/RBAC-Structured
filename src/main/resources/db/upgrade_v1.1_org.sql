-- ============================================================
-- 增量升级 V1.1：组织机构接口（适用于用旧版 seed.sql 初始化过的库）
-- 表结构无变化，只补数据：组织 API 功能点 20018~20022、超级管理员绑定、预置组织
-- 可重复执行：全部依赖唯一键 INSERT IGNORE，已存在的行跳过
-- 执行：mysql -u root -p --default-character-set=utf8mb4 iam_platform < upgrade_v1.1_org.sql
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+08:00';

START TRANSACTION;

-- 1. 组织机构 API 功能点（与 seed.sql 第 3 节一致）
INSERT IGNORE INTO iam_function_point (id, module_id, point_code, point_name, action, api_path, api_method, status, is_deleted) VALUES
(20018, 2, 'ORG_TREE',     '查询组织树',   'VIEW', '/orgs',                  'GET',  'ACTIVE', 0),
(20019, 2, 'ORG_CREATE',   '新建组织',     'ALL',  '/orgs',                  'POST', 'ACTIVE', 0),
(20020, 2, 'ORG_GET',      '查询组织详情', 'VIEW', '/orgs/{orgId}',          'GET',  'ACTIVE', 0),
(20021, 2, 'ORG_CHILDREN', '查询下级组织', 'VIEW', '/orgs/{orgId}/children', 'GET',  'ACTIVE', 0),
(20022, 2, 'ORG_MOVE',     '移动组织节点', 'ALL',  '/orgs/{orgId}/move',     'PUT',  'ACTIVE', 0);

-- 2. 超级管理员补齐全部功能点（只会新增 20018~20022 这 5 条）
INSERT IGNORE INTO iam_role_function (role_id, function_point_id, allowed_action)
SELECT 1, id, action FROM iam_function_point WHERE module_id IN (1, 2);

-- 3. 预置组织：admin 的 org_id = 1 指向总公司
INSERT IGNORE INTO iam_org (id, parent_id, ancestors, org_code, org_name, org_type, org_level, sort_no, status, is_deleted) VALUES
(1, NULL, '0',   'HQ',      '总公司',   'HQ',   1, 0, 'ACTIVE', 0),
(2, 1,    '0,1', 'HQ_INFO', '信息中心', 'DEPT', 3, 1, 'ACTIVE', 0);

COMMIT;

-- 验证：应为 30 / 30 / 2
SELECT (SELECT COUNT(*) FROM iam_function_point)                AS function_points,
       (SELECT COUNT(*) FROM iam_role_function WHERE role_id = 1) AS super_admin_bindings,
       (SELECT COUNT(*) FROM iam_org)                            AS orgs;
