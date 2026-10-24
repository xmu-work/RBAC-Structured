<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listRoles, revokeRole } from '@/api/role.api'
import { listUsers } from '@/api/user.api'
import GrantFormCard from './components/GrantFormCard.vue'
import UserRoleListCard from './components/UserRoleListCard.vue'
import RevokeDialog from './components/RevokeDialog.vue'
import type { UserRole } from '@/types/role.types'

/**
 * 授权管理主页（契约 8.6）：上授予表单、下用户角色列表；回收走 L2 确认。
 * 审批工作台与变更快照时间线 [BACKLOG]（无端点，契约 11.2）。
 */
const selectedUserId = ref<number | null>(null)
const listReloadSignal = ref(0)

const revokeDialogVisible = ref(false)
const revokeSubmitting = ref(false)
const revokingGrant = ref<UserRole | null>(null)
const revokingUserName = ref('')
const revokingRoleName = ref('')

function handleGranted(userId: number): void {
  selectedUserId.value = userId
  listReloadSignal.value += 1
}

function handleUserChange(userId: number | null): void {
  selectedUserId.value = userId
}

async function handleRevoke(grant: UserRole): Promise<void> {
  revokingGrant.value = grant
  const [userName, roleName] = await Promise.all([
    resolveUserName(grant.userId),
    resolveRoleName(grant.roleId),
  ])
  revokingUserName.value = userName
  revokingRoleName.value = roleName
  revokeDialogVisible.value = true
}

async function resolveUserName(userId: number): Promise<string> {
  try {
    const page = await listUsers({ page: 1, size: 100 })
    const user = page.list.find((item) => item.id === userId)
    return user ? `${user.userNo} · ${user.displayName ?? ''}` : `用户 #${userId}`
  } catch {
    return `用户 #${userId}`
  }
}

async function resolveRoleName(roleId: number): Promise<string> {
  try {
    const roles = await listRoles()
    return roles.find((role) => role.id === roleId)?.roleName ?? `角色 #${roleId}`
  } catch {
    return `角色 #${roleId}`
  }
}

async function handleRevokeConfirm(): Promise<void> {
  if (!revokingGrant.value) {
    return
  }
  const { userId, roleId } = revokingGrant.value
  revokeSubmitting.value = true
  try {
    await revokeRole(userId, roleId)
    ElMessage.success('授权已回收，权限将在缓存收敛后失效（最迟 300 秒）')
    revokeDialogVisible.value = false
    listReloadSignal.value += 1
  } catch {
    // 3004 等提示由拦截器统一 toast；保持对话框开启便于重试或取消
  } finally {
    revokeSubmitting.value = false
  }
}
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责纵向排列与间距 -->
  <div class="grant-page">
    <GrantFormCard @granted="handleGranted" @user-change="handleUserChange" />
    <UserRoleListCard
      :user-id="selectedUserId"
      :reload-signal="listReloadSignal"
      @revoke="handleRevoke"
    />
    <RevokeDialog
      v-model="revokeDialogVisible"
      :user-name="revokingUserName"
      :role-name="revokingRoleName"
      :loading="revokeSubmitting"
      @confirm="handleRevokeConfirm"
    />
  </div>
</template>

<style scoped>
.grant-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
