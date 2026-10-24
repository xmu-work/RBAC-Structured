import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission.store'

type PermissionValue = number | number[]

function hasPermission(value: PermissionValue): boolean {
  const permissionStore = usePermissionStore()
  const required = Array.isArray(value) ? value : [value]
  return required.every((id) => permissionStore.canAccess(id))
}

function setDisabledState(element: HTMLElement, binding: DirectiveBinding<PermissionValue>): void {
  if (hasPermission(binding.value)) {
    return
  }

  if (binding.modifiers.disable) {
    element.setAttribute('disabled', '')
    element.setAttribute('aria-disabled', 'true')
    element.classList.add('is-permission-disabled')
    element.title = '当前账号无此操作权限'
    return
  }

  element.remove()
}

/** 全项目唯一的元素级权限入口（契约 6.2 L2）。 */
export const vPerm: Directive<HTMLElement, PermissionValue> = {
  mounted: setDisabledState,
  updated: setDisabledState,
}

export default vPerm
