import type { CommonStatusCode, DataScopeTypeCode, OperationActionCode } from '@/constants/enums'

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

export interface Module {
  id: number
  systemId: number
  moduleCode: string
  moduleName: string
  sortNo: number
  status: CommonStatusCode
  isDeleted: number
  createdAt: string
  updatedAt: string
}

export interface ModuleCreatePayload {
  systemId: number
  moduleCode: string
  moduleName: string
  sortNo?: number
}

export interface FunctionPoint {
  id: number
  moduleId: number
  pointCode: string
  pointName: string
  action: OperationActionCode
  apiPath?: string
  apiMethod?: string
  status: CommonStatusCode
  isDeleted: number
  createdAt: string
  updatedAt: string
}

export interface FunctionPointCreatePayload {
  moduleId: number
  pointCode: string
  pointName: string
  action: OperationActionCode
  apiPath?: string
  apiMethod?: string
}

/** 角色-功能点绑定记录（后端 IamRoleFunction 直出） */
export interface RoleFunction {
  id: number
  roleId: number
  functionPointId: number
  allowedAction: OperationActionCode
  createdAt: string
  createdBy?: number
}

export interface RoleFunctionBindPayload {
  roleId: number
  functionPointIds: number[]
  allowedAction: OperationActionCode
  createdBy?: number
}

export interface DataScope {
  id: number
  scopeCode: string
  scopeName: string
  description?: string
  sortNo: number
  status: CommonStatusCode
  createdAt: string
}

export interface DataScopeCreatePayload {
  scopeCode: string
  scopeName: string
  description?: string
  sortNo?: number
}

export interface RoleDataScopeBindPayload {
  roleId: number
  scopeType: DataScopeTypeCode
  orgId?: number
  projectId?: number
  createdBy?: number
}
