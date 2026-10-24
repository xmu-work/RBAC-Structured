import { get } from '@/utils/request'
import type { AuditLog, AuditLogQuery } from '@/types/audit.types'
import type { PageResult } from '@/types/common'

export function listAuditLogs(query: AuditLogQuery): Promise<PageResult<AuditLog>> {
  return get<PageResult<AuditLog>>('/audit-logs', query)
}
