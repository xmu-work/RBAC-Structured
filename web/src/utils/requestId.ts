/**
 * 请求标识工具（契约 4.3）
 * X-Request-Id：与后端日志关联（[待后端] MDC 回传）
 * Idempotency-Key：写请求幂等键（[待后端] 幂等拦截；当前实际防重手段=按钮 loading 禁用）
 */
import { v4 as uuidv4 } from 'uuid'

export function newRequestId(): string {
  return uuidv4()
}

export function newIdempotencyKey(): string {
  return uuidv4()
}
