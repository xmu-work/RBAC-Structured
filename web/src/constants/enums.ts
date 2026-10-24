/**
 * 枚举字典常量（契约附录 A：与后端 common/enums/* 同名同值，UPPER_SNAKE，契约 3.7）
 * dict store 以此文件为唯一来源；页面禁止散落中文字面量（契约 9.2-7）
 */

/* 账号状态 AccountStatus */
export const AccountStatus = {
  NORMAL: 'NORMAL',
  DISABLED: 'DISABLED',
  SEALED: 'SEALED',
  LOCKED: 'LOCKED',
} as const
export type AccountStatusCode = (typeof AccountStatus)[keyof typeof AccountStatus]
export const AccountStatusLabel: Record<AccountStatusCode, string> = {
  NORMAL: '正常',
  DISABLED: '停用',
  SEALED: '封存（离职）',
  LOCKED: '锁定（异常）',
}

/* 角色类型 RoleType */
export const RoleType = {
  FUNC: 'FUNC',
  DATA: 'DATA',
} as const
export type RoleTypeCode = (typeof RoleType)[keyof typeof RoleType]
export const RoleTypeLabel: Record<RoleTypeCode, string> = {
  FUNC: '功能角色',
  DATA: '岗位型数据角色',
}

/* 组织类型 OrgType（含层级映射，契约 8.3：orgType 与 orgLevel 联动，禁止手填） */
export const OrgType = {
  HQ: 'HQ',
  BRANCH: 'BRANCH',
  DEPT: 'DEPT',
  PROJECT: 'PROJECT',
} as const
export type OrgTypeCode = (typeof OrgType)[keyof typeof OrgType]
export const OrgTypeLabel: Record<OrgTypeCode, string> = {
  HQ: '总公司',
  BRANCH: '分公司',
  DEPT: '部门',
  PROJECT: '项目部',
}
/** orgType → orgLevel 字典内置映射（HQ=1 … PROJECT=4） */
export const OrgTypeLevel: Record<OrgTypeCode, number> = {
  HQ: 1,
  BRANCH: 2,
  DEPT: 3,
  PROJECT: 4,
}

/* 功能点操作 OperationAction（12 种，用于角色-功能绑定配置界面，契约 6.1-3） */
export const OperationAction = {
  NONE: 'NONE',
  VIEW: 'VIEW',
  ALL: 'ALL',
  VIEW_EDIT: 'VIEW_EDIT',
  VIEW_ADD: 'VIEW_ADD',
  VIEW_APPROVE: 'VIEW_APPROVE',
  VIEW_EXPORT: 'VIEW_EXPORT',
  VIEW_EDIT_APPROVE: 'VIEW_EDIT_APPROVE',
  VIEW_ADD_EDIT: 'VIEW_ADD_EDIT',
  VIEW_EDIT_EXPORT: 'VIEW_EDIT_EXPORT',
  VIEW_SELF: 'VIEW_SELF',
  VIEW_EDIT_SELF: 'VIEW_EDIT_SELF',
} as const
export type OperationActionCode = (typeof OperationAction)[keyof typeof OperationAction]
export const OperationActionLabel: Record<OperationActionCode, string> = {
  NONE: '无',
  VIEW: '查看',
  ALL: '全部',
  VIEW_EDIT: '查看/编辑',
  VIEW_ADD: '查看/新增',
  VIEW_APPROVE: '查看/审批',
  VIEW_EXPORT: '查看/导出',
  VIEW_EDIT_APPROVE: '查看/编辑/审批',
  VIEW_ADD_EDIT: '查看/新增/编辑',
  VIEW_EDIT_EXPORT: '查看/编辑/导出',
  VIEW_SELF: '查看本人',
  VIEW_EDIT_SELF: '查看/编辑本人',
}

/* 数据范围 DataScopeType */
export const DataScopeType = {
  ALL: 'ALL',
  BRANCH: 'BRANCH',
  DEPT: 'DEPT',
  PROJECT: 'PROJECT',
  SELF: 'SELF',
} as const
export type DataScopeTypeCode = (typeof DataScopeType)[keyof typeof DataScopeType]
export const DataScopeTypeLabel: Record<DataScopeTypeCode, string> = {
  ALL: '全公司',
  BRANCH: '本分公司',
  DEPT: '本部门',
  PROJECT: '本项目',
  SELF: '本人',
}

/* 通用状态（组织/角色/系统/功能点 status 字段） */
export const CommonStatus = {
  ACTIVE: 'ACTIVE',
} as const
export type CommonStatusCode = (typeof CommonStatus)[keyof typeof CommonStatus]
export const CommonStatusLabel: Record<CommonStatusCode, string> = {
  ACTIVE: '有效',
}

/**
 * 数字布尔（契约 3.5：isDeleted/success/revoked 后端为 Integer 0/1）
 * 前端比较用 === 1，禁止当 boolean 使用
 */
export const DigitalBool = {
  YES: 1,
  NO: 0,
} as const

/* 权限变更类型 ChangeType */
export const ChangeType = {
  GRANT: 'GRANT',
  REVOKE: 'REVOKE',
  SEAL: 'SEAL',
  ROLE_UPDATE: 'ROLE_UPDATE',
  SCOPE_UPDATE: 'SCOPE_UPDATE',
} as const
export type ChangeTypeCode = (typeof ChangeType)[keyof typeof ChangeType]
export const ChangeTypeLabel: Record<ChangeTypeCode, string> = {
  GRANT: '授权',
  REVOKE: '回收',
  SEAL: '封存',
  ROLE_UPDATE: '角色变更',
  SCOPE_UPDATE: '数据范围变更',
}

/* 用户-角色授权状态（IamUserRole.status） */
export const GrantStatus = {
  ACTIVE: 'ACTIVE',
  EXPIRED: 'EXPIRED',
  REVOKED: 'REVOKED',
} as const
export type GrantStatusCode = (typeof GrantStatus)[keyof typeof GrantStatus]
export const GrantStatusLabel: Record<GrantStatusCode, string> = {
  ACTIVE: '生效中',
  EXPIRED: '已过期',
  REVOKED: '已回收',
}
