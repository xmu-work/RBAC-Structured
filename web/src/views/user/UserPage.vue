<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changeUserStatus } from '@/api/user.api'
import OrgFilterTree from './components/OrgFilterTree.vue'
import UserTableCard from './components/UserTableCard.vue'
import UserCreateDialog from './components/UserCreateDialog.vue'
import UserSealDialog from './components/UserSealDialog.vue'
import UserDetailDrawer from './components/UserDetailDrawer.vue'
import type { User } from '@/types/user.types'

/** 左组织筛选树 + 右用户列表的主页（契约 8.4）：本组件只负责布局与子组件协同 */
const selectedOrgId = ref<number | null>(null)
const treeReloadSignal = ref(0)
const tableReloadSignal = ref(0)

const createDialogVisible = ref(false)

const sealDialogVisible = ref(false)
const sealSubmitting = ref(false)
const sealingUser = ref<User | null>(null)

const detailDrawerVisible = ref(false)
const detailUserId = ref<number | null>(null)

function handleSelect(orgId: number | null): void {
  selectedOrgId.value = orgId
}

function handleCreated(): void {
  tableReloadSignal.value += 1
}

function handleSeal(user: User): void {
  sealingUser.value = user
  sealDialogVisible.value = true
}

async function handleSealConfirm(reason: string): Promise<void> {
  if (!sealingUser.value) {
    return
  }
  const userId = sealingUser.value.id
  sealSubmitting.value = true
  try {
    const updated = await changeUserStatus(userId, { targetStatus: 'SEALED', reason })
    ElMessage.success(`用户「${updated.realName}」已封存，其角色与数据权限已解绑、令牌已吊销`)
    sealDialogVisible.value = false
    tableReloadSignal.value += 1
  } catch {
    // 3004 等提示由拦截器统一 toast；保持对话框开启便于重试或取消
  } finally {
    sealSubmitting.value = false
  }
}

function handleDetail(user: User): void {
  detailUserId.value = user.id
  detailDrawerVisible.value = true
}
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 grid 划分与间距 -->
  <div class="user-page">
    <OrgFilterTree
      :selected-org-id="selectedOrgId"
      :reload-signal="treeReloadSignal"
      @select="handleSelect"
    />
    <UserTableCard
      :org-id="selectedOrgId"
      :reload-signal="tableReloadSignal"
      @create="createDialogVisible = true"
      @seal="handleSeal"
      @detail="handleDetail"
    />
    <UserCreateDialog v-model="createDialogVisible" @success="handleCreated" />
    <UserSealDialog
      v-model="sealDialogVisible"
      :user="sealingUser"
      :loading="sealSubmitting"
      @confirm="handleSealConfirm"
    />
    <UserDetailDrawer v-model="detailDrawerVisible" :user-id="detailUserId" />
  </div>
</template>

<style scoped>
.user-page {
  display: grid;
  grid-template-columns: minmax(280px, 4fr) 8fr;
  gap: var(--space-4);
  align-items: start;
}

@media (width <= 1080px) {
  .user-page {
    grid-template-columns: 1fr;
  }
}
</style>
