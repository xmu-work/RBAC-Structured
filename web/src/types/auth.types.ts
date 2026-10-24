import type { AccountStatusCode } from '@/constants/enums'

export interface LoginPayload {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
  clientIp?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
  /** 后端 DTO 为 String，写入会话前由前端统一归一为 number（契约 3.2） */
  userId: string
  displayName: string
  accountStatus: AccountStatusCode
}
