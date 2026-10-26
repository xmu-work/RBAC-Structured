<script setup lang="ts">
import { ref, watch } from 'vue'
import { getUser } from '@/api/user.api'
import { useDict } from '@/composables/useDict'
import { useOrgNames } from '@/composables/useOrgTree'
import { AccountStatus } from '@/constants/enums'
import type { User } from '@/types/user.types'
import { formatDateTime } from '@/utils/time'

/**
 * 用户详情抽屉（契约 8.4 / 10.3）：只渲染安全视图字段（realName/phoneMasked）。
 * 若后端误回 nameEnc/phoneEnc/*Hash 等密文字段，User 类型未声明、模板不渲染（红线 10.3）。
 */
const props = defineProps<{
  userId: number | null
}>()

const visible = defineModel<boolean>({ default: false })

const { dictLabel, accountStatusOptions } = useDict()
const { resolve: resolveOrgNames, nameOf } = useOrgNames()

const loading = ref(false)
const error = ref<string | null>(null)
const detail = ref<User | null>(null)

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const STATUS_TAG: Record<string, TagType> = {
  [AccountStatus.NORMAL]: 'success',
  [AccountStatus.DISABLED]: 'info',
  [AccountStatus.SEALED]: 'danger',
  [AccountStatus.LOCKED]: 'warning',
}

watch(
  () => [visible.value, props.userId] as const,
  () => void loadDetail(),
  { immediate: true },
)

async function loadDetail(): Promise<void> {
  if (!visible.value || props.userId === null) {
    return
  }
  const userId = props.userId
  loading.value = true
  error.value = null
  try {
    const user = await getUser(userId)
    if (props.userId !== userId) {
      return
    }
    detail.value = user
    await resolveOrgNames([user.orgId])
  } catch (caught) {
    if (props.userId !== userId) {
      return
    }
    detail.value = null
    error.value = caught instanceof Error ? caught.message : '用户详情加载失败'
  } finally {
    if (props.userId === userId) {
      loading.value = false
    }
  }
}

function dash(value: string | number | null | undefined): string {
  return value === null || value === undefined || value === '' ? '—' : String(value)
}
</script>

<template>
  <el-drawer v-model="visible" title="用户详情" size="480px">
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-result v-else-if="error" icon="error" title="用户详情加载失败" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </template>
    </el-result>
    <template v-else-if="detail">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户账号">{{ detail.userNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号（脱敏）">{{ dash(detail.phoneMasked) }}</el-descriptions-item>
        <el-descriptions-item label="所属组织">{{ nameOf(detail.orgId) }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ dash(detail.post) }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="STATUS_TAG[detail.accountStatus]" effect="light">
            {{ dictLabel(accountStatusOptions, detail.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近登录">{{ formatDateTime(detail.lastLoginAt) }}</el-descriptions-item>
        <el-descriptions-item label="离职时间">{{ formatDateTime(detail.leaveTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        class="user-detail-drawer__note"
        type="info"
        :closable="false"
        show-icon
        title="安全说明：姓名与手机号均为后端脱敏视图字段；密文/哈希字段（10.3 红线）不展示。"
      />
    </template>
    <el-empty v-else description="暂无数据" />
  </el-drawer>
</template>

<style scoped>
.user-detail-drawer__note {
  margin-top: var(--space-4);
}
</style>
