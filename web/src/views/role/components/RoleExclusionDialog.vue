<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { addRoleExclusion } from '@/api/role.api'
import { toFieldErrors } from '@/utils/formErrors'
import type { Role } from '@/types/role.types'

/** 添加互斥 L2 确认（契约 8.5 / 9.2-2）：后果文案 + 原因必填，确认即调用配置接口 */
const props = defineProps<{
  role: Role | null
  roles: Role[]
  excludedRoleIds: number[]
}>()

const emit = defineEmits<{ success: [] }>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const form = reactive<{ excludedRoleId: number | undefined; reason: string }>({
  excludedRoleId: undefined,
  reason: '',
})

/** 可选互斥角色：排除自身与已配置（含反向）项 */
const candidateRoles = computed(() =>
  props.roles.filter(
    (role) =>
      role.id !== props.role?.id && !props.excludedRoleIds.includes(role.id),
  ),
)

watch(visible, (open) => {
  if (!open) {
    return
  }
  form.excludedRoleId = undefined
  form.reason = ''
  serverErrors.value = {}
  formRef.value?.clearValidate()
})

const rules: FormRules = {
  excludedRoleId: [{ required: true, message: '请选择互斥角色', trigger: 'change' }],
  reason: [
    { required: true, message: '请填写配置原因（留痕）', trigger: 'blur' },
    { max: 200, message: '原因不超过 200 个字符', trigger: 'blur' },
  ],
}

async function submit(): Promise<void> {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false)
  if (!valid || !props.role || form.excludedRoleId === undefined) {
    return
  }
  submitting.value = true
  serverErrors.value = {}
  try {
    await addRoleExclusion(props.role.id, form.excludedRoleId, form.reason.trim())
    ElMessage.success('互斥关系已配置并即时生效')
    visible.value = false
    emit('success')
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="添加互斥角色" width="520px">
    <el-descriptions v-if="role" :column="1" border size="small">
      <el-descriptions-item label="当前角色">{{ role.roleName }}（{{ role.roleCode }}）</el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="role-exclusion-dialog__alert"
      type="warning"
      :closable="false"
      show-icon
      title="高危操作：互斥配置即时生效"
      description="配置后这两个角色将被禁止授予同一用户（UC-05）；已在同一用户身上生效的互斥角色不受影响，需在授权管理中人工处理。"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="互斥角色" prop="excludedRoleId" :error="serverErrors.excludedRoleId">
        <el-select v-model="form.excludedRoleId" placeholder="请选择角色" style="width: 100%">
          <el-option
            v-for="item in candidateRoles"
            :key="item.id"
            :label="`${item.roleName}（${item.roleCode}）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="配置原因" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="如：职责分离要求（SOX 合规）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="submitting" :icon="WarningFilled" @click="submit">
        确认配置
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.role-exclusion-dialog__alert {
  margin-top: var(--space-4);
  margin-bottom: var(--space-4);
}
</style>
