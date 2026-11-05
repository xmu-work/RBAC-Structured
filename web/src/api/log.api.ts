import { get } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { LogEntry, LogQuery } from '@/types/log.types'

export function listLogs(query: LogQuery): Promise<PageResult<LogEntry>> {
  return get<PageResult<LogEntry>>('/logs', query)
}
