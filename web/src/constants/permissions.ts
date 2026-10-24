/**
 * 管理后台功能点编码（契约 6.2）。
 *
 * [待后端] ADMIN 系统及菜单功能点尚未落库，联调时需将这里的 ID
 * 替换为 iam_function_point 中真实的功能点主键。集中维护可避免页面散落魔法数字。
 */
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

export type AdminFunctionPoint = (typeof ADMIN_FUNCTION_POINTS)[keyof typeof ADMIN_FUNCTION_POINTS]

export const ALL_ADMIN_FUNCTION_POINT_IDS = Object.values(ADMIN_FUNCTION_POINTS)
