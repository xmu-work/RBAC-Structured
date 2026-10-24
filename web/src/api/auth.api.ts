import { get, postForm, postJson } from '@/utils/request'
import type { LoginPayload, LoginResponse } from '@/types/auth.types'

export function login(payload: LoginPayload): Promise<LoginResponse> {
  return postJson<LoginResponse>('/auth/login', payload)
}

export function logout(): Promise<void> {
  return postForm<void>('/auth/logout')
}

export function validateSession(): Promise<boolean> {
  return get<boolean>('/auth/validate')
}
