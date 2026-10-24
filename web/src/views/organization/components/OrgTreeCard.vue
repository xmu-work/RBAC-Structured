<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getOrgChildren, getOrgTree, moveOrg } from '@/api/org.api'
import type { BusinessError } from '@/utils/request'
import { useDict } from '@/composables/useDict'
import { OrgType, type OrgTypeCode } from '@/constants/enums'
import type { Org } from '@/types/org.types'
import OrgMoveDialog from './OrgMoveDialog.vue'

type OrgTreeNode = Omit<Org, 'children'>

/** el-tree 懒加载/拖拽回调所需的最小节点结构（避免引 element-plus 内部 Node 类型） */
interface TreeNodeLike {
  level: number
  data: Partial<OrgTreeNode>
  parent?: TreeNodeLike | null
}

const props = defineProps<{
  reloadSignal: number
  selectedOrgId: number | null
}>()

const emit = defineEmits<{ select: [orgId: number | null] }>()

const { dictLabel, orgTypeOptions } = useDict()

/** 组织类型 → 徽标配色（视觉映射，文案仍出自 dict store，契约 9.2-7） */
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'
const ORG_TYPE_TAG: Record<OrgTypeCode, TagType> = {
  [OrgType.HQ]: 'primary',
  [OrgType.BRANCH]: 'warning',
  [OrgType.DEPT]: 'success',
  [OrgType.PROJECT]: 'info',
}

const treeRef = ref<{ setCurrentKey: (key: number | null) => void }>()
const treeKey = ref(0)
const expandedKeys = ref<number[]>([])
const rootCount = ref(-1)
const initializing = ref(true)
const rootError = ref<string | null>(null)

const conflictTargetId = ref<number | null>(null)
let conflictTimer: number | null = null

const moveDialogVisible = ref(false)
const moveSubmitting = ref(false)
const moveSource = ref<OrgTreeNode | null>(null)
const moveTarget = ref<OrgTreeNode | null>(null)

function stripChildren(org: Org): OrgTreeNode {
  return {
    id: org.id,
    parentId: org.parentId,
    ancestors: org.ancestors,
    orgCode: org.orgCode,
    orgName: org.orgName,
    orgType: org.orgType,
    orgLevel: org.orgLevel,
    sortNo: org.sortNo,
    status: org.status,
    isDeleted: org.isDeleted,
    createdAt: org.createdAt,
    updatedAt: org.updatedAt,
  }
}

/** 懒加载：根层走 GET /orgs，其余走 children 端点（契约 8.3） */
async function loadChildren(
  node: TreeNodeLike,
  resolve: (nodes: OrgTreeNode[]) => void,
): Promise<void> {
  try {
    const parentId = node.data.id
    const isRootLevel = node.level === 0 || parentId === undefined
    const list = isRootLevel ? await getOrgTree() : await getOrgChildren(parentId)
    if (isRootLevel) {
      rootCount.value = list.length
    }
    resolve(list.map(stripChildren))
  } catch (caught) {
    if (node.level === 0) {
      rootError.value = caught instanceof Error ? caught.message : '组织树加载失败'
    }
    resolve([])
  } finally {
    if (node.level === 0) {
      initializing.value = false
    }
  }
}

function handleNodeClick(data: OrgTreeNode): void {
  emit('select', data.id)
}

/** node-expand / node-collapse 事件签名是 (data, node, instance)，节点对象在第二个参数 */
function handleNodeExpand(data: unknown, node: TreeNodeLike): void {
  const id = node.data?.id ?? (data as Partial<OrgTreeNode> | null)?.id
  if (id !== undefined && !expandedKeys.value.includes(id)) {
    expandedKeys.value.push(id)
  }
}

function handleNodeCollapse(data: unknown, node: TreeNodeLike): void {
  const id = node.data?.id ?? (data as Partial<OrgTreeNode> | null)?.id
  if (id !== undefined) {
    expandedKeys.value = expandedKeys.value.filter((item) => item !== id)
  }
}

/** 重建树（保留展开与选中），用于新增/移动后的刷新 */
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

/* ---------------- 拖拽移动（契约 8.3：拖拽 + L2 确认；3003 还原并标红目标） ---------------- */

/** 顶级根节点无移动语义，不允许拖拽 */
function handleAllowDrag(node: TreeNodeLike): boolean {
  return node.level > 1
}

function isDescendantNode(
  node: TreeNodeLike | null | undefined,
  ancestorId: number | undefined,
): boolean {
  let current = node
  while (current && current.level > 0) {
    if (current.data.id !== undefined && current.data.id === ancestorId) {
      return true
    }
    current = current.parent ?? null
  }
  return false
}

function handleAllowDrop(
  dragging: TreeNodeLike,
  target: TreeNodeLike,
  dropType: string,
): boolean {
  const draggingId = dragging.data.id
  if (dropType === 'inner') {
    return !isDescendantNode(target, draggingId)
  }
  // before/after：等同挂到 target.parent 下；根层级与自身子级均不允许
  if (target.level <= 1) {
    return false
  }
  const parent = target.parent ?? null
  return (
    parent !== null &&
    parent.level > 0 &&
    parent.data.id !== draggingId &&
    !isDescendantNode(parent, draggingId)
  )
}

