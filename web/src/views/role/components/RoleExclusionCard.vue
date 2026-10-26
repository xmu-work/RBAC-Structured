<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { listRoleExclusions, listRoles } from '@/api/role.api'
import type { Role, RoleExclusion } from '@/types/role.types'
import { dateTimeColumnFormatter } from '@/utils/time'

/**
 * 互斥配置卡片（契约 8.5 / UC-05）：成对展示，配置即生效，新增走 L2 确认对话框；
 * 回收端点 [缺]，已配置项不可移除。
 */
const props = defineProps<{
  roleId: number | null
  reloadSignal: number
}>()

const emit = defineEmits<{ add: []; loaded: [exclusions: RoleExclusion[]] }>()

const loading = ref(false)
const error = ref<string | null>(null)
const exclusions = ref<RoleExclusion[]>([])
const roleMap = ref<Record<number, Role>>({})

const rows = computed(() =>
  exclusions.value.map((item) => ({
    ...item,
    excludedRoleName: roleMap.value[item.excludedRoleId]?.roleName ?? `#${item.excludedRoleId}`,
    excludedRoleCode: roleMap.value[item.excludedRoleId]?.roleCode ?? '',
  })),
)

watch(
  () => props.roleId,
  () => void load(),
  { immediate: true },
)

watch(
  () => props.reloadSignal,
  () => void load(),
)

async function load(): Promise<void> {
  const roleId = props.roleId
  if (roleId === null) {
    exclusions.value = []
    emit('loaded', [])
    return
  }
  loading.value = true
  error.value = null
  try {
    const [list, allRoles] = await Promise.all([listRoleExclusions(roleId), listRoles()])
    if (props.roleId !== roleId) {
      return
    }
    exclusions.value = list
    roleMap.value = Object.fromEntries(allRoles.map((role) => [role.id, role]))
    emit('loaded', list)
  } catch (caught) {
    if (props.roleId !== roleId) {
      return
    }
    exclusions.value = []
    error.value = caught instanceof Error ? caught.message : '互斥配置加载失败'
  } finally {
    if (props.roleId === roleId) {
      loading.value = false
    }
  }
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「互斥配置」这一块视觉区域 -->
  <el-card class="role-exclusion-card" shadow="never">
    <template #header>
      <div class="role-exclusion-card__header">
        <div>
          <h2 class="role-exclusion-card__title">互斥角色</h2>
          <p class="role-exclusion-card__hint">
            互斥角色不能同时授予同一用户（UC-05）；配置即生效
          </p>
        </div>
        <el-button type="primary" plain :icon="Plus" :disabled="roleId === null" @click="emit('add')">
          添加互斥
        </el-button>
      </div>
    </template>

    <el-skeleton v-if="loading" :rows="3" animated />
    <el-result v-else-if="error" icon="error" title="互斥配置加载失败" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="load">重试</el-button>
      </template>
    </el-result>
    <el-empty
      v-else-if="roleId === null"
      description="请先在左侧选择角色"
      :image-size="80"
    />
    <el-empty
      v-else-if="rows.length === 0"
      description="该角色暂无互斥配置"
      :image-size="80"
    />
    <el-table v-else :data="rows" size="small">
      <el-table-column label="互斥角色" min-width="140">
        <template #default="{ row }">
          <span class="role-exclusion-card__pair">{{ row.excludedRoleName }}</span>
          <span class="role-exclusion-card__code">{{ row.excludedRoleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="配置原因" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="配置时间" min-width="160" :formatter="dateTimeColumnFormatter" />
    </el-table>
  </el-card>
</template>

<style scoped>
.role-exclusion-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.role-exclusion-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.role-exclusion-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.role-exclusion-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.role-exclusion-card__pair {
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
}

.role-exclusion-card__code {
  margin-left: var(--space-2);
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>
