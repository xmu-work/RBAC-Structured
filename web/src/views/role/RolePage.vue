<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listRoles } from '@/api/role.api'
import RoleListCard from './components/RoleListCard.vue'
import RoleDetailCard from './components/RoleDetailCard.vue'
import RoleExclusionCard from './components/RoleExclusionCard.vue'
import RoleCreateDialog from './components/RoleCreateDialog.vue'
import RoleExclusionDialog from './components/RoleExclusionDialog.vue'
import type { Role, RoleExclusion } from '@/types/role.types'

/** 左列表 + 右（详情/互斥）的角色管理主页（契约 8.5）：本组件只负责布局与协同 */
const selectedRoleId = ref<number | null>(null)
const selectedRole = ref<Role | null>(null)
const allRoles = ref<Role[]>([])
const excludedIds = ref<number[]>([])
const listReloadSignal = ref(0)
const exclusionReloadSignal = ref(0)

const createDialogVisible = ref(false)
const exclusionDialogVisible = ref(false)

onMounted(() => {
  void listRoles().then((roles) => {
    allRoles.value = roles
  })
})

function handleSelect(roleId: number): void {
  selectedRoleId.value = roleId
}

function handleDetailLoaded(role: Role): void {
  selectedRole.value = role
}

function handleExclusionsLoaded(list: RoleExclusion[]): void {
  excludedIds.value = list.map((item) => item.excludedRoleId)
}

function handleRoleCreated(): void {
  listReloadSignal.value += 1
}

function handleExclusionAdded(): void {
  exclusionReloadSignal.value += 1
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
      <RoleDetailCard :role-id="selectedRoleId" @loaded="handleDetailLoaded" />
      <RoleExclusionCard
        :role-id="selectedRoleId"
        :reload-signal="exclusionReloadSignal"
        @add="exclusionDialogVisible = true"
        @loaded="handleExclusionsLoaded"
      />
    </div>
    <RoleCreateDialog v-model="createDialogVisible" @success="handleRoleCreated" />
    <RoleExclusionDialog
      v-model="exclusionDialogVisible"
      :role="selectedRole"
      :roles="allRoles"
      :excluded-role-ids="excludedIds"
      @success="handleExclusionAdded"
    />
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
