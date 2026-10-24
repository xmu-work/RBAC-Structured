<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Avatar,
  Coin,
  Document,
  Grid,
  HomeFilled,
  Key,
  OfficeBuilding,
  User,
} from '@element-plus/icons-vue'
import { visibleBusinessRoutes } from '@/router/business-routes'
import { useAppStore } from '@/stores/app.store'
import { usePermissionStore } from '@/stores/permission.store'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const iconMap: Record<string, Component> = {
  HomeFilled,
  OfficeBuilding,
  User,
  Avatar,
  Key,
  Grid,
  Coin,
  Document,
}

const menuItems = computed(() =>
  visibleBusinessRoutes(permissionStore.permissionIds).map((item) => ({
    path: `/${String(item.path)}`,
    title: String(item.meta?.title ?? ''),
    icon: item.meta?.icon,
  })),
)

function navigate(path: string): void {
  if (route.path !== path) {
    void router.push(path)
  }
}
</script>

<template>
  <div class="sidebar">
    <div class="sidebar__brand">
      <span class="sidebar__mark">I</span>
      <span v-if="!appStore.sidebarCollapsed" class="sidebar__brand-text">
        <strong class="sidebar__name">统一权限平台</strong>
        <small class="sidebar__caption">IAM CONSOLE</small>
      </span>
    </div>

    <el-menu
      class="sidebar__menu"
      :default-active="route.path"
      :collapse="appStore.sidebarCollapsed"
      :collapse-transition="false"
      @select="navigate"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon v-if="item.icon">
          <component :is="iconMap[item.icon]" />
        </el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>

    <div v-if="!appStore.sidebarCollapsed" class="sidebar__footer">
      <span class="sidebar__status-dot" />
      权限服务已连接
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  color: var(--color-text-inverse);
  background: var(--color-primary-dark);
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: var(--layout-header-height);
  padding: var(--space-0) var(--space-4);
  overflow: hidden;
  border-bottom: var(--border-width-base) solid rgb(255 255 255 / 12%);
}

.sidebar__mark {
  display: grid;
  flex: 0 0 var(--space-8);
  width: var(--space-8);
  height: var(--space-8);
  place-items: center;
  border-radius: var(--radius-md);
  color: var(--color-primary-dark);
  font-size: var(--font-size-xl);
  font-weight: 700;
  background: var(--color-text-inverse);
}

.sidebar__brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  white-space: nowrap;
}

.sidebar__name {
  font-size: var(--font-size-base);
  font-weight: 600;
}

.sidebar__caption {
  margin-top: var(--space-1);
  color: rgb(255 255 255 / 60%);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
}

.sidebar__menu {
  flex: 1;
  padding: var(--space-3) var(--space-2);
  border-right: none;
  overflow-y: auto;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgb(255 255 255 / 72%);
  --el-menu-hover-bg-color: rgb(255 255 255 / 10%);
  --el-menu-active-color: var(--color-text-inverse);
}

.sidebar__menu :deep(.el-menu-item) {
  margin-bottom: var(--space-1);
  border-radius: var(--radius-md);
}

.sidebar__menu :deep(.el-menu-item.is-active) {
  background: var(--color-primary);
}

.sidebar__footer {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  color: rgb(255 255 255 / 65%);
  font-size: var(--font-size-xs);
  background: rgb(255 255 255 / 7%);
}

.sidebar__status-dot {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: var(--radius-round);
  background: var(--color-success);
}
</style>
