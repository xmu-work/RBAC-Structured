import { get, postForm, putForm } from '@/utils/request'
import type { Org, OrgCreatePayload, OrgMovePayload } from '@/types/org.types'

export function getOrgTree(rootId?: number): Promise<Org[]> {
  return get<Org[]>('/orgs', rootId === undefined ? undefined : { rootId })
}

export function getOrg(orgId: number): Promise<Org> {
  return get<Org>(`/orgs/${orgId}`)
}

export function getOrgChildren(orgId: number): Promise<Org[]> {
  return get<Org[]>(`/orgs/${orgId}/children`)
}

export function createOrg(payload: OrgCreatePayload): Promise<Org> {
  return postForm<Org>('/orgs', payload)
}

export function moveOrg(orgId: number, payload: OrgMovePayload): Promise<Org> {
  return putForm<Org>(`/orgs/${orgId}/move`, payload)
}
