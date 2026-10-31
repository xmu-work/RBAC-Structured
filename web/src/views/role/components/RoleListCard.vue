<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { listRoles } from '@/api/role.api'
import { useDict } from '@/composables/useDict'
import type { Role } from '@/types/role.types'
import { ADMIN_API_FUNCTION_POINTS as FP } from '@/constants/permissions'
import { dateTimeColumnFormatter } from '@/utils/time'

/**
 * 角色列表卡片（契约 8.5）：后端全量返回不分页；本项目只有功能角色。
 * 更新/停用/复制/删除 [缺] 端点，UI 不提供入口（契约 11.2 待办）。
 */
const props = defineProps<{
  selectedRoleId: number | null
  reloadSignal: number
}>()

const emit = defineEmits<{ select: [roleId: number]; create: [] }>()

const { dictLabel, roleTypeOptions } = useDict()

const roles = ref<Role[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
async function loadRoles(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    roles.value = await listRoles()
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '角色列表加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadRoles()
})

watch(
  () => props.reloadSignal,
  () => void loadRoles(),
)

function handleCurrentChange(role: Role | null): void {
  if (role) {
    emit('select', role.id)
  }
}

defineExpose({ reload: loadRoles })
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「角色列表」这一块视觉区域 -->
  <el-card class="role-list-card" shadow="never">
    <template #header>
      <div class="role-list-card__header">
        <div>
          <h2 class="role-list-card__title">角色列表</h2>
          <p class="role-list-card__hint">全量 {{ roles.length }} 个 · 点击行查看详情与互斥配置</p>
        </div>
        <el-button v-perm="FP.ROLE_CREATE" type="primary" :icon="Plus" @click="emit('create')">新建角色</el-button>
      </div>
    </template>

    <el-alert
      v-if="error"
      class="role-list-card__error"
      type="error"
      :title="error"
      :closable="false"
      show-icon
    >
      <el-button size="small" type="primary" @click="loadRoles">重试</el-button>
    </el-alert>
    <el-table
      v-else
      v-loading="loading"
      :data="roles"
      highlight-current-row
      class="role-list-card__table"
      @current-change="handleCurrentChange"
    >
      <el-table-column prop="roleCode" label="角色编码" min-width="150" />
      <el-table-column prop="roleName" label="角色名称" min-width="120" />
      <el-table-column label="类型" min-width="120">
        <template #default="{ row }">
          <el-tag type="primary" effect="light">
            {{ dictLabel(roleTypeOptions, row.roleType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="160" :formatter="dateTimeColumnFormatter" />
    </el-table>
  </el-card>
</template>

<style scoped>
.role-list-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.role-list-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.role-list-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.role-list-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.role-list-card__error {
  margin-bottom: var(--space-3);
}
</style>
