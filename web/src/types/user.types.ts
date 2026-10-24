import type { AccountStatusCode } from '@/constants/enums'
import type { PageQuery } from './common'

/**
 * 用户安全视图。
 *
 * 不声明 IamUser 的 nameEnc/nameHash/phoneEnc/phoneHash 字段，避免页面误渲染密文（契约 10.3）。
 * [待后端] 当前接口仍直出实体，正式联调前需补充脱敏视图 DTO。
 */
export interface User {
  id: number
  userNo: string
  displayName?: string
  realName?: string
  phoneMasked?: string
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
  phone: string
  orgId: number
  post?: string
}

export interface UserStatusPayload {
  targetStatus: AccountStatusCode
  reason?: string
}
