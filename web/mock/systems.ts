import type { MockMethod } from 'vite-plugin-mock'

/**
 * 系统 / 模块 / 功能点 / 角色-功能绑定 / 数据范围字典 契约级 Mock（契约 8.7 / 8.8 / 附录 B）
 * - [缺] 模块列表、功能点列表、矩阵查询、已有绑定查询端点（契约 11.2 D8），仅提供登记/绑定写接口
 * - 角色绑定走混合传参：query roleId/allowedAction + JSON Body functionPointIds（契约 4.2）
 */

const now = '2026-10-24 15:30:00'

type OperationAction =
  | 'NONE'
  | 'VIEW'
  | 'ALL'
  | 'VIEW_EDIT'
  | 'VIEW_ADD'
  | 'VIEW_APPROVE'
  | 'VIEW_EXPORT'
  | 'VIEW_EDIT_APPROVE'
  | 'VIEW_ADD_EDIT'
  | 'VIEW_EDIT_EXPORT'
  | 'VIEW_SELF'
  | 'VIEW_EDIT_SELF'

interface MockSystem {
  id: number
  systemCode: string
  systemName: string
  status: 'ACTIVE'
  description?: string
  isDeleted: number
  createdAt: string
  updatedAt: string
}

interface MockModule {
  id: number
  systemId: number
  moduleCode: string
  moduleName: string
  sortNo: number
  status: 'ACTIVE'
  isDeleted: number
  createdAt: string
  updatedAt: string
}

interface MockFunctionPoint {
  id: number
  moduleId: number
  pointCode: string
  pointName: string
  action: OperationAction
  apiPath?: string
  apiMethod?: string
  status: 'ACTIVE'
  isDeleted: number
  createdAt: string
  updatedAt: string
}

let nextSystemId = 100
let nextModuleId = 100
let nextPointId = 5000
let nextRoleFunctionId = 1

/** 角色-功能点绑定（与后端 IamRoleFunction 同形；已绑定的跳过，契约 8.7） */
const roleFunctions: {
  id: number
  roleId: number
  functionPointId: number
  allowedAction: OperationAction
  createdAt: string
}[] = []

const systems: MockSystem[] = [
  sys(1, 'ADMIN', '统一权限平台', '管理后台自身（含组织/用户/角色/授权/审计菜单功能点）', '2025-01-06 09:00:00'),
  sys(2, 'OA', '协同办公系统', '公文流转、会议与日程管理', '2025-03-01 09:00:00'),
  sys(3, 'CRM', '客户关系管理系统', '客户、商机与合同管理', '2025-05-10 09:00:00'),
  sys(4, 'FIN', '财务共享系统', '报销、付款与凭证共享', '2025-08-20 09:00:00'),
]

function sys(
  id: number,
  systemCode: string,
  systemName: string,
  description: string,
  createdAt: string,
): MockSystem {
  return {
    id,
    systemCode,
    systemName,
    status: 'ACTIVE',
    description,
    isDeleted: 0,
    createdAt,
    updatedAt: createdAt,
  }
}

const modules: MockModule[] = [
  mod(1, 1, 'M-ORG', '组织管理', 1, '2025-01-08 09:00:00'),
  mod(2, 1, 'M-USER', '用户管理', 2, '2025-01-08 09:10:00'),
  mod(3, 1, 'M-AUTH', '授权管理', 3, '2025-01-08 09:20:00'),
  mod(4, 1, 'M-AUDIT', '审计日志', 4, '2025-01-08 09:30:00'),
  mod(5, 2, 'M-DOC', '公文管理', 1, '2025-03-02 09:00:00'),
  mod(6, 2, 'M-MEET', '会议管理', 2, '2025-03-02 09:10:00'),
  mod(7, 3, 'M-CUST', '客户管理', 1, '2025-05-11 09:00:00'),
  mod(8, 4, 'M-EXP', '费用报销', 1, '2025-08-21 09:00:00'),
]

function mod(
  id: number,
  systemId: number,
  moduleCode: string,
  moduleName: string,
  sortNo: number,
  createdAt: string,
): MockModule {
  return {
    id,
    systemId,
    moduleCode,
    moduleName,
    sortNo,
    status: 'ACTIVE',
    isDeleted: 0,
    createdAt,
    updatedAt: createdAt,
  }
}

const VALID_ACTIONS: OperationAction[] = [
  'NONE',
  'VIEW',
  'ALL',
  'VIEW_EDIT',
  'VIEW_ADD',
  'VIEW_APPROVE',
  'VIEW_EXPORT',
  'VIEW_EDIT_APPROVE',
  'VIEW_ADD_EDIT',
  'VIEW_EDIT_EXPORT',
  'VIEW_SELF',
  'VIEW_EDIT_SELF',
]

