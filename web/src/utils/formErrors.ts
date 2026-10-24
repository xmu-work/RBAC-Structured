/**
 * 契约 4.6：code=1001 的「字段: 原因; …」分号串解析为字段级错误，
 * 供表单页回填到对应 el-form-item（:error），非表单页由拦截器统一 toast。
 */
import type { BusinessError } from './request'

export function parseFieldErrors(message: string): Record<string, string> {
  const fieldErrors: Record<string, string> = {}
  for (const part of message.split(';')) {
    const separator = part.indexOf(':')
    if (separator <= 0) {
      continue
    }
    const field = part.slice(0, separator).trim()
    const reason = part.slice(separator + 1).trim()
    if (field && reason) {
      fieldErrors[field] = reason
    }
  }
  return fieldErrors
}

/** 业务错误 → 字段错误映射；非 1001 返回空对象（调用方判空决定是否回填） */
export function toFieldErrors(error: unknown): Record<string, string> {
  const business = error as BusinessError
  if (!business || typeof business !== 'object' || business.code !== 1001) {
    return {}
  }
  return parseFieldErrors(business.message)
}
