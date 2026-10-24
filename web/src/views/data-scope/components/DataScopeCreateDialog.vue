<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createDataScope } from '@/api/system.api'
import { useDictStore } from '@/stores/dict.store'
import { toFieldErrors } from '@/utils/formErrors'

/** 新增数据范围对话框（契约 8.8）：内置 5 项编码不可重复登记；成功后刷新全局字典 */
const emit = defineEmits<{ success: [] }>()

const visible = defineModel<boolean>({ default: false })

const dictStore = useDictStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const form = reactive<{ scopeCode: string; scopeName: string; description: string; sortNo: number | undefined }>({
  scopeCode: '',
  scopeName: '',
  description: '',
  sortNo: undefined,
})

watch(visible, (open) => {
  if (!open) {
    return
  }
  form.scopeCode = ''
  form.scopeName = ''
  form.description = ''
  form.sortNo = undefined
  serverErrors.value = {}
  formRef.value?.clearValidate()
})

watch(form, () => {
  if (Object.keys(serverErrors.value).length > 0) {
    serverErrors.value = {}
  }
})

const rules: FormRules = {
  scopeCode: [
    { required: true, message: '请输入范围编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{1,31}$/, message: '编码以大写字母开头，2-32 位大写字母/数字/下划线', trigger: 'blur' },
  ],
  scopeName: [
    { required: true, message: '请输入范围名称', trigger: 'blur' },
    { max: 50, message: '名称不超过 50 个字符', trigger: 'blur' },
  ],
  description: [{ max: 200, message: '说明不超过 200 个字符', trigger: 'blur' }],
  sortNo: [{ type: 'integer', min: 0, max: 9999, message: '排序号为 0-9999 的整数', trigger: 'blur' }],
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
    await createDataScope({
      scopeCode: form.scopeCode.trim(),
      scopeName: form.scopeName.trim(),
      description: form.description.trim() || undefined,
      sortNo: form.sortNo,
    })
    ElMessage.success(`数据范围「${form.scopeName.trim()}」登记成功，全局字典已刷新`)
    visible.value = false
    await dictStore.refreshDataScopes()
    emit('success')
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="新增数据范围" width="520px">
    <el-alert
      class="scope-create-dialog__alert"
      type="info"
      :closable="false"
      show-icon
      title="内置范围编码（ALL/BRANCH/DEPT/PROJECT/SELF）不可重复登记"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="范围编码" prop="scopeCode" :error="serverErrors.scopeCode">
        <el-input v-model="form.scopeCode" maxlength="32" placeholder="如 BRANCH_GROUP" />
      </el-form-item>
      <el-form-item label="范围名称" prop="scopeName" :error="serverErrors.scopeName">
        <el-input v-model="form.scopeName" maxlength="50" placeholder="如 协作分公司组" />
      </el-form-item>
      <el-form-item label="说明（选填）" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="该范围的数据边界说明"
        />
      </el-form-item>
      <el-form-item label="排序号（选填）" prop="sortNo">
        <el-input-number v-model="form.sortNo" :min="0" :max="9999" :precision="0" style="width: 100%" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">登记</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.scope-create-dialog__alert {
  margin-bottom: var(--space-4);
}
</style>
