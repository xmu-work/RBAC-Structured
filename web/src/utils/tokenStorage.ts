/**
 * 会话存储（契约 5.2-1：组件禁止直触 localStorage，只允许经本模块与 auth store 读写）
 * 安全契约 10.4：token 不出现在 URL、不进日志、不打点上报
 */

const KEY_ACCESS_TOKEN = 'iam.accessToken'
const KEY_REFRESH_TOKEN = 'iam.refreshToken'
const KEY_USER_ID = 'iam.userId'
const KEY_DISPLAY_NAME = 'iam.displayName'
const KEY_EXPIRES_AT = 'iam.expiresAt'

export interface SessionSnapshot {
  accessToken: string | null
  /** v1 仅存档不用（契约 5.2-5：无刷新端点，[待后端] /auth/refresh） */
  refreshToken: string | null
  /** 登录响应 userId 为 String（契约 3.2），写入会话时统一 Number() 归一 */
  userId: number | null
  /** 脱敏展示名（契约 5.2-7：无 profile 端点，以登录响应存档为准） */
  displayName: string | null
  /** 到期时间戳（毫秒），由 expiresIn 换算 */
  expiresAt: number | null
}

export interface SessionInput {
  accessToken: string
  refreshToken?: string
  userId: number
  displayName: string
  /** 令牌有效期（秒），登录响应 expiresIn=7200 */
  expiresIn: number
}

export const tokenStorage = {
  save(session: SessionInput): void {
    localStorage.setItem(KEY_ACCESS_TOKEN, session.accessToken)
    if (session.refreshToken) {
      localStorage.setItem(KEY_REFRESH_TOKEN, session.refreshToken)
    }
    localStorage.setItem(KEY_USER_ID, String(session.userId))
    localStorage.setItem(KEY_DISPLAY_NAME, session.displayName)
    localStorage.setItem(KEY_EXPIRES_AT, String(Date.now() + session.expiresIn * 1000))
  },

  getAccessToken(): string | null {
    return localStorage.getItem(KEY_ACCESS_TOKEN)
  },

  snapshot(): SessionSnapshot {
    const userId = localStorage.getItem(KEY_USER_ID)
    const expiresAt = localStorage.getItem(KEY_EXPIRES_AT)
    return {
      accessToken: localStorage.getItem(KEY_ACCESS_TOKEN),
      refreshToken: localStorage.getItem(KEY_REFRESH_TOKEN),
      userId: userId === null ? null : Number(userId),
      displayName: localStorage.getItem(KEY_DISPLAY_NAME),
      expiresAt: expiresAt === null ? null : Number(expiresAt),
    }
  },

  isExpired(now = Date.now()): boolean {
    const expiresAt = localStorage.getItem(KEY_EXPIRES_AT)
    return expiresAt !== null && Number(expiresAt) <= now
  },

  clear(): void {
    localStorage.removeItem(KEY_ACCESS_TOKEN)
    localStorage.removeItem(KEY_REFRESH_TOKEN)
    localStorage.removeItem(KEY_USER_ID)
    localStorage.removeItem(KEY_DISPLAY_NAME)
    localStorage.removeItem(KEY_EXPIRES_AT)
  },
}
