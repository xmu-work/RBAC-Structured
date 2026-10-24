<script setup lang="ts">
import { ref } from 'vue'
import DataScopeDictCard from './components/DataScopeDictCard.vue'
import DataScopeCreateDialog from './components/DataScopeCreateDialog.vue'
import RoleDataScopeBindCard from './components/RoleDataScopeBindCard.vue'

/** 数据权限管理主页（契约 8.8）：左字典、右角色授权表单 */
const createDialogVisible = ref(false)
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 grid 划分与间距 -->
  <div class="data-scope-page">
    <DataScopeDictCard @create="createDialogVisible = true" />
    <RoleDataScopeBindCard />
    <DataScopeCreateDialog v-model="createDialogVisible" />
  </div>
</template>

<style scoped>
.data-scope-page {
  display: grid;
  grid-template-columns: minmax(420px, 7fr) 5fr;
  gap: var(--space-4);
  align-items: start;
}

@media (width <= 1080px) {
  .data-scope-page {
    grid-template-columns: 1fr;
  }
}
</style>
