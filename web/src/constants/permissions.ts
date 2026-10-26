/**
 * 管理后台功能点编码（契约 6.1）。
 *
 * 与 src/main/resources/db/seed.sql 中 ADMIN 系统登记的 iam_function_point 主键一一对应，
 * 集中维护可避免页面散落魔法数字。
 */

/** 菜单功能点：L1 路由/菜单控制 */
export const ADMIN_FUNCTION_POINTS = {
  HOME: 10001,
  ORGANIZATION: 10002,
  USER: 10003,
  ROLE: 10004,
  GRANT: 10005,
  FUNCTION_PERMISSION: 10006,
  DATA_SCOPE: 10007,
  AUDIT: 10008,
} as const

/** 接口功能点：后端按 API 鉴权的依据，L2 按钮的 v-perm 直接使用（契约 6.1-2） */
export const ADMIN_API_FUNCTION_POINTS = {
  USER_LIST: 20001,
  USER_CREATE: 20002,
  USER_GET: 20003,
  USER_STATUS: 20004,
  ROLE_LIST: 20005,
  ROLE_CREATE: 20006,
  ROLE_GET: 20007,
  GRANT_CREATE: 20008,
  GRANT_REVOKE: 20009,
  GRANT_LIST: 20010,
  SYSTEM_LIST: 20011,
  SYSTEM_CREATE: 20012,
  MODULE_CREATE: 20013,
  FP_CREATE: 20014,
  ROLE_FP_BIND: 20015,
  ROLE_FP_UNBIND: 20016,
  ROLE_FP_LIST: 20017,
} as const

export type AdminFunctionPoint = (typeof ADMIN_FUNCTION_POINTS)[keyof typeof ADMIN_FUNCTION_POINTS]

export const ALL_ADMIN_FUNCTION_POINT_IDS = Object.values(ADMIN_FUNCTION_POINTS)

export const ALL_ADMIN_API_FUNCTION_POINT_IDS = Object.values(ADMIN_API_FUNCTION_POINTS)
