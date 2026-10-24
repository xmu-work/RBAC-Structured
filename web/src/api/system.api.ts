import { get, postForm, postJson } from '@/utils/request'
import type {
  DataScope,
  DataScopeCreatePayload,
  FunctionPoint,
  FunctionPointCreatePayload,
  Module,
  ModuleCreatePayload,
  RoleDataScopeBindPayload,
  RoleFunctionBindPayload,
  System,
  SystemCreatePayload,
} from '@/types/system.types'

export function listSystems(): Promise<System[]> {
  return get<System[]>('/systems')
}

export function createSystem(payload: SystemCreatePayload): Promise<System> {
  return postForm<System>('/systems', payload)
}

export function createModule(payload: ModuleCreatePayload): Promise<Module> {
  return postForm<Module>('/systems/modules', payload)
}

export function createFunctionPoint(payload: FunctionPointCreatePayload): Promise<FunctionPoint> {
  return postForm<FunctionPoint>('/systems/function-points', payload)
}

/** 混合传参：query 携带角色与操作码，功能点 ID 集合走 JSON Body（契约 4.2）。 */
export function bindRoleFunctions(payload: RoleFunctionBindPayload): Promise<void> {
  return postJson<void>('/systems/role-functions', payload.functionPointIds, {
    roleId: payload.roleId,
    allowedAction: payload.allowedAction,
    createdBy: payload.createdBy,
  })
}

export function listDataScopes(): Promise<DataScope[]> {
  return get<DataScope[]>('/systems/data-scopes')
}

export function createDataScope(payload: DataScopeCreatePayload): Promise<DataScope> {
  return postForm<DataScope>('/systems/data-scopes', payload)
}

export function bindRoleDataScope(payload: RoleDataScopeBindPayload): Promise<void> {
  return postForm<void>('/systems/role-data-scopes', payload)
}
