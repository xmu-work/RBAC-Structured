<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { changeUserStatus, listUsers } from '@/api/user.api'
import { useDict } from '@/composables/useDict'
import { useOrgNames } from '@/composables/useOrgTree'
import { useTable } from '@/composables/useTable'
import { AccountStatus, type AccountStatusCode } from '@/constants/enums'
import type { User } from '@/types/user.types'

/**
 * 用户列表卡片（契约 8.4 / 9.2-1 标准骨架）：查询区 + 工具栏 + 表格 + 分页。
 * 关键字查询 [缺] 后端参数，UI 暂不提供入口（契约 11.2 待办）。
 */
const props = defineProps<{
  orgId: number | null
  reloadSignal: number
}>()

const emit = defineEmits<{ create: []; seal: [user: User]; detail: [user: User] }>()

const { dictLabel, accountStatusOptions } = useDict()
const { resolve: resolveOrgNames, nameOf } = useOrgNames()

/** 状态 → Tag 配色（视觉映射，文案出自 dict store） */
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const STATUS_TAG: Record<AccountStatusCode, TagType> = {
  [AccountStatus.NORMAL]: 'success',
  [AccountStatus.DISABLED]: 'info',
  [AccountStatus.SEALED]: 'danger',
  [AccountStatus.LOCKED]: 'warning',
}

/* ---------------- 查询区：显式触发（契约 9.2-1） ---------------- */

const draftStatus = ref<AccountStatusCode | ''>('')
const appliedStatus = ref<AccountStatusCode | ''>('')

const fetcher = (params: Record<string, unknown>) =>
  listUsers({
    page: params.page as number,
    size: params.size as number,
    orgId: props.orgId ?? undefined,
    accountStatus: appliedStatus.value || undefined,
  })

const { rows, total, page, size, loading, error, load } = useTable<User>(fetcher)

async function refresh(nextPage = page.value): Promise<void> {
  await load(nextPage)
  await resolveOrgNames(rows.value.map((row) => row.orgId))
}

function applyQuery(): void {
  appliedStatus.value = draftStatus.value
  void refresh(1)
}

function resetQuery(): void {
  draftStatus.value = ''
  appliedStatus.value = ''
  void refresh(1)
}

watch(
  () => props.orgId,
  () => void refresh(1),
)

onMounted(() => {
  void refresh(1)
})

watch(
  () => props.reloadSignal,
  () => void refresh(),
)

/* ---------------- 状态变更（契约 8.4：分级确认；成功后刷新） ---------------- */

const changingIds = ref<number[]>([])

async function changeStatus(user: User, targetStatus: AccountStatusCode, reason?: string) {
  changingIds.value.push(user.id)
  try {
    const updated = await changeUserStatus(user.id, { targetStatus, reason })
    ElMessage.success(
      `用户「${updated.displayName}」状态已变更为「${dictLabel(accountStatusOptions.value, targetStatus)}」`,
    )
  } catch {
    // 3004 等错误提示由拦截器统一 toast；此处仅刷新列表同步真实状态
  } finally {
    changingIds.value = changingIds.value.filter((id) => id !== user.id)
    void refresh()
  }
}

/* el-table 插槽 row 为宽类型（DefaultRow），经此处收窄为 User 供模板调用 */

function asUser(row: unknown): User {
  return row as User
}

function statusOf(row: unknown): AccountStatusCode {
  return (row as User).accountStatus
}

function detail(row: unknown): void {
  emit('detail', row as User)
}

function seal(row: unknown): void {
  emit('seal', row as User)
}

function disable(row: unknown): void {
  void changeStatus(row as User, AccountStatus.DISABLED)
}

function enable(row: unknown): void {
  void changeStatus(row as User, AccountStatus.NORMAL)
}

function rowChanging(row: unknown): boolean {
  return changingIds.value.includes((row as User).id)
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「用户列表」这一块视觉区域 -->
  <el-card class="user-table-card" shadow="never">
    <template #header>
      <div class="user-table-card__header">
        <div>
          <h2 class="user-table-card__title">用户列表</h2>
          <p class="user-table-card__hint">共 {{ total }} 人 · 服务端分页（万人规模，契约 9.2-5）</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="emit('create')">新建用户</el-button>
      </div>
    </template>

    <el-form class="user-table-card__query" inline @submit.prevent="applyQuery">
      <el-form-item label="账号状态">
        <el-select
          v-model="draftStatus"
          placeholder="全部"
          clearable
          class="user-table-card__status-select"
        >
          <el-option
            v-for="option in accountStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="applyQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="error"
      class="user-table-card__error"
      type="error"
      :title="error"
      :closable="false"
      show-icon
    >
      <el-button size="small" type="primary" @click="refresh()">重试</el-button>
    </el-alert>

    <el-table v-else v-loading="loading" :data="rows" class="user-table-card__table">
      <el-table-column prop="userNo" label="账号" min-width="90" />
      <el-table-column prop="displayName" label="姓名（脱敏）" min-width="110" />
      <el-table-column prop="phoneMasked" label="手机号（脱敏）" min-width="130">
        <template #default="{ row }">{{ asUser(row).phoneMasked ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="所属组织" min-width="140">
        <template #default="{ row }">{{ nameOf(asUser(row).orgId) }}</template>
      </el-table-column>
      <el-table-column label="岗位" min-width="110">
        <template #default="{ row }">{{ asUser(row).post ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[statusOf(row)]" effect="light">
            {{ dictLabel(accountStatusOptions, statusOf(row)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近登录" min-width="160">
        <template #default="{ row }">{{ asUser(row).lastLoginAt ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="detail(row)">详情</el-button>
          <el-popconfirm
            v-if="statusOf(row) === AccountStatus.NORMAL"
            title="确认停用该用户？停用后将无法登录"
            confirm-button-text="确认"
            cancel-button-text="取消"
            @confirm="disable(row)"
          >
            <template #reference>
              <el-button link type="warning" :disabled="rowChanging(row)">停用</el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm
            v-if="statusOf(row) === AccountStatus.DISABLED"
            title="确认启用该用户？"
            confirm-button-text="确认"
            cancel-button-text="取消"
            @confirm="enable(row)"
          >
            <template #reference>
              <el-button link type="success" :disabled="rowChanging(row)">启用</el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm
            v-if="statusOf(row) === AccountStatus.LOCKED"
            title="确认解锁该用户？"
            confirm-button-text="确认"
            cancel-button-text="取消"
            @confirm="enable(row)"
          >
            <template #reference>
              <el-button link type="success" :disabled="rowChanging(row)">解锁</el-button>
            </template>
          </el-popconfirm>
          <!-- SEALED 走 L2 高危对话框（契约 9.2-2），由页面承接 -->
          <el-button
            v-if="statusOf(row) === AccountStatus.NORMAL"
            link
            type="danger"
            :disabled="rowChanging(row)"
            @click="seal(row)"
          >
            封存
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="user-table-card__pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="refresh()"
        @size-change="refresh(1)"
      />
    </div>
  </el-card>
</template>

<style scoped>
.user-table-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.user-table-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.user-table-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.user-table-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.user-table-card__query {
  margin-bottom: var(--space-2);
}

.user-table-card__status-select {
  width: 160px;
}

.user-table-card__error {
  margin-bottom: var(--space-3);
}

.user-table-card__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}
</style>
