import type { MockMethod } from 'vite-plugin-mock'
import { orgs, subtreeIds } from './org'

/**
 * 用户模块契约级 Mock（契约 8.4 / 附录 B）
 * - 列表分页返回 PageResult{total,page,pageSize,list}；orgId 过滤按组织子树
 * - 响应为后端 UserView 形状（realName 明文姓名 / phoneMasked 脱敏电话），不含 nameEnc/phoneEnc/*Hash（契约 10.3）
 * - 写接口 query 传参（postForm/putForm 契约 4.2）
 */

const now = '2026-10-24T15:30:00'

type AccountStatus = 'NORMAL' | 'DISABLED' | 'SEALED' | 'LOCKED'

interface MockUser {
  id: number
  userNo: string
  realName: string
  phoneMasked: string | null
  orgId: number
  post?: string
  accountStatus: AccountStatus
  leaveTime?: string
  lastLoginAt?: string
  isDeleted: number
  createdAt: string
  updatedAt: string
}

let nextUserId = 100

export const users: MockUser[] = [
  u(1, 'U1001', '张*丰', '138****1021', 1, '总经理', 'NORMAL', undefined, '2026-10-24 09:12:33', '2025-01-10 09:00:00'),
  u(2, 'U1002', '李*敏', '139****3082', 1, '副总经理', 'NORMAL', undefined, '2026-10-23 18:40:11', '2025-01-10 09:05:00'),
  u(3, 'U1003', '王*强', '136****7745', 4, '中心主任', 'NORMAL', undefined, '2026-10-24 08:55:02', '2025-01-15 10:00:00'),
  u(4, 'U1004', '赵*静', '137****2216', 4, '架构师', 'NORMAL', undefined, '2026-10-22 14:22:47', '2025-01-15 10:10:00'),
  u(5, 'U1005', '陈*宇', '135****9930', 4, '开发工程师', 'NORMAL', undefined, '2026-10-24 10:01:19', '2025-02-01 09:00:00'),
  u(6, 'U1006', '刘*洋', '138****4417', 2, '分公司经理', 'NORMAL', undefined, '2026-10-21 16:38:50', '2025-02-20 09:00:00'),
  u(7, 'U1007', '周*红', '139****6652', 2, '行政主管', 'NORMAL', undefined, '2026-10-23 11:15:36', '2025-02-20 09:20:00'),
  u(8, 'U1008', '吴*斌', '136****8874', 3, '分公司经理', 'NORMAL', undefined, '2026-10-20 09:47:28', '2025-02-21 09:00:00'),
  u(9, 'U1009', '徐*丽', '137****1195', 3, '人事专员', 'DISABLED', undefined, '2026-08-14 10:30:00', '2025-03-01 09:00:00'),
  u(10, 'U1010', '孙*磊', '135****3368', 5, '行政专员', 'NORMAL', undefined, '2026-10-23 15:12:09', '2025-03-05 09:00:00'),
  u(11, 'U1011', '马*芳', '138****5523', 5, '前台文员', 'NORMAL', undefined, '2026-10-18 17:26:44', '2025-03-05 09:30:00'),
  u(12, 'U1012', '朱*军', '139****7719', 6, '财务主管', 'NORMAL', undefined, '2026-10-24 08:31:57', '2025-03-05 10:00:00'),
  u(13, 'U1013', '胡*燕', '136****2260', 6, '会计', 'NORMAL', undefined, '2026-10-19 13:44:32', '2025-03-05 10:15:00'),
  u(14, 'U1014', '郭*涛', '137****9948', 6, '出纳', 'LOCKED', undefined, '2026-09-30 22:18:05', '2025-03-06 09:00:00'),
  u(15, 'U1015', '何*平', '135****4411', 7, '人事主管', 'NORMAL', undefined, '2026-10-22 10:09:26', '2025-03-06 09:30:00'),
  u(16, 'U1016', '高*欣', '138****6673', 7, '薪酬专员', 'NORMAL', undefined, '2026-10-21 09:52:13', '2025-03-06 10:00:00'),
  u(17, 'U1017', '林*杰', '139****8214', 8, '项目经理', 'NORMAL', undefined, '2026-10-24 07:58:40', '2025-04-18 09:00:00'),
  u(18, 'U1018', '罗*梅', '136****3357', 8, '安全员', 'NORMAL', undefined, '2026-10-17 16:20:31', '2025-04-18 09:30:00'),
  u(19, 'U1019', '郑*辉', '137****5506', 9, '项目经理', 'NORMAL', undefined, '2026-10-23 14:11:52', '2025-05-20 09:00:00'),
  u(20, 'U1020', '黄*婷', '135****7792', 9, '资料员', 'NORMAL', undefined, '2026-10-16 11:34:18', '2025-05-20 09:40:00'),
  u(21, 'U1021', '宋*超', '138****2219', 10, '项目经理', 'NORMAL', undefined, '2026-10-22 08:44:07', '2025-06-02 09:00:00'),
  u(22, 'U1022', '唐*雯', '139****4485', 10, '造价工程师', 'DISABLED', undefined, '2026-07-28 15:55:23', '2025-06-02 09:30:00'),
  u(23, 'U1023', '许*成', '136****9931', 2, '市场专员', 'SEALED', '2026-09-15 18:00:00', '2026-09-12 10:20:15', '2025-02-20 10:00:00'),
  u(24, 'U1024', '冯*兰', '137****3347', 6, '审计专员', 'SEALED', '2026-10-10 18:00:00', '2026-10-09 09:12:44', '2025-03-05 11:00:00'),
  u(25, 'U1025', '邓*凯', '135****6682', 4, '运维工程师', 'NORMAL', undefined, '2026-10-24 09:41:08', '2025-07-01 09:00:00'),
  u(26, 'U1026', '曹*宁', '138****0153', 4, '测试工程师', 'NORMAL', undefined, '2026-10-23 17:02:37', '2025-07-01 09:20:00'),
]

