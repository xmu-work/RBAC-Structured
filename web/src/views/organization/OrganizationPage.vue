<script setup lang="ts">
import { ref } from 'vue'
import OrgTreeCard from './components/OrgTreeCard.vue'
import OrgDetailCard from './components/OrgDetailCard.vue'
import OrgCreateDialog from './components/OrgCreateDialog.vue'
import type { Org } from '@/types/org.types'

/** 左树右详情的组织管理主页（契约 8.3）：本组件只负责布局与子组件协同 */
const selectedOrgId = ref<number | null>(null)
const selectedOrg = ref<Org | null>(null)
const createDialogVisible = ref(false)
const treeReloadSignal = ref(0)

function handleSelect(orgId: number | null): void {
  selectedOrgId.value = orgId
  if (orgId === null) {
    selectedOrg.value = null
  }
}

function handleDetailLoaded(org: Org): void {
  selectedOrg.value = org
}

function handleCreate(parent: Org): void {
  selectedOrg.value = parent
  createDialogVisible.value = true
}

function handleCreated(): void {
  treeReloadSignal.value += 1
}
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 grid 划分与间距 -->
  <div class="organization-page">
    <OrgTreeCard
      :reload-signal="treeReloadSignal"
      :selected-org-id="selectedOrgId"
      @select="handleSelect"
    />
    <OrgDetailCard :org-id="selectedOrgId" @create="handleCreate" @loaded="handleDetailLoaded" />
    <OrgCreateDialog
      v-model="createDialogVisible"
      :parent-org="selectedOrg"
      @success="handleCreated"
    />
  </div>
</template>

<style scoped>
.organization-page {
  display: grid;
  grid-template-columns: minmax(320px, 5fr) 7fr;
  gap: var(--space-4);
  align-items: start;
}

@media (width <= 1080px) {
  .organization-page {
    grid-template-columns: 1fr;
  }
}
</style>
