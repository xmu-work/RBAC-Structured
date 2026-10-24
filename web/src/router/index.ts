import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { allBusinessPaths } from './business-routes'
import { ensureAccessReady, isAccessReady, installBusinessRoutes } from './access'
import { useAppStore } from '@/stores/app.store'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'LoginPage',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/403',
    name: 'ForbiddenPage',
    component: () => import('@/views/error/ForbiddenPage.vue'),
    meta: { title: '无访问权限', public: true },
  },
  {
    path: '/500',
    name: 'ServerErrorPage',
    component: () => import('@/views/error/ServerErrorPage.vue'),
    meta: { title: '服务异常', public: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFoundPage',
    component: () => import('@/views/error/NotFoundPage.vue'),
    meta: { title: '页面不存在', public: true },
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes: publicRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

let guardsInstalled = false

export function installRouterGuards(): void {
  if (guardsInstalled) {
    return
  }
  guardsInstalled = true

  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    authStore.restore()
    const requestedBusinessPath =
      to.name === 'NotFoundPage' && (allBusinessPaths.has(to.path) || to.path === '/')

    if (to.meta.public === true && !requestedBusinessPath) {
      if (to.name === 'LoginPage' && authStore.hasValidSession && isAccessReady()) {
        return { path: '/', replace: true }
      }
      return true
    }

    if (!authStore.hasValidSession && (requestedBusinessPath || to.name !== 'LoginPage')) {
      return {
        name: 'LoginPage',
        query: { redirect: to.fullPath },
        replace: true,
      }
    }

    const readyBeforeNavigation = isAccessReady()
    try {
      await ensureAccessReady(router)
    } catch {
      authStore.clearSession()
      return {
        name: 'LoginPage',
        query: { redirect: to.fullPath },
        replace: true,
      }
    }

    // 动态路由刚注册时，重新解析当前 URL，避免首屏误匹配兜底路由。
    if (!readyBeforeNavigation) {
      return { path: to.fullPath, replace: true }
    }

    const permissionStore = usePermissionStore()
    installBusinessRoutes(router)

    if (to.name === 'NotFoundPage' && (allBusinessPaths.has(to.path) || to.path === '/')) {
      return { name: 'ForbiddenPage', replace: true }
    }

    if (to.meta.functionPointId !== undefined) {
      if (!permissionStore.canAccess(to.meta.functionPointId)) {
        return { name: 'ForbiddenPage', replace: true }
      }
    }

    return true
  })

  router.afterEach((to) => {
    useAppStore().addTab(to)
  })
}

export default router
