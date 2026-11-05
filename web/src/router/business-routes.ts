import type { RouteRecordRaw } from 'vue-router'
import { ADMIN_MENU_PERMISSIONS } from '@/constants/permissions'

export const BUSINESS_LAYOUT_ROUTE_NAME = 'AppLayout'

/**
 * 权限过滤后的业务路由表，同时是侧边菜单的唯一数据源（契约 6.2）。
 * 这里登记已接入的业务页面，菜单直接按功能点过滤。
 */
export const businessRouteChildren: RouteRecordRaw[] = [
  {
    path: 'home',
    name: 'HomePage',
    component: () => import('@/views/home/HomePage.vue'),
    meta: {
      title: '首页',
      icon: 'HomeFilled',
      permissionId: ADMIN_MENU_PERMISSIONS.HOME,
    },
  },
  {
    path: 'organization',
    name: 'OrganizationPage',
    component: () => import('@/views/organization/OrganizationPage.vue'),
    meta: {
      title: '组织管理',
      icon: 'OfficeBuilding',
      permissionId: ADMIN_MENU_PERMISSIONS.ORGANIZATION,
    },
  },
  {
    path: 'user',
    name: 'UserPage',
    component: () => import('@/views/user/UserPage.vue'),
    meta: {
      title: '用户管理',
      icon: 'User',
      permissionId: ADMIN_MENU_PERMISSIONS.USER,
    },
  },
  {
    path: 'role',
    name: 'RolePage',
    component: () => import('@/views/role/RolePage.vue'),
    meta: {
      title: '角色管理',
      icon: 'Avatar',
      permissionId: ADMIN_MENU_PERMISSIONS.ROLE,
    },
  },
  {
    path: 'grant',
    name: 'GrantPage',
    component: () => import('@/views/grant/GrantPage.vue'),
    meta: {
      title: '授权管理',
      icon: 'Key',
      permissionId: ADMIN_MENU_PERMISSIONS.GRANT,
    },
  },
  {
    path: 'permission',
    name: 'PermissionPage',
    component: () => import('@/views/permission/PermissionPage.vue'),
    meta: {
      title: '功能权限',
      icon: 'Grid',
      permissionId: ADMIN_MENU_PERMISSIONS.FUNCTION_PERMISSION,
    },
  },
  {
    path: 'logs',
    name: 'AuditPage',
    component: () => import('@/views/audit/AuditPage.vue'),
    meta: {
      title: '日志审计',
      icon: 'Document',
      permissionId: ADMIN_MENU_PERMISSIONS.LOGS,
    },
  },
]

export function visibleBusinessRoutes(permissionIds: ReadonlySet<number>): RouteRecordRaw[] {
  return businessRouteChildren.filter((route) => {
    const permissionId = route.meta?.permissionId
    return permissionId === undefined || permissionIds.has(permissionId)
  })
}

export function createBusinessRoute(permissionIds: ReadonlySet<number>): RouteRecordRaw | null {
  const children = visibleBusinessRoutes(permissionIds)
  if (children.length === 0) {
    return null
  }

  const homeAllowed = children.some((route) => route.name === 'HomePage')
  return {
    path: '/',
    name: BUSINESS_LAYOUT_ROUTE_NAME,
    component: () => import('@/layout/AppLayout.vue'),
    redirect: homeAllowed ? '/home' : `/${String(children[0]?.path ?? '')}`,
    children,
  }
}

export const allBusinessPaths = new Set(
  businessRouteChildren.map((route) => `/${String(route.path)}`),
)
