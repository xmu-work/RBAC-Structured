import type { MockMethod } from 'vite-plugin-mock'

/**
 * 骨架自检 Mock（契约第 12 章「Mock 先行」示例）
 * 响应包与后端 Result 严格一致：code/message/data/timestamp（时间格式 yyyy-MM-dd HH:mm:ss，契约 3.3）
 * 后续业务模块的 mock 一律按附录 B 签名在本目录按域分文件编写
 */
export default [
  {
    url: '/api/v1/demo/ping',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: { pong: true, time: '2026-10-24 15:30:00' },
      timestamp: '2026-10-24 15:30:00',
    }),
  },
] as MockMethod[]
