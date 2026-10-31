import { computed } from 'vue'
import { defineStore } from 'pinia'
import {
  AccountStatus,
  AccountStatusLabel,
  ChangeType,
  ChangeTypeLabel,
  CommonStatus,
  CommonStatusLabel,
  GrantStatus,
  GrantStatusLabel,
  OperationAction,
  OperationActionLabel,
  OrgType,
  OrgTypeLabel,
  RoleType,
  RoleTypeLabel,
  type AccountStatusCode,
  type ChangeTypeCode,
  type CommonStatusCode,
  type GrantStatusCode,
  type OperationActionCode,
  type OrgTypeCode,
  type RoleTypeCode,
} from '@/constants/enums'
import type { DictOption } from '@/types'

function toOptions<T extends string>(
  values: Record<string, T>,
  labels: Record<T, string>,
): DictOption<T>[] {
  return Object.values(values).map((value) => ({ value, label: labels[value] }))
}

export const useDictStore = defineStore('dict', () => {
  // computed 包一层：storeToRefs 只收 ref/computed，普通数组会被跳过导致 useDict 解构为 undefined
  const accountStatusOptions = computed(() =>
    toOptions<AccountStatusCode>(AccountStatus, AccountStatusLabel),
  )
  const roleTypeOptions = computed(() => toOptions<RoleTypeCode>(RoleType, RoleTypeLabel))
  const orgTypeOptions = computed(() => toOptions<OrgTypeCode>(OrgType, OrgTypeLabel))
  const operationActionOptions = computed(() =>
    toOptions<OperationActionCode>(OperationAction, OperationActionLabel),
  )
  const commonStatusOptions = computed(() => toOptions<CommonStatusCode>(CommonStatus, CommonStatusLabel))
  const changeTypeOptions = computed(() => toOptions<ChangeTypeCode>(ChangeType, ChangeTypeLabel))
  const grantStatusOptions = computed(() =>
    toOptions<GrantStatusCode>(GrantStatus, GrantStatusLabel),
  )

  return {
    accountStatusOptions,
    roleTypeOptions,
    orgTypeOptions,
    operationActionOptions,
    commonStatusOptions,
    changeTypeOptions,
    grantStatusOptions,
  }
})
