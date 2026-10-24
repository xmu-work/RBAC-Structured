import type { PageQuery } from './common'

export interface AuditLog {
  logId: number
  userId?: number
  username?: string
  action: string
  operateTime: string
  success: number
  target?: string
  ip?: string
  systemCode?: string
  detail?: string
  decisionId?: string
}

export interface AuditLogQuery extends PageQuery {
  userId?: number
  systemCode?: string
  /** ISO 本地时间 YYYY-MM-DDTHH:mm:ss */
  startTime?: string
  /** ISO 本地时间 YYYY-MM-DDTHH:mm:ss */
  endTime?: string
}
