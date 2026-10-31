/**
 * 请求标识工具（契约 4.3）
 * X-Request-Id：与后端日志关联（后端 RequestIdFilter 写入 MDC 并回写响应头）
 * Idempotency-Key：写请求幂等键；跨重试复用同一 Key 时后端返回首次成功响应
 */
import { v4 as uuidv4 } from 'uuid'

export function newRequestId(): string {
  return uuidv4()
}

export function newIdempotencyKey(): string {
  return uuidv4()
}
