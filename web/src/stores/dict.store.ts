import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { systemApi } from '@/api'
import {
  AccountStatus,
  AccountStatusLabel,
  ChangeType,
  ChangeTypeLabel,
  CommonStatus,
  CommonStatusLabel,
  DataScopeType,
  DataScopeTypeLabel,
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
  type DataScopeTypeCode,
  type GrantStatusCode,
  type OperationActionCode,
  type OrgTypeCode,
  type RoleTypeCode,
} from '@/constants/enums'
import type { DataScope, DictOption } from '@/types'

function toOptions<T extends string>(
  values: Record<string, T>,
  labels: Record<T, string>,
): DictOption<T>[] {
  return Object.values(values).map((value) => ({ value, label: labels[value] }))
}

export const useDictStore = defineStore('dict', () => {
  const accountStatusOptions = toOptions<AccountStatusCode>(AccountStatus, AccountStatusLabel)
  const roleTypeOptions = toOptions<RoleTypeCode>(RoleType, RoleTypeLabel)
  const orgTypeOptions = toOptions<OrgTypeCode>(OrgType, OrgTypeLabel)
  const operationActionOptions = toOptions<OperationActionCode>(
    OperationAction,
    OperationActionLabel,
  )
  const dataScopeTypeOptions = toOptions<DataScopeTypeCode>(DataScopeType, DataScopeTypeLabel)
  const commonStatusOptions = toOptions<CommonStatusCode>(CommonStatus, CommonStatusLabel)
  const changeTypeOptions = toOptions<ChangeTypeCode>(ChangeType, ChangeTypeLabel)
  const grantStatusOptions = toOptions<GrantStatusCode>(GrantStatus, GrantStatusLabel)

  const dataScopes = ref<DataScope[]>([])
  const dataScopesLoading = ref(false)
  const dataScopesLoaded = ref(false)
  const dataScopesError = ref<string | null>(null)

  const dataScopeOptions = computed<DictOption[]>(() =>
    dataScopes.value.map((scope) => ({
      value: scope.scopeCode,
      label: scope.scopeName,
    })),
  )

  async function loadDataScopes(): Promise<void> {
    if (dataScopesLoading.value || dataScopesLoaded.value) {
      return
    }
    dataScopesLoading.value = true
    dataScopesError.value = null
    try {
      dataScopes.value = await systemApi.listDataScopes()
      dataScopesLoaded.value = true
    } catch (error) {
      dataScopesError.value = error instanceof Error ? error.message : '数据范围字典加载失败'
    } finally {
      dataScopesLoading.value = false
    }
  }

  return {
    accountStatusOptions,
    roleTypeOptions,
    orgTypeOptions,
    operationActionOptions,
    dataScopeTypeOptions,
    commonStatusOptions,
    changeTypeOptions,
    grantStatusOptions,
    dataScopes,
    dataScopesLoading,
    dataScopesLoaded,
    dataScopesError,
    dataScopeOptions,
    loadDataScopes,
  }
})
