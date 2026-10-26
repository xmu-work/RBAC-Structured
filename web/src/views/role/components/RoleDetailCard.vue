<script setup lang="ts">
import { ref, watch } from 'vue'
import { getRole } from '@/api/role.api'
import { useDict } from '@/composables/useDict'
import { RoleType } from '@/constants/enums'
import type { Role } from '@/types/role.types'
import { formatDateTime } from '@/utils/time'

/** 角色详情卡片（契约 8.5）：只读视图；更新/停用/删除、角色继承 [缺] 端点不提供入口 */
const props = defineProps<{ roleId: number | null }>()

const emit = defineEmits<{ loaded: [role: Role] }>()

const { dictLabel, roleTypeOptions, commonStatusOptions } = useDict()

const loading = ref(false)
const error = ref<string | null>(null)
const detail = ref<Role | null>(null)

watch(
  () => props.roleId,
  () => void loadDetail(),
  { immediate: true },
)

async function loadDetail(): Promise<void> {
  const roleId = props.roleId
  if (roleId === null) {
    detail.value = null
    error.value = null
    return
  }
  loading.value = true
  error.value = null
  try {
    const role = await getRole(roleId)
    if (props.roleId !== roleId) {
      return
    }
    detail.value = role
    emit('loaded', role)
  } catch (caught) {
    if (props.roleId !== roleId) {
      return
    }
    detail.value = null
    error.value = caught instanceof Error ? caught.message : '角色详情加载失败'
  } finally {
    if (props.roleId === roleId) {
      loading.value = false
    }
  }
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「角色详情」这一块视觉区域 -->
  <el-card class="role-detail-card" shadow="never">
    <template #header>
      <div class="role-detail-card__header">
        <div>
          <h2 class="role-detail-card__title">角色详情</h2>
          <p class="role-detail-card__hint">只读视图；编辑/停用/删除与角色继承待后端补齐端点</p>
        </div>
      </div>
    </template>

    <el-skeleton v-if="loading" :rows="4" animated />
    <el-result v-else-if="error" icon="error" title="角色详情加载失败" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </template>
    </el-result>
    <el-empty v-else-if="!detail" description="请在左侧选择角色查看详情" />
    <template v-else>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="角色编码">{{ detail.roleCode }}</el-descriptions-item>
        <el-descriptions-item label="角色名称">{{ detail.roleName }}</el-descriptions-item>
        <el-descriptions-item label="角色类型">
          <el-tag :type="detail.roleType === RoleType.FUNC ? 'primary' : 'success'" effect="light">
            {{ dictLabel(roleTypeOptions, detail.roleType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success" effect="light">
            {{ dictLabel(commonStatusOptions, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ detail.description ?? '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-card>
</template>

<style scoped>
.role-detail-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.role-detail-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.role-detail-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.role-detail-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
