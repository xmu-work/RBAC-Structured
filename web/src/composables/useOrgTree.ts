import { ref } from 'vue'
import { getOrg, getOrgChildren, getOrgTree } from '@/api/org.api'
import type { Org } from '@/types/org.types'

/** el-tree 懒加载/拖拽回调所需的最小节点结构（避免引 element-plus 内部 Node 类型） */
export interface TreeNodeLike {
  level: number
  data: Partial<OrgTreeNode>
  parent?: TreeNodeLike | null
}

export type OrgTreeNode = Omit<Org, 'children'>

export function stripChildren(org: Org): OrgTreeNode {
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

/**
 * 组织树懒加载共享逻辑（契约 8.3）：根层走 GET /orgs，其余走 children 端点。
 * 组织管理树、组织筛选树、组织选择器共用。
 */
export function useOrgLazyLoad(handlers?: {
  onRootLoaded?: (count: number) => void
  onRootError?: (message: string) => void
}) {
  async function loadChildren(
    node: TreeNodeLike,
    resolve: (nodes: OrgTreeNode[]) => void,
  ): Promise<void> {
    try {
      const parentId = node.data.id
      const isRootLevel = node.level === 0 || parentId === undefined
      const list = isRootLevel ? await getOrgTree() : await getOrgChildren(parentId)
      if (isRootLevel) {
        handlers?.onRootLoaded?.(list.length)
      }
      resolve(list.map(stripChildren))
    } catch (caught) {
      if (node.level === 0) {
        handlers?.onRootError?.(caught instanceof Error ? caught.message : '组织树加载失败')
      }
      resolve([])
    }
  }

  return { loadChildren }
}

/** 组织 ID → 组织名按需解析缓存（表格/详情展示组织名，接口只回 orgId） */
export function useOrgNames() {
  const names = ref<Record<number, string>>({})

  async function resolve(orgIds: Array<number | null | undefined>): Promise<void> {
    const missing = Array.from(
      new Set(
        orgIds.filter((id): id is number => id !== null && id !== undefined && !(id in names.value)),
      ),
    )
    if (missing.length === 0) {
      return
    }
    await Promise.all(
      missing.map(async (id) => {
        try {
          const org = await getOrg(id)
          names.value = { ...names.value, [id]: org.orgName }
        } catch {
          // 单个组织名解析失败不阻断列表展示，占位显示 #id
        }
      }),
    )
  }

  function nameOf(orgId: number | null | undefined): string {
    if (orgId === null || orgId === undefined) {
      return '—'
    }
    return names.value[orgId] ?? `#${orgId}`
  }

  return { names, resolve, nameOf }
}
