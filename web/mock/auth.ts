import type { MockMethod } from 'vite-plugin-mock'

const timestamp = '2026-10-24 15:30:00'

export default [
  {
    url: '/api/v1/auth/login',
    method: 'post',
    response: ({ body }: { body?: Record<string, unknown> }) => {
      if (!body?.username || !body?.password) {
        return {
          code: 1001,
          message: 'username: 账号不能为空; password: 密码不能为空',
          timestamp,
        }
      }
      return {
        code: 0,
        message: 'success',
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          tokenType: 'Bearer',
          expiresIn: 7200,
          userId: '1',
          displayName: '管理员',
          accountStatus: 'NORMAL',
        },
        timestamp,
      }
    },
  },
  {
    url: '/api/v1/auth/logout',
    method: 'post',
    response: () => ({
      code: 0,
      message: 'success',
      data: null,
      timestamp,
    }),
  },
  {
    url: '/api/v1/auth/validate',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: true,
      timestamp,
    }),
  },
] as MockMethod[]
