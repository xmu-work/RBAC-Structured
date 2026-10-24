/**
 * Axios 单实例封装（契约第 4 章：拦截器统一承接全部 API 访问层契约）
 * - baseURL = VITE_API_BASE（dev 经 Vite proxy 至 8080，禁止浏览器跨域直连，契约 10.6）
 * - 资源路径一律裸路径（/auth/login、/users），baseURL 已含 /api/v1（契约 4.1）
 * - code=0 解包返回 data；code≠0 进入错误矩阵（契约 4.6），业务页默认不写 try/catch
 */
import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { tokenStorage } from './tokenStorage'
import { newIdempotencyKey, newRequestId } from './requestId'
import type { Result } from '@/types/common'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 后台轮询等场景不展示全局错误提示，但仍执行未授权会话处理。 */
    silent?: boolean
  }
}

export interface RequestOptions {
  silent?: boolean
}

/** 业务错误：code≠0 或 HTTP 层错误统一包装成此结构 reject 给业务层（局部自定义展示时 catch） */
export interface BusinessError {
  code: number
  message: string
}

/**
 * 未授权回调注册（2001~2006 触发，契约 4.6）
 * 由 F-AUTH 模块的 auth store / 路由守卫装配，避免 request ↔ store 循环依赖
 */
let unauthorizedHandler: (code: number, message: string) => void = () => undefined
export function onUnauthorized(handler: (code: number, message: string) => void): void {
  unauthorizedHandler = handler
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 15_000,
})

/* ---------------- 请求拦截器（契约 4.3） ---------------- */
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // X-Request-Id：每请求 UUID v4，用于与后端日志关联（[待后端] 回传）
  config.headers.set('X-Request-Id', newRequestId())

  // Authorization：除 POST /auth/login 外所有请求自动携带 Bearer Token
  const isLogin = config.url?.endsWith('/auth/login') ?? false
  const token = tokenStorage.getAccessToken()
  if (token && !isLogin) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  // Idempotency-Key：写请求自动携带（表单会话级 Key 由调用方传入覆盖；[待后端] 幂等拦截）
  const isWrite = (config.method ?? 'get').toUpperCase() !== 'GET'
  if (isWrite && !config.headers.get('Idempotency-Key')) {
    config.headers.set('Idempotency-Key', newIdempotencyKey())
  }
  return config
})

/* ---------------- 响应拦截器：错误矩阵（契约 4.6，逐码对齐 ErrorCode.java） ---------------- */
function rejectWith(code: number, message: string): Promise<never> {
  const error: BusinessError = { code, message }
  return Promise.reject(error)
}

function handleBusinessError(body: Result, silent = false): Promise<never> {
  const code = body.code
  const message = body.message || '请求失败'
  switch (code) {
    case 2001: // 未登录
    case 2002: // 令牌无效
    case 2004: // 令牌已吊销
      // 清会话 → 跳 /login?redirect=当前地址（清会话与跳转收口在 unauthorizedHandler）
      unauthorizedHandler(code, '登录状态失效，请重新登录')
      return rejectWith(code, message)
    case 2003: // 令牌过期（v1 无刷新端点，契约 5.2-5）
      unauthorizedHandler(code, '登录已过期，请重新登录')
      return rejectWith(code, message)
    case 2005: // 账号已封存（UC-02）
    case 2006: // 停用/锁定
      // 强制登出 + 原因提示（全屏提示由 unauthorizedHandler 承载）
      unauthorizedHandler(code, message)
      return rejectWith(code, message)
    case 3008: // 并发冲突（契约 3.6：v1 不传 version）
      if (!silent) {
        ElMessage.error('数据已被他人修改，请刷新后重试')
      }
      return rejectWith(code, message)
    case 4001: // 限流
      if (!silent) {
        ElMessage.warning('操作过于频繁，请稍后再试')
      }
      return rejectWith(code, message)
    case 5000: // 服务器内部错误
    case 5001: // 依赖服务不可用
      if (!silent) {
        ElMessage.error(`${message}，请联系管理员`)
      }
      return rejectWith(code, message)
    default:
      // 1001/1002/1003/2007/2008/3001/3003/3004/3005/3007 等：toast 后端 message 原文（契约 9.2-7）
      // 需自定义展示的场景（3001 互斥详情、表单 1001 回填、3003 还原拖拽）由页面局部 catch 处理
      if (!silent) {
        ElMessage.error(message)
      }
      return rejectWith(code, message)
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data as Result | undefined
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        // 解包：业务代码只见 data（契约 4.4）
        return body.data as never
      }
      return handleBusinessError(body, response.config.silent)
    }
    return response.data
  },
  (error: AxiosError<Result>) => {
    // 后端 GlobalExceptionHandler 保证 HTTP 状态与错误码匹配，body 同走错误矩阵
    if (error.response?.data && typeof error.response.data.code === 'number') {
      return handleBusinessError(error.response.data, error.config?.silent)
    }
    if (error.code === 'ECONNABORTED') {
      if (!error.config?.silent) {
        ElMessage.error('请求超时，请检查网络后重试')
      }
    } else if (!error.config?.silent) {
      ElMessage.error('网络异常，无法连接服务器')
    }
    return rejectWith(error.response?.data?.code ?? -1, error.message)
  },
)

/* ---------------- 请求形态封装（契约 4.2，与 Controller 签名严格对齐） ---------------- */

/** 查询：GET + query 参数（分页参数 page 从 1 起、size 默认 20） */
export function get<T>(url: string, params?: object, options?: RequestOptions): Promise<T> {
  return request.get(url, { params, silent: options?.silent }) as Promise<T>
}

/** JSON Body 写接口：仅登录、批量权限判定等 @RequestBody 场景使用 */
export function postJson<T>(url: string, data?: unknown, params?: object): Promise<T> {
  return request.post(url, data ?? null, { params }) as Promise<T>
}

/**
 * @RequestParam 写接口（契约 4.2：除 JSON Body 类外全部写接口走 query 传参，Body 为空）
 * 禁止自行为其拼 JSON Body（后端不解析）
 */
export function postForm<T>(url: string, params?: object): Promise<T> {
  return request.post(url, null, { params }) as Promise<T>
}

/** PUT + query 参数（如 /users/{id}/status、/orgs/{id}/move） */
export function putForm<T>(url: string, params?: object): Promise<T> {
  return request.put(url, null, { params }) as Promise<T>
}

/** DELETE + query 参数（如 /roles/grants 回收授权） */
export function deleteForm<T>(url: string, params?: object): Promise<T> {
  return request.delete(url, { params }) as Promise<T>
}

export default request
