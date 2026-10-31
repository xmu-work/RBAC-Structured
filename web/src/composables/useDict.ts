import { storeToRefs } from 'pinia'
import { useDictStore } from '@/stores/dict.store'
import type { DictOption } from '@/types/common'

export function dictLabel(
  options: DictOption[],
  value: string | number | null | undefined,
): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return options.find((option) => option.value === value)?.label ?? String(value)
}

export function useDict() {
  const store = useDictStore()
  return {
    ...storeToRefs(store),
    dictLabel,
  }
}
