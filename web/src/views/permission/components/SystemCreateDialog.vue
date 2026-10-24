<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createSystem } from '@/api/system.api'
import { toFieldErrors } from '@/utils/formErrors'
import type { System } from '@/types/system.types'

/** 登记系统对话框（契约 8.7）：systemCode 全大写规则校验 */
const emit = defineEmits<{ success: [system: System] }>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

interface SystemCreateForm {
  systemCode: string
  systemName: string
  description: string
}

const form = reactive<SystemCreateForm>({
  systemCode: '',
  systemName: '',
  description: '',
})

watch(visible, (open) => {
  if (!open) {
    return
  }
  form.systemCode = ''
  form.systemName = ''
  form.description = ''
  serverErrors.value = {}
  formRef.value?.clearValidate()
})

watch(form, () => {
  if (Object.keys(serverErrors.value).length > 0) {
    serverErrors.value = {}
  }
})

const rules: FormRules = {
  systemCode: [
    { required: true, message: '请输入系统编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{1,31}$/,
      message: '编码以大写字母开头，2-32 位大写字母/数字/下划线',
      trigger: 'blur',
    },
  ],
  systemName: [
    { required: true, message: '请输入系统名称', trigger: 'blur' },
    { max: 50, message: '系统名称不超过 50 个字符', trigger: 'blur' },
  ],
  description: [{ max: 200, message: '描述不超过 200 个字符', trigger: 'blur' }],
}

async function submit(): Promise<void> {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  serverErrors.value = {}
  try {
    const system = await createSystem({
      systemCode: form.systemCode.trim(),
      systemName: form.systemName.trim(),
      description: form.description.trim() || undefined,
    })
    ElMessage.success(`系统「${system.systemName}」登记成功`)
    visible.value = false
    emit('success', system)
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="登记系统" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="系统编码" prop="systemCode" :error="serverErrors.systemCode">
        <el-input v-model="form.systemCode" maxlength="32" placeholder="如 ERP / HR-PORTAL" />
      </el-form-item>
      <el-form-item label="系统名称" prop="systemName" :error="serverErrors.systemName">
        <el-input v-model="form.systemName" maxlength="50" placeholder="如 企业资源计划系统" />
      </el-form-item>
      <el-form-item label="描述（选填）" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="系统的业务范围说明"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">登记</el-button>
    </template>
  </el-dialog>
</template>
