<script setup lang="ts">
import { ref } from 'vue'
import SystemListCard from './components/SystemListCard.vue'
import SystemCreateDialog from './components/SystemCreateDialog.vue'
import ModuleRegisterCard from './components/ModuleRegisterCard.vue'
import FunctionPointRegisterCard from './components/FunctionPointRegisterCard.vue'
import RoleFunctionBindCard from './components/RoleFunctionBindCard.vue'
import type { System } from '@/types/system.types'

/** 功能权限管理主页（契约 8.7）：上系统列表，下（模块/功能点登记 + 角色绑定） */
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
        <ModuleRegisterCard :systems="systems" />
        <FunctionPointRegisterCard />
      </div>
      <RoleFunctionBindCard />
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
