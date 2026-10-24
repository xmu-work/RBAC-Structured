import type { MockMethod } from 'vite-plugin-mock'
import { users } from './user'

/**
 * 角色 + 互斥 + 授权契约级 Mock（契约 8.5 / 8.6 / 附录 B）
 * - GET /roles 全量不分页；写接口 query 传参
 * - 授权时间用相对真实时钟生成，便于演示「临期 7 天徽标 / 已过期」状态
 */

const now = () => fmt(0)

type RoleType = 'FUNC' | 'DATA'

interface MockRole {
  id: number
  roleCode: string
  roleName: string
  roleType: RoleType
  parentId: number | null
  status: 'ACTIVE'
  description?: string
  isDeleted: number
  createdAt: string
  updatedAt: string
}

interface MockExclusion {
  id: number
  roleId: number
  excludedRoleId: number
  description?: string
  createdAt: string
  createdBy?: number
}

type GrantStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED'

interface MockGrant {
  id: number
  userId: number
  roleId: number
  effectiveTime?: string
  expireTime?: string
  grantBy?: number
  grantReason?: string
  status: GrantStatus
  createdAt: string
  updatedAt: string
}

let nextRoleId = 100
let nextExclusionId = 100
let nextGrantId = 100

/** 相对真实时钟的时间串（yyyy-MM-dd HH:mm:ss），供临期/过期演示 */
function fmt(offsetDays: number, hour = 12, minute = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  d.setHours(hour, minute, 0, 0)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const roles: MockRole[] = [
  r(1, 'SYS_ADMIN', '系统管理员', 'FUNC', '平台全量管理权限', '2025-01-06 09:00:00'),
  r(2, 'FINANCE_OFFICER', '财务经办', 'DATA', '财务单据经办岗数据权限', '2025-01-20 10:00:00'),
  r(3, 'FINANCE_MANAGER', '财务主管', 'DATA', '财务单据审批与全分公司财务数据', '2025-01-20 10:10:00'),
  r(4, 'AUDITOR', '审计员', 'FUNC', '审计日志查询与合规检查', '2025-02-01 09:00:00'),
  r(5, 'HR_SPECIALIST', '人事专员', 'FUNC', '员工档案与账号管理', '2025-02-15 09:00:00'),
  r(6, 'PROJECT_MANAGER', '项目经理', 'FUNC', '项目部业务管理', '2025-03-10 09:00:00'),
  r(7, 'SELF_DATA_VIEWER', '本人数据查看', 'DATA', '仅可查看本人相关数据', '2025-04-01 09:00:00'),
]

function r(
  id: number,
  roleCode: string,
  roleName: string,
  roleType: RoleType,
  description: string,
  createdAt: string,
): MockRole {
  return {
    id,
    roleCode,
    roleName,
    roleType,
    parentId: null,
    status: 'ACTIVE',
    description,
    isDeleted: 0,
    createdAt,
    updatedAt: createdAt,
  }
}

const exclusions: MockExclusion[] = [
  e(1, 1, 4, '系统管理员与审计员职责分离（UC-05）', '2025-02-01 09:30:00'),
  e(2, 4, 1, '系统管理员与审计员职责分离（双向登记）', '2025-02-01 09:31:00'),
  e(3, 2, 3, '财务经办与财务审批岗位分离', '2025-03-12 14:00:00'),
  e(4, 3, 2, '财务经办与财务审批岗位分离（双向登记）', '2025-03-12 14:01:00'),
]

function e(
  id: number,
  roleId: number,
  excludedRoleId: number,
  reason: string,
  createdAt: string,
): MockExclusion {
  return { id, roleId, excludedRoleId, description: reason, createdAt, createdBy: 1 }
}

const grants: MockGrant[] = [
  g(1, 1, 1, 'ACTIVE', fmt(-280, 9), undefined, undefined, '平台初始化管理员授权'),
  g(2, 2, 4, 'ACTIVE', fmt(-260, 10, 5), fmt(-260, 10), undefined, '审计岗入职授权'),
  g(3, 4, 1, 'ACTIVE', fmt(-250, 14, 5), fmt(-250, 14), undefined, '数字化中心管理需要'),
  g(4, 12, 3, 'ACTIVE', fmt(-30, 9), fmt(-200, 9), fmt(3, 18), '年度财务专项临时授权'),
  g(5, 15, 5, 'EXPIRED', fmt(-400, 9), fmt(-400, 9), fmt(-30, 18), '试用期授权'),
  g(6, 17, 6, 'ACTIVE', fmt(-180, 8), fmt(-180, 8), undefined, '项目部组建授权'),
  g(7, 13, 2, 'ACTIVE', fmt(-150, 9), fmt(-150, 9), undefined, '会计岗标准授权'),
  g(8, 18, 6, 'REVOKED', fmt(-170, 9), fmt(-170, 9), fmt(-10, 18), '临时项目支持（已结束）'),
]

function g(
  id: number,
  userId: number,
  roleId: number,
  status: GrantStatus,
  createdAt: string,
  effectiveTime?: string,
  expireTime?: string,
  grantReason?: string,
): MockGrant {
  return {
    id,
    userId,
    roleId,
    effectiveTime,
    expireTime,
    grantBy: 1,
    grantReason,
    status,
    createdAt,
    updatedAt: createdAt,
  }
}

function ok(data: unknown) {
  return { code: 0, message: 'success', data, timestamp: now() }
}

function fail(code: number, message: string) {
  return { code, message, timestamp: now() }
}

function findRole(id: number): MockRole | undefined {
  return roles.find((item) => item.id === id && item.isDeleted === 0)
}

function findUser(id: number): boolean {
  return users.some((item) => item.id === id && item.isDeleted === 0)
}

function roleView(role: MockRole): Record<string, unknown> {
  const view: Record<string, unknown> = { ...role }
  delete view.parentId
  if (!role.description) delete view.description
  return view
}

function grantView(grant: MockGrant): Record<string, unknown> {
  const view: Record<string, unknown> = { ...grant }
  for (const key of ['effectiveTime', 'expireTime', 'grantBy', 'grantReason']) {
    if (view[key] === undefined) delete view[key]
  }
  return view
}

/** 请求里的 ISO 本地时间（YYYY-MM-DDTHH:mm:ss）转实体展示格式 */
function fromIso(value: string): string {
  return value.replace('T', ' ')
}

function idFromUrl(url: string, prefix: string, suffix: string): number | null {
  const path = url.split('?')[0]
  const match = new RegExp(`${prefix}/(\\d+)${suffix}$`).exec(path)
  return match ? Number(match[1]) : null
}

export default [
  /* ---------------- 角色（契约 8.5） ---------------- */
  {
    url: '/api/v1/roles',
    method: 'get',
    response: () => ok(roles.filter((item) => item.isDeleted === 0).map(roleView)),
  },
  {
    url: /\/api\/v1\/roles\/\d+$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const id = idFromUrl(url, '/roles', '')
      const found = id === null ? undefined : findRole(id)
      return found ? ok(roleView(found)) : fail(1002, '角色不存在')
    },
  },
  {
    url: '/api/v1/roles',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const roleCode = (query.roleCode ?? '').trim()
      const roleName = (query.roleName ?? '').trim()
      const roleType = (query.roleType ?? '').trim() as RoleType
      const description = (query.description ?? '').trim()
      const problems: string[] = []
      if (!roleCode) problems.push('roleCode: 角色编码不能为空')
      if (!roleName) problems.push('roleName: 角色名称不能为空')
      if (roleType !== 'FUNC' && roleType !== 'DATA') {
        problems.push('roleType: 角色类型必须为 FUNC 或 DATA')
      }
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (roles.some((item) => item.roleCode === roleCode)) {
        return fail(1001, 'roleCode: 角色编码已存在')
      }
      const created: MockRole = {
        id: nextRoleId++,
        roleCode,
        roleName,
        roleType,
        parentId: null,
        status: 'ACTIVE',
        description: description || undefined,
        isDeleted: 0,
        createdAt: now(),
        updatedAt: now(),
      }
      roles.push(created)
      return ok(roleView(created))
    },
  },

  /* ---------------- 互斥配置（契约 8.5 / UC-05） ---------------- */
  {
    url: /\/api\/v1\/roles\/\d+\/exclusions$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const id = idFromUrl(url, '/roles', '/exclusions')
      const role = id === null ? undefined : findRole(id)
      if (!role) {
        return fail(1002, '角色不存在')
      }
      return ok(exclusions.filter((item) => item.roleId === role.id))
    },
  },
  {
    url: /\/api\/v1\/roles\/\d+\/exclusions$/,
    method: 'post',
    response: ({ url, query }: { url: string; query: Record<string, string> }) => {
      const id = idFromUrl(url, '/roles', '/exclusions')
      const role = id === null ? undefined : findRole(id)
      if (!role) {
        return fail(1002, '角色不存在')
      }
      const excludedRoleId = Number(query.excludedRoleId)
      if (!Number.isInteger(excludedRoleId)) {
        return fail(1001, 'excludedRoleId: 互斥角色不能为空')
      }
      if (excludedRoleId === role.id) {
        return fail(1001, 'excludedRoleId: 不能与角色自身配置互斥')
      }
      const excluded = findRole(excludedRoleId)
      if (!excluded) {
        return fail(1002, '互斥角色不存在')
      }
      const duplicated = exclusions.some(
        (item) =>
          (item.roleId === role.id && item.excludedRoleId === excludedRoleId) ||
          (item.roleId === excludedRoleId && item.excludedRoleId === role.id),
      )
      if (duplicated) {
        return fail(1001, `excludedRoleId: 「${role.roleName}」与「${excluded.roleName}」的互斥关系已存在`)
      }
      exclusions.push({
        id: nextExclusionId++,
        roleId: role.id,
        excludedRoleId,
        description: (query.reason ?? '').trim() || undefined,
        createdAt: now(),
        createdBy: 1,
      })
      return ok(null)
    },
  },

  /* ---------------- 授权（契约 8.6，端点位于 /roles/grants） ---------------- */
  {
    url: /\/api\/v1\/roles\/grants\/\d+$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const userId = idFromUrl(url, '/roles/grants', '')
      if (userId === null || !findUser(userId)) {
        return fail(1002, '用户不存在')
      }
      return ok(grants.filter((item) => item.userId === userId).map(grantView))
    },
  },
  {
    url: '/api/v1/roles/grants',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const userId = Number(query.userId)
      const roleId = Number(query.roleId)
      const problems: string[] = []
      if (!Number.isInteger(userId)) problems.push('userId: 用户不能为空')
      if (!Number.isInteger(roleId)) problems.push('roleId: 角色不能为空')
      if (!query.grantReason?.trim()) problems.push('grantReason: 授权原因不能为空（留痕）')
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (!findUser(userId)) {
        return fail(1002, '用户不存在')
      }
      const role = findRole(roleId)
      if (!role) {
        return fail(1002, '角色不存在')
      }
      // 3005：重复授权（生效中）
      if (
        grants.some((item) => item.userId === userId && item.roleId === roleId && item.status === 'ACTIVE')
      ) {
        return fail(3005, `该用户已持有生效中的「${role.roleName}」角色，无需重复授权`)
      }
      // 3001：互斥冲突（UC-05）——与用户已持有的生效角色互斥
      const heldRoleIds = grants
        .filter((item) => item.userId === userId && item.status === 'ACTIVE')
        .map((item) => item.roleId)
      const conflict = exclusions.find(
        (item) =>
          (item.roleId === roleId && heldRoleIds.includes(item.excludedRoleId)) ||
          (item.excludedRoleId === roleId && heldRoleIds.includes(item.roleId)),
      )
      if (conflict) {
        const other =
          conflict.roleId === roleId ? findRole(conflict.excludedRoleId) : findRole(conflict.roleId)
        return fail(
          3001,
          `互斥角色冲突：目标角色「${role.roleName}」与用户已持有的「${other?.roleName ?? '?'}」角色互斥，禁止同时授予`,
        )
      }
      const created: MockGrant = {
        id: nextGrantId++,
        userId,
        roleId,
        effectiveTime: now(),
        expireTime: query.expireTime ? fromIso(query.expireTime) : undefined,
        grantBy: query.grantBy ? Number(query.grantBy) : 1,
        grantReason: query.grantReason.trim(),
        status: 'ACTIVE',
        createdAt: now(),
        updatedAt: now(),
      }
      grants.push(created)
      return ok(grantView(created))
    },
  },
  {
    url: '/api/v1/roles/grants',
    method: 'delete',
    response: ({ query }: { query: Record<string, string> }) => {
      const userId = Number(query.userId)
      const roleId = Number(query.roleId)
      if (!Number.isInteger(userId) || !Number.isInteger(roleId)) {
        return fail(1001, 'userId: 用户不能为空; roleId: 角色不能为空')
      }
      const grant = grants.find(
        (item) => item.userId === userId && item.roleId === roleId && item.status === 'ACTIVE',
      )
      if (!grant) {
        return fail(3004, '未找到该用户生效中的此角色授权记录')
      }
      grant.status = 'REVOKED'
      grant.updatedAt = now()
      return ok(null)
    },
  },
] as MockMethod[]
