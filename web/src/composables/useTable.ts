import { ref } from 'vue'
import { DEFAULT_PAGE_SIZE } from '@/constants'
import type { PageResult } from '@/types/common'

type QueryParams = Record<string, unknown>
type PageFetcher<T> = (params: QueryParams) => Promise<PageResult<T>>

export function useTable<T>(fetcher: PageFetcher<T>, initialQuery: QueryParams = {}) {
  const rows = ref<T[]>([]) as { value: T[] }
  const total = ref(0)
  const page = ref(1)
  const size = ref(DEFAULT_PAGE_SIZE)
  const loading = ref(false)
  const error = ref<string | null>(null)
  let requestSequence = 0

  async function load(nextPage = page.value, query: QueryParams = initialQuery) {
    const sequence = ++requestSequence
    loading.value = true
    error.value = null
    page.value = nextPage
    try {
      const result = await fetcher({ ...query, page: page.value, size: size.value })
      if (sequence !== requestSequence) {
        return
      }
      rows.value = result.list
      total.value = result.total
    } catch (caught) {
      if (sequence !== requestSequence) {
        return
      }
      rows.value = []
      total.value = 0
      error.value = caught instanceof Error ? caught.message : '数据加载失败'
    } finally {
      if (sequence === requestSequence) {
        loading.value = false
      }
    }
  }

  function reset() {
    requestSequence += 1
    rows.value = []
    total.value = 0
    page.value = 1
    loading.value = false
    error.value = null
  }

  return { rows, total, page, size, loading, error, load, reset }
}
