import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

const SIDEBAR_STORAGE_KEY = 'iam.sidebarCollapsed'

export interface AppTab {
  path: string
  title: string
  name: string
  closable: boolean
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true')
  const globalLoading = ref(false)
  const tabs = ref<AppTab[]>([])

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(sidebarCollapsed.value))
  }

  function addTab(route: RouteLocationNormalizedLoaded): void {
    if (route.meta.public === true || !route.name) {
      return
    }
    const title = String(route.meta.title ?? '未命名页面')
    if (tabs.value.some((tab) => tab.path === route.path)) {
      return
    }
    tabs.value.push({
      path: route.path,
      title,
      name: String(route.name),
      closable: route.name !== 'HomePage',
    })
  }

  function removeTab(path: string): string | null {
    const index = tabs.value.findIndex((tab) => tab.path === path)
    if (index < 0 || !tabs.value[index]?.closable) {
      return null
    }
    tabs.value.splice(index, 1)
    return tabs.value[index]?.path ?? tabs.value[index - 1]?.path ?? '/'
  }

  function resetTabs(): void {
    tabs.value = []
  }

  return {
    sidebarCollapsed,
    globalLoading,
    tabs,
    toggleSidebar,
    addTab,
    removeTab,
    resetTabs,
  }
})
