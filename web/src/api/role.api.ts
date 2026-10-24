import { deleteForm, get, postForm } from '@/utils/request'
import type {
  GrantPayload,
  Role,
  RoleCreatePayload,
  RoleExclusion,
  UserRole,
} from '@/types/role.types'

export function listRoles(): Promise<Role[]> {
  return get<Role[]>('/roles')
}

export function getRole(roleId: number): Promise<Role> {
  return get<Role>(`/roles/${roleId}`)
}

export function createRole(payload: RoleCreatePayload): Promise<Role> {
  return postForm<Role>('/roles', payload)
}

export function addRoleExclusion(
  roleId: number,
  excludedRoleId: number,
  reason?: string,
): Promise<void> {
  return postForm<void>(`/roles/${roleId}/exclusions`, { excludedRoleId, reason })
}

export function listRoleExclusions(roleId: number): Promise<RoleExclusion[]> {
  return get<RoleExclusion[]>(`/roles/${roleId}/exclusions`)
}

export function grantRole(payload: GrantPayload): Promise<UserRole> {
  return postForm<UserRole>('/roles/grants', payload)
}

export function revokeRole(userId: number, roleId: number): Promise<void> {
  return deleteForm<void>('/roles/grants', { userId, roleId })
}

export function listUserRoles(userId: number): Promise<UserRole[]> {
  return get<UserRole[]>(`/roles/grants/${userId}`)
}
