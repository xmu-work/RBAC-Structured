import { get } from '@/utils/request'
import { ADMIN_SYSTEM_CODE } from '@/constants'

/**
 * 管理后台只允许消费权限集合读接口（契约 10.5）。
 * 当前后端返回 Set<Long>，JSON 序列化后为 number[]。
 */
export function getUserPermissionIds(userId: number, silent = false): Promise<number[]> {
  return get<number[]>(
    '/open/permissions',
    {
      userId,
      systemCode: ADMIN_SYSTEM_CODE,
    },
    { silent },
  )
}
