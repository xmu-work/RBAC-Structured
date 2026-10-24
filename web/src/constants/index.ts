/** 管理后台自身系统编码（契约 6.1：[待后端] 在 iam_system 登记 ADMIN 及其菜单/按钮功能点） */
export const ADMIN_SYSTEM_CODE = 'ADMIN'

/** 权限轮询间隔（契约 6.3：60s 定时轮询 + 主动失效双保险） */
export const PERMISSION_POLL_INTERVAL = 60_000

/** 分页默认页大小（契约 9.2-1：size 默认 20） */
export const DEFAULT_PAGE_SIZE = 20