function u(
  id: number,
  userNo: string,
  realName: string,
  phoneMasked: string,
  orgId: number,
  post: string,
  accountStatus: AccountStatus,
  leaveTime?: string,
  lastLoginAt?: string,
  createdAt?: string,
): MockUser {
  return {
    id,
    userNo,
    realName,
    phoneMasked,
    orgId,
    post,
    accountStatus,
    leaveTime,
    lastLoginAt,
    isDeleted: 0,
    createdAt: createdAt ?? now,
    updatedAt: createdAt ?? now,
  }
}

function ok(data: unknown) {
  return { code: 0, message: 'success', data, timestamp: now }
}

function fail(code: number, message: string) {
  return { code, message, timestamp: now }
}

function findById(id: number): MockUser | undefined {
  return users.find((item) => item.id === id && item.isDeleted === 0)
}

/** 脱敏视图输出：空可选字段整体省略，模拟 @JsonInclude(NON_NULL)（契约 3.4） */
function toView(user: MockUser): Record<string, unknown> {
  const view: Record<string, unknown> = { ...user }
  if (!user.leaveTime) {
    delete view.leaveTime
  }
  if (!user.lastLoginAt) {
    delete view.lastLoginAt
  }
  if (!user.post) {
    delete view.post
  }
  return view
}

function maskPhone(phone: string): string {
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

const VALID_STATUS: AccountStatus[] = ['NORMAL', 'DISABLED', 'SEALED', 'LOCKED']

function idFromUrl(url: string, suffix: string): number | null {
  const path = url.split('?')[0]
  const match = new RegExp(`/users/(\\d+)${suffix}$`).exec(path)
  return match ? Number(match[1]) : null
}

export default [
  {
    url: '/api/v1/users',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => {
      const page = Math.max(1, Number(query.page ?? 1) || 1)
      const size = Math.max(1, Number(query.size ?? 20) || 20)
      let list = users.filter((item) => item.isDeleted === 0)
      if (query.orgId) {
        const scope = new Set(subtreeIds(Number(query.orgId)))
        list = list.filter((item) => scope.has(item.orgId))
      }
      if (query.accountStatus) {
        list = list.filter((item) => item.accountStatus === query.accountStatus)
      }
      list = [...list].sort((a, b) => a.id - b.id)
      const total = list.length
      const start = (page - 1) * size
      return ok({
        total,
        page,
        pageSize: size,
        list: list.slice(start, start + size).map(toView),
      })
    },
  },
  {
    url: /\/api\/v1\/users\/\d+$/,
    method: 'get',
    response: ({ url }: { url: string }) => {
      const id = idFromUrl(url, '')
      const found = id === null ? undefined : findById(id)
      return found ? ok(toView(found)) : fail(1002, '用户不存在')
    },
  },
  {
    url: '/api/v1/users',
    method: 'post',
    response: ({ query }: { query: Record<string, string> }) => {
      const userNo = (query.userNo ?? '').trim()
      const realName = (query.realName ?? '').trim()
      const phone = (query.phone ?? '').trim()
      const orgId = Number(query.orgId)
      const post = (query.post ?? '').trim()
      const problems: string[] = []
      if (!userNo) problems.push('userNo: 用户账号不能为空')
      if (!realName) problems.push('realName: 真实姓名不能为空')
      if (phone && !/^1[3-9]\d{9}$/.test(phone)) problems.push('phone: 手机号格式不正确')
      if (!Number.isInteger(orgId)) problems.push('orgId: 所属组织不能为空')
      if (problems.length > 0) {
        return fail(1001, problems.join('; '))
      }
      if (users.some((item) => item.userNo === userNo)) {
        return fail(3009, `账号已存在: ${userNo}`)
      }
      if (!orgs.some((item) => item.id === orgId && item.isDeleted === 0)) {
        return fail(1002, '所属组织不存在')
      }
      const created: MockUser = {
        id: nextUserId++,
        userNo,
        realName,
        phoneMasked: phone ? maskPhone(phone) : null,
        orgId,
        post: post || undefined,
        accountStatus: 'NORMAL',
        isDeleted: 0,
        createdAt: now,
        updatedAt: now,
      }
      users.push(created)
      return ok(toView(created))
    },
  },
  {
    url: /\/api\/v1\/users\/\d+\/status$/,
    method: 'put',
    response: ({ url, query }: { url: string; query: Record<string, string> }) => {
      const id = idFromUrl(url, '/status')
      const user = id === null ? undefined : findById(id)
      if (!user) {
        return fail(1002, '用户不存在')
      }
      const targetStatus = (query.targetStatus ?? '') as AccountStatus
      if (!VALID_STATUS.includes(targetStatus)) {
        return fail(1001, 'targetStatus: 目标状态无效')
      }
      if (user.accountStatus === targetStatus) {
        return fail(3004, `用户已是「${targetStatus}」状态，无需重复操作`)
      }
      user.accountStatus = targetStatus
      user.updatedAt = now
      if (targetStatus === 'SEALED') {
        user.leaveTime = now
      } else {
        user.leaveTime = undefined
      }
      return ok(toView(user))
    },
  },
] as MockMethod[]
