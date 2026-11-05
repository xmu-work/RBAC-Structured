import type { CommonStatusCode, OperationActionCode } from '@/constants/enums'

export interface System {
  id: number
  systemCode: string
  systemName: string
  callbackUrl?: string
  publicKey?: string
  status: CommonStatusCode
  description?: string
  isDeleted: number
  createdAt: string
  updatedAt: string
}

export interface SystemCreatePayload {
  systemCode: string
  systemName: string
  description?: string
}

/** 前端菜单/按钮权限节点。 */
export interface Permission {
  id: number
  systemId: number
  parentId?: number
  permissionCode: string
  permissionName: string
  permissionType: 'MENU' | 'BUTTON'
  routePath?: string
  action: OperationActionCode
  sortNo: number
  status: CommonStatusCode
  isDeleted: number
  createdAt: string
  updatedAt: string
}

export interface PermissionCreatePayload {
  systemId: number
  parentId?: number
  permissionCode: string
  permissionName: string
  permissionType: 'MENU' | 'BUTTON'
  routePath?: string
  action: OperationActionCode
  sortNo?: number
}

/** 后端 API 独立实体。permissionId 指向其所属菜单/按钮权限。 */
export interface Api {
  id: number
  systemId: number
  permissionId?: number
  apiCode: string
  apiName: string
  apiPath: string
  httpMethod: string
  status: CommonStatusCode
  isDeleted: number
  createdAt: string
  updatedAt: string
}

export interface ApiCreatePayload {
  systemId: number
  apiCode: string
  apiName: string
  apiPath: string
  httpMethod: string
}

/** 角色-前端权限绑定记录。 */
export interface RolePermission {
  id: number
  roleId: number
  permissionId: number
  allowedAction: OperationActionCode
  createdAt: string
  createdBy?: number
}

export interface RolePermissionBindPayload {
  roleId: number
  permissionIds: number[]
  allowedAction: OperationActionCode
}