function handleNodeDrop(dragging: TreeNodeLike, target: TreeNodeLike, dropType: string): void {
  const newParent = dropType === 'inner' ? target : (target.parent ?? null)
  if (!newParent || newParent.level === 0) {
    void reloadTree()
    return
  }
  if (newParent.data.id === dragging.data.parentId) {
    ElMessage.info('已在同一父节点下，位置未变化')
    void reloadTree()
    return
  }
  moveSource.value = dragging.data as OrgTreeNode
  moveTarget.value = newParent.data as OrgTreeNode
  moveDialogVisible.value = true
}

async function handleMoveConfirm(): Promise<void> {
  if (!moveSource.value || !moveTarget.value) {
    return
  }
  const source = moveSource.value
  const target = moveTarget.value
  moveSubmitting.value = true
  try {
    await moveOrg(source.id, { newParentId: target.id })
    ElMessage.success(`「${source.orgName}」已移动到「${target.orgName}」下`)
    moveDialogVisible.value = false
    await reloadTree()
  } catch (error) {
    // 错误提示由响应拦截器统一 toast（契约 4.6）；此处仅做 3003 的结构还原与冲突标红
    moveDialogVisible.value = false
    const business = error as BusinessError
    if (business && typeof business === 'object' && business.code === 3003) {
      markConflictTarget(target.id)
    }
    await reloadTree()
  } finally {
    moveSubmitting.value = false
  }
}

/** 3003：标红目标节点（契约 8.3），数秒后自动消除 */
function markConflictTarget(orgId: number): void {
  conflictTargetId.value = orgId
  if (conflictTimer !== null) {
    window.clearTimeout(conflictTimer)
  }
  conflictTimer = window.setTimeout(() => {
    conflictTargetId.value = null
  }, 5000)
}

onBeforeUnmount(() => {
  if (conflictTimer !== null) {
    window.clearTimeout(conflictTimer)
  }
})
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：只承载「组织结构树」这一块视觉区域 -->
  <el-card class="org-tree-card" shadow="never">
    <template #header>
      <div class="org-tree-card__header">
        <div>
          <h2 class="org-tree-card__title">组织结构树</h2>
          <p class="org-tree-card__hint">点击节点查看详情；拖拽节点到目标父节点发起移动</p>
        </div>
        <el-button :icon="Refresh" text bg @click="reloadTree">刷新</el-button>
      </div>
    </template>

    <el-result v-if="rootError" icon="error" title="组织树加载失败" :sub-title="rootError">
      <template #extra>
        <el-button type="primary" @click="reloadTree">重试</el-button>
      </template>
    </el-result>
    <!-- 树必须常驻挂载：懒加载回调只挂在 el-tree 上，骨架/空态不能阻断其挂载（loading 用遮罩承载） -->
    <el-empty
      v-else-if="!initializing && rootCount === 0"
      description="暂无组织数据"
    />
    <div v-else v-loading="initializing" class="org-tree-card__body">
      <el-tree
        :key="treeKey"
        ref="treeRef"
        lazy
        node-key="id"
        highlight-current
        draggable
        :load="loadChildren"
        :props="{ label: 'orgName' }"
        :default-expanded-keys="expandedKeys"
        :current-node-key="selectedOrgId ?? undefined"
        :allow-drag="handleAllowDrag"
        :allow-drop="handleAllowDrop"
        @node-click="handleNodeClick"
        @node-expand="handleNodeExpand"
        @node-collapse="handleNodeCollapse"
        @node-drop="handleNodeDrop"
      >
        <template #default="{ data }">
          <div
            class="org-tree-node"
            :class="{ 'org-tree-node--conflict': conflictTargetId === data.id }"
          >
            <span class="org-tree-node__name">{{ data.orgName }}</span>
            <span class="org-tree-node__code">{{ data.orgCode }}</span>
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

  <OrgMoveDialog
    v-model="moveDialogVisible"
    :source="moveSource"
    :target-parent="moveTarget"
    :loading="moveSubmitting"
    @confirm="handleMoveConfirm"
  />
</template>

<style scoped>
.org-tree-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.org-tree-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.org-tree-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.org-tree-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.org-tree-card__body {
  max-height: var(--layout-content-min-height);
  min-height: calc(var(--font-size-title) * 4);
  overflow-y: auto;
}

.org-tree-node {
  display: flex;
  flex: 1;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  padding-right: var(--space-2);
}

.org-tree-node__name {
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-tree-node__code {
  overflow: hidden;
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 3003 移动非法时的目标节点标红（契约 8.3） */
.org-tree-node--conflict {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  animation: org-tree-conflict-blink 1s ease-in-out infinite;
}

.org-tree-node--conflict .org-tree-node__name,
.org-tree-node--conflict .org-tree-node__code {
  color: var(--color-danger);
  font-weight: 600;
}

@keyframes org-tree-conflict-blink {
  0%,
  100% {
    background: var(--color-bg-card);
  }

  50% {
    background: var(--color-primary-light-9);
  }
}
</style>
