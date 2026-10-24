<script setup lang="ts">
import { onMounted } from 'vue'
import SidebarMenu from './components/SidebarMenu.vue'
import AppHeader from './components/AppHeader.vue'
import AppTabs from './components/AppTabs.vue'
import ContentBreadcrumb from './components/ContentBreadcrumb.vue'
import { useAppStore } from '@/stores/app.store'
import { useDictStore } from '@/stores/dict.store'
import { usePermissionSync } from '@/composables/usePermissionSync'

const appStore = useAppStore()
const dictStore = useDictStore()

usePermissionSync()

onMounted(() => {
  void dictStore.loadDataScopes()
})
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：视觉属性由各独立子组件承载。 -->
  <div class="app-layout" :class="{ 'app-layout--collapsed': appStore.sidebarCollapsed }">
    <aside class="app-layout__sidebar">
      <SidebarMenu />
    </aside>

    <section class="app-layout__workspace">
      <header class="app-layout__header">
        <AppHeader />
      </header>
      <div class="app-layout__tabs">
        <AppTabs />
      </div>
      <main id="main-content" class="app-layout__main">
        <ContentBreadcrumb />
        <router-view v-slot="{ Component, route }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.app-layout__sidebar {
  flex: 0 0 var(--layout-sidebar-width);
  width: var(--layout-sidebar-width);
  overflow: hidden;
  transition:
    flex-basis 0.2s ease,
    width 0.2s ease;
}

.app-layout--collapsed .app-layout__sidebar {
  flex-basis: var(--layout-sidebar-collapsed-width);
  width: var(--layout-sidebar-collapsed-width);
}

.app-layout__workspace {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.app-layout__header {
  flex: 0 0 var(--layout-header-height);
  height: var(--layout-header-height);
}

.app-layout__tabs {
  flex: 0 0 var(--layout-tabs-height);
  height: var(--layout-tabs-height);
}

.app-layout__main {
  flex: 1;
  min-width: 0;
  padding: var(--space-4) var(--space-6) var(--space-6);
  overflow-x: hidden;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.16s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
