import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { openApi } from '@/api'

function hasSameIds(left: Set<number>, right: Set<number>): boolean {
  if (left.size !== right.size) {
    return false
  }
  return [...left].every((id) => right.has(id))
}

export const usePermissionStore = defineStore('permission', () => {
  const permissionIds = ref<Set<number>>(new Set())
  const loaded = ref(false)
  const loading = ref(false)
  const lastLoadedAt = ref<number | null>(null)

  const count = computed(() => permissionIds.value.size)

  function canAccess(functionPointId?: number): boolean {
    return functionPointId === undefined || permissionIds.value.has(functionPointId)
  }

  function setPermissions(ids: Iterable<number>): boolean {
    const next = new Set(ids)
    const changed = !hasSameIds(permissionIds.value, next)
    permissionIds.value = next
    loaded.value = true
    lastLoadedAt.value = Date.now()
    return changed
  }

  async function fetchPermissions(userId: number, silent = false): Promise<boolean> {
    if (loading.value) {
      return false
    }
    loading.value = true
    try {
      return setPermissions(await openApi.getUserPermissionIds(userId, silent))
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    permissionIds.value = new Set()
    loaded.value = false
    loading.value = false
    lastLoadedAt.value = null
  }

  return {
    permissionIds,
    loaded,
    loading,
    lastLoadedAt,
    count,
    canAccess,
    setPermissions,
    fetchPermissions,
    reset,
  }
})
