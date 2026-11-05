import { deleteForm, get, postForm } from '@/utils/request'
import type {
  GrantPayload,
  Role,
  RoleCreatePayload,
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

export function grantRole(payload: GrantPayload): Promise<UserRole> {
  return postForm<UserRole>(`/roles/${payload.roleId}/assignments`, {
    userId: payload.userId,
    expireTime: payload.expireTime,
    grantReason: payload.grantReason,
  })
}

export function revokeRole(userId: number, roleId: number): Promise<void> {
  return deleteForm<void>(`/roles/${roleId}/assignments/${userId}`)
}

export function listUserRoles(userId: number): Promise<UserRole[]> {
  return get<UserRole[]>('/roles/assignments', { userId })
}
