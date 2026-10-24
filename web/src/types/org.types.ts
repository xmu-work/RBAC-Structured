import type { CommonStatusCode, OrgTypeCode } from '@/constants/enums'

export interface Org {
  id: number
  parentId?: number
  ancestors: string
  orgCode: string
  orgName: string
  orgType: OrgTypeCode
  orgLevel: number
  leaderUserId?: number
  sortNo: number
  status: CommonStatusCode
  isDeleted: number
  createdAt: string
  updatedAt: string
  children?: Org[]
}

export interface OrgCreatePayload {
  orgCode: string
  orgName: string
  orgType: OrgTypeCode
  orgLevel: number
  parentId?: number
  sortNo?: number
}

export interface OrgMovePayload {
  newParentId: number
}
