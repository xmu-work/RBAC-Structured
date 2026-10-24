import { get, postForm, putForm } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { User, UserCreatePayload, UserListQuery, UserStatusPayload } from '@/types/user.types'

export function listUsers(query: UserListQuery): Promise<PageResult<User>> {
  return get<PageResult<User>>('/users', query)
}

export function getUser(userId: number): Promise<User> {
  return get<User>(`/users/${userId}`)
}

export function createUser(payload: UserCreatePayload): Promise<User> {
  return postForm<User>('/users', payload)
}

export function changeUserStatus(userId: number, payload: UserStatusPayload): Promise<User> {
  return putForm<User>(`/users/${userId}/status`, payload)
}
