<script setup lang="ts">
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getOrg } from '@/api/org.api'
import { useDict } from '@/composables/useDict'
import { OrgType } from '@/constants/enums'
import type { Org } from '@/types/org.types'
import { formatDateTime } from '@/utils/time'

/** 组织类型 → 徽标配色（视觉映射，文案仍出自 dict store，契约 9.2-7） */
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const ORG_TYPE_TAG: Record<string, TagType> = {
  [OrgType.HQ]: 'primary',
  [OrgType.BRANCH]: 'warning',
  [OrgType.DEPT]: 'success',
  [OrgType.PROJECT]: 'info',
}

const props = defineProps<{ orgId: number | null }>()
const emit = defineEmits<{ create: [org: Org]; loaded: [org: Org] }>()

const { dictLabel, orgTypeOptions, commonStatusOptions } = useDict()

const loading = ref(false)
const error = ref<string | null>(null)
const detail = ref<Org | null>(null)
const parentName = ref<string | null>(null)

watch(
  () => props.orgId,
  () => void loadDetail(),
  { immediate: true },
)

async function loadDetail(): Promise<void> {
  const orgId = props.orgId
  if (orgId === null) {
    detail.value = null
    parentName.value = null
    error.value = null
    return
  }
  loading.value = true
  error.value = null
  try {
    const org = await getOrg(orgId)
    if (props.orgId !== orgId) {
      return
    }
    detail.value = org
    emit('loaded', org)
    parentName.value = null
    // 实体内空值可能序列化为 null 而非缺省（仅 Result 外层省略 null），两者都按无父级处理
    if (org.parentId !== undefined && org.parentId !== null) {
      parentName.value = await getOrg(org.parentId)
        .then((parent) => parent.orgName)
        .catch(() => null)
    }
  } catch (caught) {
    if (props.orgId !== orgId) {
      return
    }
    detail.value = null
    error.value = caught instanceof Error ? caught.message : '组织详情加载失败'
  } finally {
    if (props.orgId === orgId) {
      loading.value = false
    }
  }
}

function dash(value: string | number | null | undefined): string {
  return value === null || value === undefined || value === '' ? '—' : String(value)
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：只承载「选中组织详情」这一块视觉区域 -->
  <el-card class="org-detail-card" shadow="never">
    <template #header>
      <div class="org-detail-card__header">
        <div>
          <h2 class="org-detail-card__title">组织详情</h2>
          <p class="org-detail-card__hint">只读视图；组织编辑/停用能力待后端补齐端点后开放</p>
        </div>
        <el-button
          type="primary"
          plain
          :icon="Plus"
          :disabled="!detail"
          @click="detail && emit('create', detail)"
        >
          新增子节点
        </el-button>
      </div>
    </template>

    <el-skeleton v-if="loading" :rows="5" animated />
    <el-result
      v-else-if="error"
      icon="error"
      title="组织详情加载失败"
      :sub-title="error"
    >
      <template #extra>
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </template>
    </el-result>
    <el-empty v-else-if="!detail" description="请在左侧选择组织节点查看详情" />
    <template v-else>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="组织名称">
          {{ detail.orgName }}
        </el-descriptions-item>
        <el-descriptions-item label="组织编码">
          {{ detail.orgCode }}
        </el-descriptions-item>
        <el-descriptions-item label="组织类型">
          <el-tag :type="ORG_TYPE_TAG[detail.orgType]" effect="light">
            {{ dictLabel(orgTypeOptions, detail.orgType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="组织层级">
          第 {{ detail.orgLevel }} 级
        </el-descriptions-item>
        <el-descriptions-item label="父组织">
          {{ parentName ?? (detail.parentId === undefined ? '—（顶级组织）' : '—') }}
        </el-descriptions-item>
        <el-descriptions-item label="排序号">
          {{ dash(detail.sortNo) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success" effect="light">
            {{ dictLabel(commonStatusOptions, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="负责人 ID">
          {{ dash(detail.leaderUserId) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(detail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(detail.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </el-card>
</template>

<style scoped>
.org-detail-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.org-detail-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.org-detail-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.org-detail-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

</style>
