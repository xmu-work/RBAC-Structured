import type { MockMethod } from 'vite-plugin-mock'

const timestamp = '2026-10-24 15:30:00'

export default [
  {
    url: '/api/v1/systems/data-scopes',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          scopeCode: 'ALL',
          scopeName: '全公司',
          description: '可访问全公司数据',
          sortNo: 1,
          status: 'ACTIVE',
          createdAt: timestamp,
        },
        {
          id: 2,
          scopeCode: 'BRANCH',
          scopeName: '本分公司',
          description: '仅可访问所属分公司数据',
          sortNo: 2,
          status: 'ACTIVE',
          createdAt: timestamp,
        },
        {
          id: 3,
          scopeCode: 'DEPT',
          scopeName: '本部门',
          description: '仅可访问所属部门数据',
          sortNo: 3,
          status: 'ACTIVE',
          createdAt: timestamp,
        },
        {
          id: 4,
          scopeCode: 'PROJECT',
          scopeName: '本项目',
          description: '仅可访问所属项目数据',
          sortNo: 4,
          status: 'ACTIVE',
          createdAt: timestamp,
        },
        {
          id: 5,
          scopeCode: 'SELF',
          scopeName: '本人',
          description: '仅可访问本人数据',
          sortNo: 5,
          status: 'ACTIVE',
          createdAt: timestamp,
        },
      ],
      timestamp,
    }),
  },
] as MockMethod[]