function ok(data: unknown) {
  return { code: 0, message: 'success', data, timestamp: now }
}

function fail(code: number, message: string) {
  return { code, message, timestamp: now }
}

function systemView(item: MockSystem): Record<string, unknown> {
  const view: Record<string, unknown> = { ...item }
  if (!item.description) delete view.description
  return view
}

export default [
  /* ---------------- 数据范围字典（契约 8.8，dict store 消费） ---------------- */
  {
    url: '/api/v1/systems/data-scopes',
    method: 'get',
    response: () =>
      ok([
        {
          id: 1,
          scopeCode: 'ALL',
          scopeName: '全公司',
          description: '可访问全公司数据',
          sortNo: 1,
          status: 'ACTIVE',
          createdAt: now,
        },
        {
          id: 2,
          scopeCode: 'BRANCH',
          scopeName: '本分公司',
          description: '仅可访问所属分公司数据',
          sortNo: 2,
          status: 'ACTIVE',
          createdAt: now,
        },
        {
          id: 3,
          scopeCode: 'DEPT',
          scopeName: '本部门',
          description: '仅可访问所属部门数据',
          sortNo: 3,
          status: 'ACTIVE',
          createdAt: now,
        },
        {
          id: 4,
          scopeCode: 'PROJECT',
          scopeName: '本项目',
          description: '仅可访问所属项目部数据',
          sortNo: 4,
          status: 'ACTIVE',
          createdAt: now,
        },
        {
          id: 5,
          scopeCode: 'SELF',
          scopeName: '本人',
          description: '仅可访问本人数据',
          sortNo: 5,
          status: 'ACTIVE',
          createdAt: now,
        },
      ]),
  },
  {
    url: '/api/v1/systems/data-scopes',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const scopeCode = (query.scopeCode ?? '').trim()
      const scopeName = (query.scopeName ?? '').trim()
      const problems: string[] = []
      if (!scopeCode) problems.push('scopeCode: 范围编码不能为空')
      if (!scopeName) problems.push('scopeName: 范围名称不能为空')
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (['ALL', 'BRANCH', 'DEPT', 'PROJECT', 'SELF'].includes(scopeCode)) {
        return fail(1001, 'scopeCode: 内置数据范围编码不可重复登记')
      }
      return ok({
        id: 100,
        scopeCode,
        scopeName,
        description: (query.description ?? '').trim() || undefined,
        sortNo: Number(query.sortNo ?? 0) || 0,
        status: 'ACTIVE',
        createdAt: now,
      })
    },
  },

  /* ---------------- 系统（契约 8.7） ---------------- */
  {
    url: '/api/v1/systems',
    method: 'get',
    response: () => ok(systems.filter((item) => item.isDeleted === 0).map(systemView)),
  },
  {
    url: '/api/v1/systems',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const systemCode = (query.systemCode ?? '').trim()
      const systemName = (query.systemName ?? '').trim()
      const problems: string[] = []
      if (!/^[A-Z][A-Z0-9_]{1,31}$/.test(systemCode)) {
        problems.push('systemCode: 系统编码须以大写字母开头，2-32 位大写字母/数字/下划线')
      }
      if (!systemName) problems.push('systemName: 系统名称不能为空')
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (systems.some((item) => item.systemCode === systemCode)) {
        return fail(1001, 'systemCode: 系统编码已存在')
      }
      const created: MockSystem = {
        id: nextSystemId++,
        systemCode,
        systemName,
        status: 'ACTIVE',
        description: (query.description ?? '').trim() || undefined,
        isDeleted: 0,
        createdAt: now,
        updatedAt: now,
      }
      systems.push(created)
      return ok(systemView(created))
    },
  },

  /* ---------------- 模块登记（契约 8.7；[缺] 模块列表查询） ---------------- */
  {
    url: '/api/v1/systems/modules',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const systemId = Number(query.systemId)
      const moduleCode = (query.moduleCode ?? '').trim()
      const moduleName = (query.moduleName ?? '').trim()
      const problems: string[] = []
      if (!Number.isInteger(systemId)) problems.push('systemId: 所属系统不能为空')
      if (!moduleCode) problems.push('moduleCode: 模块编码不能为空')
      if (!moduleName) problems.push('moduleName: 模块名称不能为空')
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (!systems.some((item) => item.id === systemId && item.isDeleted === 0)) {
        return fail(1002, '所属系统不存在')
      }
      if (modules.some((item) => item.systemId === systemId && item.moduleCode === moduleCode)) {
        return fail(1001, 'moduleCode: 该系统下模块编码已存在')
      }
      const created: MockModule = {
        id: nextModuleId++,
        systemId,
        moduleCode,
        moduleName,
        sortNo: Number(query.sortNo ?? 0) || 0,
        status: 'ACTIVE',
        isDeleted: 0,
        createdAt: now,
        updatedAt: now,
      }
      modules.push(created)
      return ok({ ...created })
    },
  },

  /* ---------------- 功能点登记（契约 8.7；[缺] 功能点列表查询） ---------------- */
  {
    url: '/api/v1/systems/function-points',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const moduleId = Number(query.moduleId)
      const pointCode = (query.pointCode ?? '').trim()
      const pointName = (query.pointName ?? '').trim()
      const action = (query.action ?? '').trim() as OperationAction
      const problems: string[] = []
      if (!Number.isInteger(moduleId)) problems.push('moduleId: 所属模块不能为空')
      if (!pointCode) problems.push('pointCode: 功能点编码不能为空')
      if (!pointName) problems.push('pointName: 功能点名称不能为空')
      if (!VALID_ACTIONS.includes(action)) problems.push('action: 操作类型无效（12 取值之一）')
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (!modules.some((item) => item.id === moduleId && item.isDeleted === 0)) {
        return fail(1002, '所属模块不存在')
      }
      const created: MockFunctionPoint = {
        id: nextPointId++,
        moduleId,
        pointCode,
        pointName,
        action,
        apiPath: (query.apiPath ?? '').trim() || undefined,
        apiMethod: (query.apiMethod ?? '').trim() || undefined,
        status: 'ACTIVE',
        isDeleted: 0,
        createdAt: now,
        updatedAt: now,
      }
      return ok({ ...created })
    },
  },

  /* ---------------- 角色-功能绑定（契约 8.7：混合传参） ---------------- */
  {
    url: '/api/v1/systems/role-functions',
    method: 'post',
    response: ({
      query,
      body,
    }: {
      query: Record<string, string>
      body: unknown
    }) => {
      const roleId = Number(query.roleId)
      const allowedAction = (query.allowedAction ?? '').trim() as OperationAction
      const problems: string[] = []
      if (!Number.isInteger(roleId)) problems.push('roleId: 角色不能为空')
      if (!VALID_ACTIONS.includes(allowedAction)) {
        problems.push('allowedAction: 操作类型无效（12 取值之一）')
      }
      if (!Array.isArray(body) || body.length === 0 || !body.every((id) => Number.isInteger(id))) {
        problems.push('functionPointIds: 功能点 ID 集合不能为空且须为数字数组')
      }
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      for (const functionPointId of body as number[]) {
        if (!roleFunctions.some((rf) => rf.roleId === roleId && rf.functionPointId === functionPointId)) {
          roleFunctions.push({ id: nextRoleFunctionId++, roleId, functionPointId, allowedAction, createdAt: now })
        }
      }
      return ok(null)
    },
  },
  {
    url: /\/api\/v1\/systems\/role-functions\/\d+$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const roleId = Number(url.split('?')[0].split('/').pop())
      return ok(roleFunctions.filter((rf) => rf.roleId === roleId))
    },
  },
  {
    url: '/api/v1/systems/role-functions',
    method: 'delete',
    response: ({ query, body }: { query: Record<string, string>; body: unknown }) => {
      const roleId = Number(query.roleId)
      const ids = Array.isArray(body) ? (body as number[]) : []
      for (let i = roleFunctions.length - 1; i >= 0; i--) {
        if (roleFunctions[i].roleId === roleId && ids.includes(roleFunctions[i].functionPointId)) {
          roleFunctions.splice(i, 1)
        }
      }
      return ok(null)
    },
  },

  /* ---------------- 角色-数据范围授权（契约 8.8） ---------------- */
  {
    url: '/api/v1/systems/role-data-scopes',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const roleId = Number(query.roleId)
      const scopeType = (query.scopeType ?? '').trim()
      const validScopes = ['ALL', 'BRANCH', 'DEPT', 'PROJECT', 'SELF']
      const problems: string[] = []
      if (!Number.isInteger(roleId)) problems.push('roleId: 角色不能为空')
      if (!validScopes.includes(scopeType)) problems.push('scopeType: 数据范围类型无效')
      if ((scopeType === 'BRANCH' || scopeType === 'DEPT') && !Number.isInteger(Number(query.orgId))) {
        problems.push('orgId: 分公司/部门范围必须指定组织')
      }
      if (scopeType === 'PROJECT' && !Number.isInteger(Number(query.projectId))) {
        problems.push('projectId: 项目范围必须指定项目部')
      }
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      return ok(null)
    },
  },
] as MockMethod[]
