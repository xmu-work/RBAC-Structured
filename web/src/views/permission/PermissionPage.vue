<script setup lang="ts">
import { ref } from 'vue'
import SystemListCard from './components/SystemListCard.vue'
import SystemCreateDialog from './components/SystemCreateDialog.vue'
import PermissionRegisterCard from './components/PermissionRegisterCard.vue'
import ApiRegisterCard from './components/ApiRegisterCard.vue'
import RolePermissionBindCard from './components/RolePermissionBindCard.vue'
import type { System } from '@/types/system.types'

/** 功能权限管理主页：多系统、前端权限树、后端 API 与角色授权。 */
const systems = ref<System[]>([])
const listReloadSignal = ref(0)
const createDialogVisible = ref(false)

function handleSystemsLoaded(list: System[]): void {
  systems.value = list
}

function handleSystemCreated(): void {
  listReloadSignal.value += 1
}
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 grid 划分与间距 -->
  <div class="permission-page">
    <SystemListCard
      :reload-signal="listReloadSignal"
      @create="createDialogVisible = true"
      @loaded="handleSystemsLoaded"
    />
    <div class="permission-page__grid">
      <div class="permission-page__column">
        <PermissionRegisterCard :systems="systems" />
        <ApiRegisterCard :systems="systems" />
      </div>
      <RolePermissionBindCard />
    </div>
    <SystemCreateDialog v-model="createDialogVisible" @success="handleSystemCreated" />
  </div>
</template>

<style scoped>
.permission-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.permission-page__grid {
  display: grid;
  grid-template-columns: minmax(340px, 1fr) minmax(340px, 1fr);
  gap: var(--space-4);
  align-items: start;
}

.permission-page__column {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

@media (width <= 1080px) {
  .permission-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
