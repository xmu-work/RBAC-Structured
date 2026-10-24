<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useDict } from '@/composables/useDict'
import { useOrgLazyLoad } from '@/composables/useOrgTree'
import { OrgType, type OrgTypeCode } from '@/constants/enums'

/** 用户列表左侧的组织筛选树（契约 8.4：组织树侧栏联动过滤）：只选不拖 */
const props = defineProps<{
  selectedOrgId: number | null
  reloadSignal: number
}>()

const emit = defineEmits<{ select: [orgId: number | null] }>()

const { dictLabel, orgTypeOptions } = useDict()

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const ORG_TYPE_TAG: Record<OrgTypeCode, TagType> = {
  [OrgType.HQ]: 'primary',
  [OrgType.BRANCH]: 'warning',
  [OrgType.DEPT]: 'success',
  [OrgType.PROJECT]: 'info',
}

const treeRef = ref<{ setCurrentKey: (key: number | null) => void }>()
const treeKey = ref(0)
const initializing = ref(true)
const rootError = ref<string | null>(null)
const rootCount = ref(-1)

const { loadChildren } = useOrgLazyLoad({
  onRootLoaded: (count) => {
    rootCount.value = count
    initializing.value = false
  },
  onRootError: (message) => {
    rootError.value = message
    initializing.value = false
  },
})

function handleNodeClick(data: { id?: number }): void {
  const id = data.id
  if (id === undefined) {
    return
  }
  // 再次点击当前节点 = 取消筛选
  if (props.selectedOrgId === id) {
    treeRef.value?.setCurrentKey(null)
    emit('select', null)
    return
  }
  emit('select', id)
}

async function reloadTree(): Promise<void> {
  initializing.value = true
  rootError.value = null
  treeKey.value += 1
  await nextTick()
  if (props.selectedOrgId !== null) {
    treeRef.value?.setCurrentKey(props.selectedOrgId)
  }
}

watch(
  () => props.reloadSignal,
  () => void reloadTree(),
)
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：只承载「组织筛选树」这一块视觉区域 -->
  <el-card class="org-filter-tree" shadow="never">
    <template #header>
      <div class="org-filter-tree__header">
        <div>
          <h2 class="org-filter-tree__title">按组织筛选</h2>
          <p class="org-filter-tree__hint">{{ selectedOrgId === null ? '当前：全部用户' : '再次点击节点取消筛选' }}</p>
        </div>
        <el-button :icon="Refresh" text bg @click="reloadTree">刷新</el-button>
      </div>
    </template>

    <el-result v-if="rootError" icon="error" title="组织树加载失败" :sub-title="rootError">
      <template #extra>
        <el-button type="primary" @click="reloadTree">重试</el-button>
      </template>
    </el-result>
    <el-empty v-else-if="!initializing && rootCount === 0" description="暂无组织数据" />
    <div v-else v-loading="initializing" class="org-filter-tree__body">
      <el-tree
        :key="treeKey"
        ref="treeRef"
        lazy
        node-key="id"
        highlight-current
        :load="loadChildren"
        :props="{ label: 'orgName' }"
        :current-node-key="selectedOrgId ?? undefined"
        @node-click="handleNodeClick"
      >
        <template #default="{ data }">
          <div class="org-filter-tree__node">
            <span class="org-filter-tree__node-name">{{ data.orgName }}</span>
            <el-tag
              size="small"
              :type="ORG_TYPE_TAG[data.orgType as OrgTypeCode] ?? 'info'"
              effect="light"
            >
              {{ dictLabel(orgTypeOptions, data.orgType) }}
            </el-tag>
          </div>
        </template>
      </el-tree>
    </div>
  </el-card>
</template>

<style scoped>
.org-filter-tree {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.org-filter-tree__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.org-filter-tree__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.org-filter-tree__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.org-filter-tree__body {
  max-height: var(--layout-content-min-height);
  overflow-y: auto;
}

.org-filter-tree__node {
  display: flex;
  flex: 1;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.org-filter-tree__node-name {
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
