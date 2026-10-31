import { deleteJson, get, postForm, postJson } from '@/utils/request'
import type {
  FunctionPoint,
  FunctionPointCreatePayload,
  Module,
  ModuleCreatePayload,
  RoleFunction,
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

/** 角色已绑定的功能点（回显 / 矩阵雏形） */
export function getRoleFunctions(roleId: number): Promise<RoleFunction[]> {
  return get<RoleFunction[]>(`/systems/role-functions/${roleId}`)
}

/** 解绑：DELETE 带请求体——roleId 走 query，功能点 ID 数组走 JSON Body（契约 4.2）；未绑定的后端忽略 */
export function unbindRoleFunctions(roleId: number, functionPointIds: number[]): Promise<void> {
  return deleteJson<void>('/systems/role-functions', functionPointIds, { roleId })
}
