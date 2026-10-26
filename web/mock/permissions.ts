import type { MockMethod } from 'vite-plugin-mock'
import { ALL_ADMIN_API_FUNCTION_POINT_IDS, ALL_ADMIN_FUNCTION_POINT_IDS } from '../src/constants/permissions'

const timestamp = '2026-10-24 15:30:00'

export default [
  {
    url: '/api/v1/open/permissions',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      // 与 seed.sql 中超级管理员一致：菜单功能点 + 接口功能点
      data: [...ALL_ADMIN_FUNCTION_POINT_IDS, ...ALL_ADMIN_API_FUNCTION_POINT_IDS],
      timestamp,
    }),
  },
] as MockMethod[]
