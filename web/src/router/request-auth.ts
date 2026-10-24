import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import { onUnauthorized } from '@/utils/request'
import { useAppStore } from '@/stores/app.store'
import { useAuthStore } from '@/stores/auth.store'
import { resetAccessState } from './access'

/** 把 Axios 的未授权事件接到会话、动态路由和登录页，避免请求层依赖 Router/Store。 */
export function setupRequestAuth(router: Router): void {
  onUnauthorized((code, message) => {
    const authStore = useAuthStore()
    authStore.clearSession()
    resetAccessState()
    useAppStore().resetTabs()
    ElMessage.error(message)

    const current = router.currentRoute.value
    if (current.name !== 'LoginPage') {
      void router.replace({
        name: 'LoginPage',
        query: {
          redirect: current.fullPath,
          unauthorizedCode: String(code),
          reason: message,
        },
      })
    }
  })
}
