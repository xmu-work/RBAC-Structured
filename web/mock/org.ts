import type { MockMethod } from 'vite-plugin-mock'

/**
 * 组织模块契约级 Mock（契约 8.3 / 附录 B）
 * - GET /orgs：无 rootId 返回顶层节点（子级经 children 端点懒加载）；带 rootId 返回整棵子树
 * - 写接口全部 query 传参（postForm 契约 4.2）
 */

const now = '2026-10-24 15:30:00'

type OrgType = 'HQ' | 'BRANCH' | 'DEPT' | 'PROJECT'

interface MockOrg {
  id: number
  parentId: number | null
  ancestors: string
  orgCode: string
  orgName: string
  orgType: OrgType
  orgLevel: number
  sortNo: number
  status: 'ACTIVE'
  isDeleted: number
  createdAt: string
  updatedAt: string
}

let nextOrgId = 100

export const orgs: MockOrg[] = [
  {
    id: 1,
    parentId: null,
    ancestors: '0',
    orgCode: 'HQ',
    orgName: '中恒建工集团',
    orgType: 'HQ',
    orgLevel: 1,
    sortNo: 1,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-01-06 09:00:00',
    updatedAt: '2025-06-30 10:00:00',
  },
  {
    id: 2,
    parentId: 1,
    ancestors: '0,1',
    orgCode: 'BR-EAST',
    orgName: '华东分公司',
    orgType: 'BRANCH',
    orgLevel: 2,
    sortNo: 1,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-02-11 09:30:00',
    updatedAt: '2025-02-11 09:30:00',
  },
  {
    id: 3,
    parentId: 1,
    ancestors: '0,1',
    orgCode: 'BR-SOUTH',
    orgName: '华南分公司',
    orgType: 'BRANCH',
    orgLevel: 2,
    sortNo: 2,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-02-12 10:00:00',
    updatedAt: '2025-02-12 10:00:00',
  },
  {
    id: 4,
    parentId: 1,
    ancestors: '0,1',
    orgCode: 'DP-DIGITAL',
    orgName: '数字化中心',
    orgType: 'DEPT',
    orgLevel: 3,
    sortNo: 3,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-03-01 14:00:00',
    updatedAt: '2025-03-01 14:00:00',
  },
  {
    id: 5,
    parentId: 2,
    ancestors: '0,1,2',
    orgCode: 'DP-EAST-GA',
    orgName: '综合管理部',
    orgType: 'DEPT',
    orgLevel: 3,
    sortNo: 1,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-03-05 09:00:00',
    updatedAt: '2025-03-05 09:00:00',
  },
  {
    id: 6,
    parentId: 2,
    ancestors: '0,1,2',
    orgCode: 'DP-EAST-FI',
    orgName: '财务部',
    orgType: 'DEPT',
    orgLevel: 3,
    sortNo: 2,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-03-05 09:10:00',
    updatedAt: '2025-03-05 09:10:00',
  },
  {
    id: 7,
    parentId: 3,
    ancestors: '0,1,3',
    orgCode: 'DP-SOUTH-HR',
    orgName: '人力资源部',
    orgType: 'DEPT',
    orgLevel: 3,
    sortNo: 1,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-03-06 11:00:00',
    updatedAt: '2025-03-06 11:00:00',
  },
  {
    id: 8,
    parentId: 6,
    ancestors: '0,1,2,6',
    orgCode: 'PR-SZ-METRO',
    orgName: '苏州轨道交通项目部',
    orgType: 'PROJECT',
    orgLevel: 4,
    sortNo: 1,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-04-18 15:00:00',
    updatedAt: '2025-04-18 15:00:00',
  },
  {
    id: 9,
    parentId: 2,
    ancestors: '0,1,2',
    orgCode: 'PR-SH-LINGANG',
    orgName: '上海临港项目部',
    orgType: 'PROJECT',
    orgLevel: 4,
    sortNo: 3,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-05-20 16:30:00',
    updatedAt: '2025-05-20 16:30:00',
  },
  {
    id: 10,
    parentId: 3,
    ancestors: '0,1,3',
    orgCode: 'PR-SZ-QIANHAI',
    orgName: '深圳前海项目部',
    orgType: 'PROJECT',
    orgLevel: 4,
    sortNo: 2,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt: '2025-06-02 10:30:00',
    updatedAt: '2025-06-02 10:30:00',
  },
]

function ok(data: unknown) {
  return { code: 0, message: 'success', data, timestamp: now }
}

function fail(code: number, message: string) {
  return { code, message, timestamp: now }
}

function findById(id: number): MockOrg | undefined {
  return orgs.find((item) => item.id === id && item.isDeleted === 0)
}

function childrenOf(parentId: number | null): MockOrg[] {
  return orgs
    .filter((item) => item.parentId === parentId && item.isDeleted === 0)
    .sort((a, b) => a.sortNo - b.sortNo)
}

function toNode(item: MockOrg, withChildren = false): Record<string, unknown> {
  const node: Record<string, unknown> = { ...item }
  if (withChildren) {
    node.children = childrenOf(item.id).map((child) => toNode(child, true))
  } else {
    delete node.children
  }
  return node
}

