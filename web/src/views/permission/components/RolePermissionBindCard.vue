<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bindRolePermissions, getRolePermissions, unbindRolePermissions } from '@/api/system.api'
import { ADMIN_ACTION_PERMISSIONS as FP } from '@/constants/permissions'
import type { RolePermission } from '@/types/system.types'

const roleId = ref<number>()
const permissionIdsText = ref('')
const allowedAction = ref('VIEW')
const bindings = ref<RolePermission[]>([])
const loading = ref(false)

function parseIds(text: string): number[] | null {
  if (!text.trim()) return null
  const values = text.split(',').map((item) => Number(item.trim()))
  return values.every((id) => Number.isSafeInteger(id) && id > 0) ? values : null
}

async function loadBindings(): Promise<void> {
  if (roleId.value === undefined) return
  loading.value = true
  try {
    bindings.value = await getRolePermissions(roleId.value)
  } finally {
    loading.value = false
  }
}

async function bind(): Promise<void> {
  const ids = parseIds(permissionIdsText.value)
  if (roleId.value === undefined || ids === null) {
    ElMessage.warning('请输入角色 ID 和逗号分隔的权限 ID')
    return
  }
  await bindRolePermissions({ roleId: roleId.value, permissionIds: ids, allowedAction: allowedAction.value as never })
  ElMessage.success(`已授予 ${ids.length} 项前端权限`)
  await loadBindings()
}

async function unbind(item: RolePermission): Promise<void> {
  if (roleId.value === undefined) return
  await unbindRolePermissions(roleId.value, [item.permissionId])
  ElMessage.success(`已解除权限 ${item.permissionId}`)
  await loadBindings()
}

function asRolePermission(row: unknown): RolePermission {
  return row as RolePermission
}
</script>

<template>
  <el-card class="role-permission-card" shadow="never">
    <template #header>
      <div>
        <h2 class="role-permission-card__title">角色权限</h2>
        <p class="role-permission-card__hint">角色只绑定前端菜单/按钮权限，对应 API 由权限映射推导</p>
      </div>
    </template>
    <el-space direction="vertical" fill style="width: 100%">
      <el-input-number v-model="roleId" :min="1" :precision="0" placeholder="角色 ID" style="width: 100%" />
      <el-input v-model="permissionIdsText" placeholder="权限 ID，逗号分隔，如 10003,20001" />
      <el-select v-model="allowedAction" style="width: 100%">
        <el-option label="查看（VIEW）" value="VIEW" />
        <el-option label="全部（ALL）" value="ALL" />
      </el-select>
      <el-button v-perm="FP.ROLE_PERMISSION_BIND" type="primary" @click="bind">授予权限</el-button>
      <el-button :loading="loading" @click="loadBindings">查看已授权限</el-button>
      <el-table v-loading="loading" :data="bindings" size="small">
        <el-table-column prop="permissionId" label="权限 ID" />
        <el-table-column prop="allowedAction" label="操作" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="danger" @click="unbind(asRolePermission(row))">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-space>
  </el-card>
</template>

<style scoped>
.role-permission-card { border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.role-permission-card__title { margin: var(--space-0); color: var(--color-text-primary); font-size: var(--font-size-lg); }
.role-permission-card__hint { margin: var(--space-2) var(--space-0) var(--space-0); color: var(--color-text-secondary); font-size: var(--font-size-sm); }
</style>
