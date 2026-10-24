<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app.store'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

function closeTab(path: string): void {
  const nextPath = appStore.removeTab(path)
  if (route.path === path && nextPath) {
    void router.push(nextPath)
  }
}
</script>

<template>
  <div class="app-tabs">
    <div class="app-tabs__list">
      <button
        v-for="tab in appStore.tabs"
        :key="tab.path"
        type="button"
        class="app-tabs__item"
        :class="{ 'app-tabs__item--active': route.path === tab.path }"
        @click="router.push(tab.path)"
      >
        <span>{{ tab.title }}</span>
        <el-icon
          v-if="tab.closable"
          class="app-tabs__close"
          :size="14"
          @click.stop="closeTab(tab.path)"
        >
          <Close />
        </el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
.app-tabs {
  display: flex;
  height: 100%;
  align-items: center;
  padding: var(--space-0) var(--space-4);
  overflow-x: auto;
  border-bottom: var(--border-width-base) solid var(--color-border-light);
  background: var(--color-bg-card);
}

.app-tabs__list {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: max-content;
}

.app-tabs__item {
  display: inline-flex;
  height: var(--space-8);
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-0) var(--space-3);
  border: var(--border-width-base) solid var(--color-border-light);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font: inherit;
  font-size: var(--font-size-sm);
  background: var(--color-bg-card);
  cursor: pointer;
}

.app-tabs__item:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-light-7);
  background: var(--color-bg-hover);
}

.app-tabs__item--active {
  color: var(--color-primary);
  border-color: var(--color-primary-light-7);
  background: var(--color-primary-light-9);
}

.app-tabs__close {
  border-radius: var(--radius-round);
}

.app-tabs__close:hover {
  color: var(--color-text-inverse);
  background: var(--color-primary);
}
</style>
