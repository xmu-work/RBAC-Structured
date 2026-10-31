import type { MockMethod } from 'vite-plugin-mock'

/**
 * 审计日志契约级 Mock（契约 8.9 / 附录 B）
 * - 时间范围必填（默认当天），startTime/endTime 为 ISO 本地格式（契约 3.3）
 * - 日志为永久保存数据：不提供任何删除端点（UC-06）
 * - 时间基于真实时钟生成近 3 天数据，默认当天查询可命中
 */

const now = () => fmt(0, 23, 59)

interface MockAuditLog {
  logId: number
  userId?: number
  username?: string
  action: string
  operateTime: string
  success: number
  target?: string
  ip?: string
  systemCode?: string
  detail?: string
  decisionId?: string
}

/** 相对真实时钟的时间串（yyyy-MM-dd HH:mm:ss） */
function fmt(offsetDays: number, hour: number, minute: number, second = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  d.setHours(hour, minute, second, 0)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const seeds: Array<{
  userId: number
  username: string
  action: string
  target: string
  systemCode: string
  detail: string
  success: number
}> = [
  { userId: 1, username: '张*丰', action: 'LOGIN', target: 'auth/login', systemCode: 'ADMIN', detail: '管理员登录成功', success: 1 },
  { userId: 4, username: '赵*静', action: 'LOGIN', target: 'auth/login', systemCode: 'ADMIN', detail: '登录成功', success: 1 },
  { userId: 4, username: '赵*静', action: 'LOGIN', target: 'auth/login', systemCode: 'ADMIN', detail: '密码错误一次后成功', success: 1 },
  { userId: 23, username: '许*成', action: 'LOGIN', target: 'auth/login', systemCode: 'ADMIN', detail: '账号已封存，登录被拒绝（UC-02）', success: 0 },
  { userId: 14, username: '郭*涛', action: 'LOGIN', target: 'auth/login', systemCode: 'ADMIN', detail: '账号锁定期间登录被拒绝', success: 0 },
  { userId: 1, username: '张*丰', action: 'CREATE_ORG', target: 'org#100', systemCode: 'ADMIN', detail: '新增组织节点：内审项目部', success: 1 },
  { userId: 1, username: '张*丰', action: 'MOVE_ORG', target: 'org#6', systemCode: 'ADMIN', detail: '移动组织「财务部」至「华南分公司」', success: 1 },
  { userId: 1, username: '张*丰', action: 'MOVE_ORG', target: 'org#8', systemCode: 'ADMIN', detail: '移动组织失败：目标节点是当前节点的子级（3003）', success: 0 },
  { userId: 2, username: '李*敏', action: 'CREATE_USER', target: 'user#100', systemCode: 'ADMIN', detail: '新建用户 U1027（测试员）', success: 1 },
  { userId: 5, username: '何*平', action: 'SEAL_USER', target: 'user#24', systemCode: 'ADMIN', detail: '封存用户 U1024（冯*兰）：离职留痕', success: 1 },
  { userId: 2, username: '李*敏', action: 'CHANGE_USER_STATUS', target: 'user#9', systemCode: 'ADMIN', detail: '用户 U1009 状态变更为 DISABLED', success: 1 },
  { userId: 2, username: '李*敏', action: 'CHANGE_USER_STATUS', target: 'user#14', systemCode: 'ADMIN', detail: '用户 U1014 解锁', success: 1 },
  { userId: 1, username: '张*丰', action: 'CREATE_ROLE', target: 'role#100', systemCode: 'ADMIN', detail: '新建角色：合同审计员', success: 1 },
  { userId: 1, username: '张*丰', action: 'CONFIG_EXCLUSION', target: 'role#6', systemCode: 'ADMIN', detail: '配置互斥：项目经理 ↔ 人事专员', success: 1 },
  { userId: 1, username: '张*丰', action: 'GRANT_ROLE', target: 'user#12', systemCode: 'ADMIN', detail: '向用户 U1012 授予「财务主管」，长期有效', success: 1 },
  { userId: 4, username: '赵*静', action: 'GRANT_ROLE', target: 'user#4', systemCode: 'ADMIN', detail: '授予「审计员」被拒绝：与已持有「系统管理员」互斥（UC-05，3001）', success: 0 },
  { userId: 1, username: '张*丰', action: 'GRANT_ROLE', target: 'user#12', systemCode: 'ADMIN', detail: '重复授予「财务主管」被拒绝（3005）', success: 0 },
  { userId: 1, username: '张*丰', action: 'REVOKE_ROLE', target: 'user#12', systemCode: 'ADMIN', detail: '回收用户 U1012 的「财务主管」授权', success: 1 },
  { userId: 4, username: '赵*静', action: 'BIND_ROLE_FUNCTION', target: 'role#1', systemCode: 'ADMIN', detail: '系统管理员绑定 3 个功能点（VIEW_EDIT）', success: 1 },
  { userId: 7, username: '周*红', action: 'SUBMIT_DOC', target: 'doc#2026-118', systemCode: 'OA', detail: '提交公文：关于开展年度内审的通知', success: 1 },
  { userId: 10, username: '孙*磊', action: 'APPROVE_DOC', target: 'doc#2026-118', systemCode: 'OA', detail: '审批通过公文', success: 1 },
  { userId: 11, username: '马*芳', action: 'EXPORT_MEETING', target: 'meeting/q4', systemCode: 'OA', detail: '导出四季度会议纪要', success: 1 },
  { userId: 12, username: '朱*军', action: 'CREATE_EXPENSE', target: 'exp#50231', systemCode: 'FIN', detail: '创建差旅报销单 50231', success: 1 },
  { userId: 13, username: '胡*燕', action: 'APPROVE_EXPENSE', target: 'exp#50231', systemCode: 'FIN', detail: '审批报销单 50231 通过', success: 1 },
  { userId: 13, username: '胡*燕', action: 'APPROVE_EXPENSE', target: 'exp#50244', systemCode: 'FIN', detail: '审批失败：金额超限需上级审批', success: 0 },
  { userId: 19, username: '郑*辉', action: 'VIEW_CUSTOMER', target: 'crm/customer', systemCode: 'CRM', detail: '查看客户列表（DEPT 范围）', success: 1 },
  { userId: 20, username: '黄*婷', action: 'EXPORT_CONTRACT', target: 'crm/contract', systemCode: 'CRM', detail: '导出合同台账', success: 1 },
  { userId: 5, username: '陈*宇', action: 'DEPLOY_CONFIG', target: 'admin/config', systemCode: 'ADMIN', detail: '修改权限缓存 TTL：300 → 180', success: 1 },
  { userId: 25, username: '邓*凯', action: 'API_SYNC', target: 'open/permissions', systemCode: 'ADMIN', detail: 'OA 系统拉取权限快照', success: 1 },
  { userId: 26, username: '曹*宁', action: 'API_SYNC', target: 'open/authorize/batch', systemCode: 'FIN', detail: 'FIN 系统批量鉴权 128 项', success: 1 },
]

const ips = ['10.8.16.21', '10.8.16.34', '172.16.4.10', '192.168.30.8', '10.8.20.117']

const logs: MockAuditLog[] = seeds.map((seed, index) => {
  const dayOffset = index % 3 === 0 ? 0 : index % 3 === 1 ? -1 : -2
  const hour = 8 + (index % 10)
  const minute = (index * 7) % 60
  const entry: MockAuditLog = {
    logId: 90000 + index,
    ...seed,
    operateTime: fmt(dayOffset, hour, minute, (index * 13) % 60),
    ip: ips[index % ips.length],
    decisionId: `D-2026-${String(1000 + index)}`,
  }
  return entry
})

function ok(data: unknown) {
  return { code: 0, message: 'success', data, timestamp: now() }
}

function fail(code: number, message: string) {
  return { code, message, timestamp: now() }
}

/** ISO 本地（YYYY-MM-DDTHH:mm:ss）→ 展示格式，便于字符串比较 */
function fromIso(value: string): string {
  return value.replace('T', ' ')
}

function toView(log: MockAuditLog): Record<string, unknown> {
  const view: Record<string, unknown> = { ...log }
  for (const key of ['userId', 'username', 'target', 'ip', 'systemCode', 'detail', 'decisionId']) {
    if (view[key] === undefined) {
      delete view[key]
    }
  }
  return view
}

export default [
  {
    url: '/api/v1/audit-logs',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => {
      // 契约 8.9：时间范围必填
      if (!query.startTime || !query.endTime) {
        return fail(1001, 'startTime: 开始时间不能为空; endTime: 结束时间不能为空')
      }
      const start = fromIso(query.startTime)
      const end = fromIso(query.endTime)
      if (start > end) {
        return fail(1001, 'endTime: 结束时间不能早于开始时间')
      }
      const page = Math.max(1, Number(query.page ?? 1) || 1)
      const size = Math.max(1, Number(query.size ?? 20) || 20)
      let list = [...logs]
      if (query.userId) {
        const userId = Number(query.userId)
        list = list.filter((item) => item.userId === userId)
      }
      if (query.systemCode) {
        list = list.filter((item) => item.systemCode === query.systemCode)
      }
      list = list.filter((item) => item.operateTime >= start && item.operateTime <= end)
      list.sort((a, b) => (a.operateTime < b.operateTime ? 1 : -1))
      const total = list.length
      const sliceStart = (page - 1) * size
      return ok({
        total,
        page,
        pageSize: size,
        list: list.slice(sliceStart, sliceStart + size).map(toView),
      })
    },
  },
] as MockMethod[]
