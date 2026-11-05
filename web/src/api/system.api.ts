import { deleteJson, get, postForm, postJson } from '@/utils/request'
import type {
  Api,
  ApiCreatePayload,
  Permission,
  PermissionCreatePayload,
  RolePermission,
  RolePermissionBindPayload,
  System,
  SystemCreatePayload,
} from '@/types/system.types'

export function listSystems(): Promise<System[]> {
  return get<System[]>('/systems')
}

export function createSystem(payload: SystemCreatePayload): Promise<System> {
  return postForm<System>('/systems', payload)
}

export function listPermissions(systemId: number): Promise<Permission[]> {
  return get<Permission[]>('/permissions', { systemId })
}

export function createPermission(payload: PermissionCreatePayload): Promise<Permission> {
  return postForm<Permission>('/permissions', payload)
}

export function listApis(systemId: number): Promise<Api[]> {
  return get<Api[]>('/apis', { systemId })
}

export function createApi(payload: ApiCreatePayload): Promise<Api> {
  return postForm<Api>('/apis', payload)
}

export function bindApiToPermission(permissionId: number, apiId: number): Promise<Api> {
  return postForm<Api>(`/permissions/${permissionId}/apis/${apiId}`)
}

export function bindRolePermissions(payload: RolePermissionBindPayload): Promise<void> {
  return postJson<void>(`/roles/${payload.roleId}/permissions`, payload.permissionIds, {
    allowedAction: payload.allowedAction,
  })
}

export function getRolePermissions(roleId: number): Promise<RolePermission[]> {
  return get<RolePermission[]>(`/roles/${roleId}/permissions`)
}

export function unbindRolePermissions(roleId: number, permissionIds: number[]): Promise<void> {
  return deleteJson<void>(`/roles/${roleId}/permissions`, permissionIds)
}
