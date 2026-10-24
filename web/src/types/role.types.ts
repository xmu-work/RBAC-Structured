import type { CommonStatusCode, GrantStatusCode, RoleTypeCode } from '@/constants/enums'

export interface Role {
  id: number
  roleCode: string
  roleName: string
  roleType: RoleTypeCode
  parentId?: number
  status: CommonStatusCode
  description?: string
  isDeleted: number
  createdAt: string
  updatedAt: string
  createdBy?: number
  updatedBy?: number
}

export interface RoleCreatePayload {
  roleCode: string
  roleName: string
  roleType: RoleTypeCode
  description?: string
}

export interface RoleExclusion {
  id: number
  roleId: number
  excludedRoleId: number
  description?: string
  createdAt: string
  createdBy?: number
}

export interface UserRole {
  id: number
  userId: number
  roleId: number
  effectiveTime?: string
  expireTime?: string
  grantBy?: number
  grantReason?: string
  status: GrantStatusCode
  createdAt: string
  updatedAt: string
}

export interface GrantPayload {
  userId: number
  roleId: number
  expireTime?: string
  grantBy?: number
  grantReason?: string
}
