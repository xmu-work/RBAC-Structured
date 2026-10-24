import type { MockMethod } from 'vite-plugin-mock'
import { ALL_ADMIN_FUNCTION_POINT_IDS } from '../src/constants/permissions'

const timestamp = '2026-10-24 15:30:00'

export default [
  {
    url: '/api/v1/open/permissions',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: ALL_ADMIN_FUNCTION_POINT_IDS,
      timestamp,
    }),
  },
] as MockMethod[]
