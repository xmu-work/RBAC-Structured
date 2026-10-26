import type { AccountStatusCode } from '@/constants/enums'
import type { PageQuery } from './common'

/**
 * 用户安全视图：对应后端 UserView（契约 8.4 / 10.3），不含密文、检索哈希与密码哈希。
 * realName 为解密后的完整姓名；电话只返回脱敏后的 phoneMasked（未填电话时为 null）。
 */
export interface User {
  id: number
  userNo: string
  realName: string
  phoneMasked?: string | null
  orgId: number
  post?: string
  accountStatus: AccountStatusCode
  leaveTime?: string
  lastLoginAt?: string
  isDeleted: number
  createdAt: string
  updatedAt: string
}

export interface UserListQuery extends PageQuery {
  orgId?: number
  accountStatus?: AccountStatusCode
}

export interface UserCreatePayload {
  userNo: string
  realName: string
  orgId: number
  /** 选填：不填由后端使用初始密码 */
  password?: string
  /** 选填 */
  phone?: string
  post?: string
}

export interface UserStatusPayload {
  targetStatus: AccountStatusCode
  reason?: string
}
