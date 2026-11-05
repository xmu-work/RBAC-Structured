<script setup lang="ts">
import { ref } from 'vue'
import RoleListCard from './components/RoleListCard.vue'
import RoleDetailCard from './components/RoleDetailCard.vue'
import RoleCreateDialog from './components/RoleCreateDialog.vue'

/** 左列表 + 右详情的角色管理主页。 */
const selectedRoleId = ref<number | null>(null)
const listReloadSignal = ref(0)

const createDialogVisible = ref(false)

function handleSelect(roleId: number): void {
  selectedRoleId.value = roleId
}

function handleRoleCreated(): void {
  listReloadSignal.value += 1
}
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 grid 划分与间距 -->
  <div class="role-page">
    <RoleListCard
      :selected-role-id="selectedRoleId"
      :reload-signal="listReloadSignal"
      @select="handleSelect"
      @create="createDialogVisible = true"
    />
    <div class="role-page__aside">
      <RoleDetailCard :role-id="selectedRoleId" />
    </div>
    <RoleCreateDialog v-model="createDialogVisible" @success="handleRoleCreated" />
  </div>
</template>

<style scoped>
.role-page {
  display: grid;
  grid-template-columns: minmax(420px, 6fr) 6fr;
  gap: var(--space-4);
  align-items: start;
}

.role-page__aside {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

@media (width <= 1080px) {
  .role-page {
    grid-template-columns: 1fr;
  }
}
</style>
