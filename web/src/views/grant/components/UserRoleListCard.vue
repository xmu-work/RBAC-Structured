<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { listRoles, listUserRoles } from '@/api/role.api'
import { useDict } from '@/composables/useDict'
import { GrantStatus } from '@/constants/enums'
import type { Role, UserRole } from '@/types/role.types'

/**
 * 用户角色列表卡（契约 8.6）：展示生效/过期/回收状态；临期 7 天徽标提醒；
 * 回收走 L2 高危确认（由页面承接对话框）。
 */
const props = defineProps<{
  userId: number | null
  reloadSignal: number
}>()

const emit = defineEmits<{ revoke: [grant: UserRole] }>()

const { dictLabel, grantStatusOptions } = useDict()

const loading = ref(false)
const error = ref<string | null>(null)
const grants = ref<UserRole[]>([])
const roleMap = ref<Record<number, Role>>({})

const rows = computed(() =>
  grants.value.map((grant) => ({
    ...grant,
    roleName: roleMap.value[grant.roleId]?.roleName ?? `#${grant.roleId}`,
    roleCode: roleMap.value[grant.roleId]?.roleCode ?? '',
    expiringSoon: isExpiringSoon(grant),
  })),
)

/** 契约 8.6：expireTime 临期 7 天徽标（仅生效中的授权） */
function isExpiringSoon(grant: UserRole): boolean {
  if (grant.status !== GrantStatus.ACTIVE || !grant.expireTime) {
    return false
  }
  const days = dayjs(grant.expireTime).diff(dayjs(), 'day')
  return days >= 0 && days <= 7
}

watch(
  () => [props.userId, props.reloadSignal] as const,
  () => void load(),
  { immediate: true },
)

async function load(): Promise<void> {
  const userId = props.userId
  if (userId === null) {
    grants.value = []
    return
  }
  loading.value = true
  error.value = null
  try {
    const [list, roles] = await Promise.all([listUserRoles(userId), listRoles()])
    if (props.userId !== userId) {
      return
    }
    grants.value = list
    roleMap.value = Object.fromEntries(roles.map((role) => [role.id, role]))
  } catch (caught) {
    if (props.userId !== userId) {
      return
    }
    grants.value = []
    error.value = caught instanceof Error ? caught.message : '用户角色加载失败'
  } finally {
    if (props.userId === userId) {
      loading.value = false
    }
  }
}

function asGrant(row: unknown): UserRole {
  return row as UserRole
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「用户角色」这一块视觉区域 -->
  <el-card class="user-role-card" shadow="never">
    <template #header>
      <div class="user-role-card__header">
        <div>
          <h2 class="user-role-card__title">用户角色</h2>
          <p class="user-role-card__hint">
            {{ userId === null ? '请先在上方选择用户' : `共 ${grants.length} 条授权记录` }}
          </p>
        </div>
      </div>
    </template>

    <el-result v-if="error" icon="error" title="用户角色加载失败" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="load">重试</el-button>
      </template>
    </el-result>
    <el-empty v-else-if="userId === null" description="选择用户后展示其角色授权" :image-size="90" />
    <el-table v-else v-loading="loading" :data="rows">
      <el-table-column label="角色" min-width="170">
        <template #default="{ row }">
          <span class="user-role-card__name">{{ row.roleName }}</span>
          <span class="user-role-card__code">{{ row.roleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="130">
        <template #default="{ row }">
          <el-tag
            :type="row.status === GrantStatus.ACTIVE ? 'success' : row.status === GrantStatus.EXPIRED ? 'warning' : 'info'"
            effect="light"
          >
            {{ dictLabel(grantStatusOptions, row.status) }}
          </el-tag>
          <el-tag v-if="row.expiringSoon" type="danger" size="small" effect="plain" class="user-role-card__expiring">
            临期
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效时间" min-width="150">
        <template #default="{ row }">{{ row.effectiveTime ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="过期时间" min-width="160">
        <template #default="{ row }">
          {{ row.expireTime ?? '长期' }}
        </template>
      </el-table-column>
      <el-table-column label="授予原因" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.grantReason ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="asGrant(row).status === GrantStatus.ACTIVE"
            link
            type="danger"
            @click="emit('revoke', asGrant(row))"
          >
            回收
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.user-role-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.user-role-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.user-role-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.user-role-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.user-role-card__name {
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
}

.user-role-card__code {
  margin-left: var(--space-2);
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}

.user-role-card__expiring {
  margin-left: var(--space-2);
}
</style>
