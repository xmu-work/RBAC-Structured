<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import { listRoles } from '@/api/role.api'
import { bindRoleFunctions, getRoleFunctions, unbindRoleFunctions } from '@/api/system.api'
import { useAuthStore } from '@/stores/auth.store'
import { useDict } from '@/composables/useDict'
import { toFieldErrors } from '@/utils/formErrors'
import type { Role } from '@/types/role.types'
import type { RoleFunction } from '@/types/system.types'
import { ADMIN_API_FUNCTION_POINTS } from '@/constants/permissions'

/**
 * 角色-功能绑定卡片（契约 8.7 权限矩阵雏形）：混合传参（roleId/allowedAction 走 query，
 * functionPointIds 走 JSON Body）；选中角色后展示已绑定功能点，可逐个解绑。矩阵视图仍待后端补齐。
 */
const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const authStore = useAuthStore()
const { operationActionOptions } = useDict()

const roles = ref<Role[]>([])
const bound = ref<RoleFunction[]>([])
const boundLoading = ref(false)

const form = reactive<{ roleId: number | undefined; allowedAction: string; pointIdsText: string }>({
  roleId: undefined,
  allowedAction: '',
  pointIdsText: '',
})

onMounted(() => {
  void listRoles().then((list) => {
    roles.value = list
  })
})

async function loadBound(): Promise<void> {
  if (form.roleId === undefined) {
    bound.value = []
    return
  }
  boundLoading.value = true
  try {
    bound.value = await getRoleFunctions(form.roleId)
  } catch {
    bound.value = []
  } finally {
    boundLoading.value = false
  }
}

watch(() => form.roleId, () => void loadBound())

/** 解绑属回收授权，按 L2 高危确认（契约 9.2-2） */
async function unbind(item: RoleFunction): Promise<void> {
  if (form.roleId === undefined) {
    return
  }
  const confirmed = await ElMessageBox.confirm(
    `解绑后，拥有该角色的用户将立即失去功能点 ${item.functionPointId} 的权限。`,
    '解绑功能点',
    { type: 'warning', confirmButtonText: '解绑', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) {
    return
  }
  await unbindRoleFunctions(form.roleId, [item.functionPointId])
  ElMessage.success(`已解绑功能点 ${item.functionPointId}`)
  await loadBound()
}

/** 逗号/空格/换行分隔的功能点 ID 文本 → 数字数组（非法项报表单错误） */
function parsePointIds(text: string): number[] | null {
  const parts = text.split(/[\s,，;；]+/).filter(Boolean)
  if (parts.length === 0) {
    return null
  }
  const ids: number[] = []
  for (const part of parts) {
    const id = Number(part)
    if (!Number.isInteger(id) || id <= 0) {
      return null
    }
    ids.push(id)
  }
  return ids
}

const rules: FormRules = {
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  allowedAction: [{ required: true, message: '请选择操作类型', trigger: 'change' }],
  pointIdsText: [
    {
      required: true,
      validator: (_rule, value: string, callback) => {
        if (!value || parsePointIds(value) === null) {
          callback(new Error('请填写功能点 ID，多个以逗号或换行分隔'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submit(): Promise<void> {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false)
  if (!valid || form.roleId === undefined) {
    return
  }
  const functionPointIds = parsePointIds(form.pointIdsText)
  if (functionPointIds === null) {
    return
  }
  submitting.value = true
  serverErrors.value = {}
  try {
    await bindRoleFunctions({
      roleId: form.roleId,
      allowedAction: form.allowedAction as never,
      functionPointIds,
      createdBy: authStore.userId ?? undefined,
    })
    ElMessage.success(`已为角色绑定 ${functionPointIds.length} 个功能点（操作：${form.allowedAction}）`)
    form.pointIdsText = ''
    formRef.value?.clearValidate()
    await loadBound()
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「角色-功能绑定」这一块视觉区域 -->
  <el-card class="role-bind-card" shadow="never">
    <template #header>
      <div>
        <h2 class="role-bind-card__title">角色-功能绑定</h2>
        <p class="role-bind-card__hint">
          选中角色后显示其已绑定的功能点，可逐个解绑；矩阵视图待后端补齐
        </p>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="角色" prop="roleId">
        <el-select v-model="form.roleId" filterable placeholder="请选择角色" style="width: 100%">
          <el-option
            v-for="role in roles"
            :key="role.id"
            :label="`${role.roleName}（${role.roleCode}）`"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.roleId !== undefined" label="已绑定功能点">
        <div v-loading="boundLoading" class="role-bind-card__bound">
          <el-tag
            v-for="item in bound"
            :key="item.id"
            v-perm:disable="ADMIN_API_FUNCTION_POINTS.ROLE_FP_UNBIND"
            closable
            effect="plain"
            @close="unbind(item)"
          >
            {{ item.functionPointId }}（{{ item.allowedAction }}）
          </el-tag>
          <span v-if="!boundLoading && bound.length === 0" class="role-bind-card__empty">尚未绑定功能点</span>
        </div>
      </el-form-item>
      <el-form-item label="操作类型" prop="allowedAction">
        <el-select v-model="form.allowedAction" filterable placeholder="12 取值之一" style="width: 100%">
          <el-option
            v-for="option in operationActionOptions"
            :key="option.value"
            :label="`${option.label}（${option.value}）`"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="功能点 ID 集合" prop="pointIdsText">
        <el-input
          v-model="form.pointIdsText"
          type="textarea"
          :rows="4"
          placeholder="多个 ID 以逗号或换行分隔，如：&#10;5001, 5002, 5003"
        />
      </el-form-item>
      <el-button type="primary" :icon="Link" :loading="submitting" style="width: 100%" @click="submit">
        绑定功能点
      </el-button>
    </el-form>
  </el-card>
</template>

<style scoped>
.role-bind-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.role-bind-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.role-bind-card__bound {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  min-height: var(--space-6);
}

.role-bind-card__empty {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.role-bind-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
