import type { Router } from 'vue-router'
import { BUSINESS_LAYOUT_ROUTE_NAME, createBusinessRoute } from './business-routes'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

let accessReady = false
let bootstrapPromise: Promise<void> | null = null
let routerInstance: Router | null = null

function removeBusinessRoute(router: Router): void {
  if (router.hasRoute(BUSINESS_LAYOUT_ROUTE_NAME)) {
    router.removeRoute(BUSINESS_LAYOUT_ROUTE_NAME)
  }
}

export function installBusinessRoutes(router: Router): void {
  const permissionStore = usePermissionStore()
  removeBusinessRoute(router)
  const route = createBusinessRoute(permissionStore.permissionIds)
  if (route) {
    router.addRoute(route)
  }
}

export function isAccessReady(): boolean {
  return accessReady
}

/** 首次进入受保护页面时恢复会话、拉取权限并注册动态路由。 */
export async function ensureAccessReady(router: Router): Promise<void> {
  routerInstance = router
  if (accessReady) {
    return
  }
  if (bootstrapPromise) {
    return bootstrapPromise
  }

  bootstrapPromise = (async () => {
    const authStore = useAuthStore()
    const permissionStore = usePermissionStore()
    authStore.restore()

    if (
      !authStore.hasValidSession ||
      authStore.userId === null ||
      !(await authStore.validateRemote())
    ) {
      authStore.clearSession()
      permissionStore.reset()
      removeBusinessRoute(router)
      throw new Error('登录状态已失效')
    }

    await permissionStore.fetchPermissions(authStore.userId)
    installBusinessRoutes(router)
    accessReady = true
  })()

  try {
    await bootstrapPromise
  } finally {
    bootstrapPromise = null
  }
}

/** 手动或轮询刷新权限；返回权限集合是否发生变化。 */
export async function refreshAccessRoutes(silent = false): Promise<boolean> {
  const authStore = useAuthStore()
  if (!authStore.hasValidSession || authStore.userId === null) {
    return false
  }
  const permissionStore = usePermissionStore()
  const changed = await permissionStore.fetchPermissions(authStore.userId, silent)
  if ((changed || routerInstance) && routerInstance) {
    installBusinessRoutes(routerInstance)
  }
  return changed
}

export function resetAccessState(): void {
  if (routerInstance) {
    removeBusinessRoute(routerInstance)
  }
  usePermissionStore().reset()
  accessReady = false
  bootstrapPromise = null
}
