import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { AccountStatus, AccountStatusLabel } from '@/constants/enums'
import type { LoginPayload, LoginResponse } from '@/types/auth.types'
import { tokenStorage } from '@/utils/tokenStorage'

export const useAuthStore = defineStore('auth', () => {
  const snapshot = tokenStorage.snapshot()
  const accessToken = ref<string | null>(snapshot.accessToken)
  const refreshToken = ref<string | null>(snapshot.refreshToken)
  const userId = ref<number | null>(snapshot.userId)
  const displayName = ref<string | null>(snapshot.displayName)
  const expiresAt = ref<number | null>(snapshot.expiresAt)

  const hasValidSession = computed(
    () => Boolean(accessToken.value) && expiresAt.value !== null && expiresAt.value > Date.now(),
  )

  function restore(): void {
    const stored = tokenStorage.snapshot()
    accessToken.value = stored.accessToken
    refreshToken.value = stored.refreshToken
    userId.value = stored.userId
    displayName.value = stored.displayName
    expiresAt.value = stored.expiresAt
  }

  function clearSession(): void {
    tokenStorage.clear()
    accessToken.value = null
    refreshToken.value = null
    userId.value = null
    displayName.value = null
    expiresAt.value = null
  }

  async function login(payload: LoginPayload): Promise<LoginResponse> {
    const response = await authApi.login(payload)
    if (response.accountStatus !== AccountStatus.NORMAL) {
      throw new Error(
        `账号当前为“${AccountStatusLabel[response.accountStatus] ?? response.accountStatus}”，无法登录`,
      )
    }

    const normalizedUserId = Number(response.userId)
    if (!Number.isFinite(normalizedUserId)) {
      throw new Error('登录响应中的用户标识无效')
    }

    tokenStorage.save({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: normalizedUserId,
      displayName: response.displayName,
      expiresIn: response.expiresIn,
    })
    restore()
    return response
  }

  async function validateRemote(): Promise<boolean> {
    if (!hasValidSession.value) {
      return false
    }
    return authApi.validateSession()
  }

  async function logout(): Promise<void> {
    try {
      if (hasValidSession.value) {
        await authApi.logout()
      }
    } finally {
      clearSession()
    }
  }

  return {
    accessToken,
    refreshToken,
    userId,
    displayName,
    expiresAt,
    hasValidSession,
    restore,
    clearSession,
    login,
    validateRemote,
    logout,
  }
})