function buildAncestors(parentId: number): string {
  const parts: number[] = []
  let current = findById(parentId)
  while (current) {
    parts.unshift(current.id)
    current = current.parentId === null ? undefined : findById(current.parentId)
  }
  return ['0', ...parts].join(',')
}

function isDescendantOf(candidateId: number, ancestorId: number): boolean {
  let current = findById(candidateId)
  while (current && current.parentId !== null) {
    if (current.parentId === ancestorId) {
      return true
    }
    current = findById(current.parentId)
  }
  return false
}

/** 以 rootId 为根的子树全部组织 ID 集合（含自身），供用户 mock 按组织子树过滤共享 */
export function subtreeIds(rootId: number): number[] {
  const result: number[] = []
  const queue: number[] = [rootId]
  while (queue.length > 0) {
    const id = queue.shift() as number
    if (!findById(id)) {
      continue
    }
    result.push(id)
    queue.push(...childrenOf(id).map((child) => child.id))
  }
  return result
}

function orgIdFromUrl(url: string, suffix: string): number | null {
  // handler 收到的 url 可能带 query 串（如 ?newParentId=4），先剥再匹配
  const path = url.split('?')[0]
  const match = new RegExp(`/orgs/(\\d+)${suffix}$`).exec(path)
  return match ? Number(match[1]) : null
}

export default [
  {
    url: '/api/v1/orgs',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => {
      if (query.rootId) {
        const root = findById(Number(query.rootId))
        return root ? ok(toNode(root, true)) : fail(1002, '组织不存在')
      }
      return ok(childrenOf(null).map((item) => toNode(item)))
    },
  },
  {
    url: /\/api\/v1\/orgs\/\d+\/children$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const id = orgIdFromUrl(url, '/children')
      if (id === null) {
        return fail(1002, '组织不存在')
      }
      return findById(id) ? ok(childrenOf(id).map((item) => toNode(item))) : fail(1002, '组织不存在')
    },
  },
  {
    url: /\/api\/v1\/orgs\/\d+$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const id = orgIdFromUrl(url, '')
      if (id === null) {
        return fail(1002, '组织不存在')
      }
      const found = findById(id)
      return found ? ok(toNode(found)) : fail(1002, '组织不存在')
    },
  },
  {
    url: '/api/v1/orgs',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const orgCode = (query.orgCode ?? '').trim()
      const orgName = (query.orgName ?? '').trim()
      const orgType = (query.orgType ?? '').trim() as OrgType
      const orgLevel = Number(query.orgLevel)
      const problems: string[] = []
      if (!orgCode) problems.push('orgCode: 组织编码不能为空')
      if (!orgName) problems.push('orgName: 组织名称不能为空')
      if (!orgType) problems.push('orgType: 组织类型不能为空')
      if (!Number.isInteger(orgLevel) || orgLevel < 1 || orgLevel > 4) {
        problems.push('orgLevel: 组织层级必须为 1-4')
      }
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (orgs.some((item) => item.orgCode === orgCode)) {
        return fail(1001, 'orgCode: 组织编码已存在')
      }
      let parentId: number | null = null
      if (query.parentId) {
        parentId = Number(query.parentId)
        if (!findById(parentId)) {
          return fail(1002, '父组织不存在')
        }
      }
      const created: MockOrg = {
        id: nextOrgId++,
        parentId,
        ancestors: parentId === null ? '0' : buildAncestors(parentId),
        orgCode,
        orgName,
        orgType,
        orgLevel,
        sortNo: Number(query.sortNo ?? 0) || 0,
        status: 'ACTIVE',
        isDeleted: 0,
        createdAt: now,
        updatedAt: now,
      }
      orgs.push(created)
      return ok(toNode(created))
    },
  },
  {
    url: /\/api\/v1\/orgs\/\d+\/move$/,
    method: 'put',
    response: ({ url, query }: { url: string; query: Record<string, string> }) => {
      const id = orgIdFromUrl(url, '/move')
      const moving = id === null ? undefined : findById(id)
      if (!moving) {
        return fail(1002, '组织不存在')
      }
      const newParentId = Number(query.newParentId)
      if (!Number.isInteger(newParentId)) {
        return fail(1001, 'newParentId: 目标父组织不能为空')
      }
      if (newParentId === moving.id) {
        return fail(3003, '不能将组织移动到自身节点下')
      }
      const parent = findById(newParentId)
      if (!parent) {
        return fail(1002, '目标父组织不存在')
      }
      if (isDescendantOf(newParentId, moving.id)) {
        return fail(3003, '目标节点是当前节点的子级，不允许移动')
      }
      if (moving.parentId === newParentId) {
        return fail(3003, '组织已在目标父节点下，无需移动')
      }
      moving.parentId = newParentId
      moving.ancestors = buildAncestors(newParentId)
      moving.updatedAt = now
      return ok(toNode(moving))
    },
  },
] as MockMethod[]
