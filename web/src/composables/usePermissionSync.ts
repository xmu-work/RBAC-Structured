import { onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PERMISSION_POLL_INTERVAL } from '@/constants'
import { refreshAccessRoutes } from '@/router/access'
import { usePermissionStore } from '@/stores/permission.store'

/** 主布局挂载一次的 60 秒权限收敛器（契约 6.3）。 */
export function usePermissionSync(): void {
  const router = useRouter()
  const route = useRoute()
  const permissionStore = usePermissionStore()
  let timer: number | null = null
  let polling = false

  async function poll(): Promise<void> {
    if (polling || document.visibilityState === 'hidden') {
      return
    }
    polling = true
    try {
      await refreshAccessRoutes(true)
      const required = route.meta.functionPointId
      if (required !== undefined && !permissionStore.canAccess(required)) {
        await router.replace('/403')
      }
    } catch {
      // 轮询失败保留当前权限，等待下一次成功拉取；错误提示由请求层统一处理。
    } finally {
      polling = false
    }
  }

  onMounted(() => {
    timer = window.setInterval(poll, PERMISSION_POLL_INTERVAL)
  })

  onBeforeUnmount(() => {
    if (timer !== null) {
      window.clearInterval(timer)
    }
  })
}
