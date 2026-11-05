import type { PageQuery } from './common'

export type LogType = 'ACTION' | 'PERMISSION_CHANGE'

export interface LogEntry {
  logId: number
  logType: LogType
  userId?: number
  username?: string
  action: string
  operateTime: string
  success: number
  target?: string
  ip?: string
  systemCode?: string
  detail?: string
  beforeSnapshot?: string
  afterSnapshot?: string
  decisionId?: string
  requestId?: string
}

export interface LogQuery extends PageQuery {
  userId?: number
  systemCode?: string
  logType?: LogType
  /** ISO 本地时间 YYYY-MM-DDTHH:mm:ss */
  startTime?: string
  /** ISO 本地时间 YYYY-MM-DDTHH:mm:ss */
  endTime?: string
}
