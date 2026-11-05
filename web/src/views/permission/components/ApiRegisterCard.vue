<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { bindApiToPermission, createApi } from '@/api/system.api'
import { ADMIN_ACTION_PERMISSIONS as FP } from '@/constants/permissions'
import { toFieldErrors } from '@/utils/formErrors'
import type { System } from '@/types/system.types'

const props = defineProps<{ systems: System[] }>()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})
const form = reactive({
  systemId: undefined as number | undefined,
  permissionId: undefined as number | undefined,
  apiCode: '',
  apiName: '',
  apiPath: '',
  httpMethod: 'GET',
})

const rules: FormRules = {
  systemId: [{ required: true, message: '请选择所属系统', trigger: 'change' }],
  apiCode: [{ required: true, message: '请输入 API 编码', trigger: 'blur' }],
  apiName: [{ required: true, message: '请输入 API 名称', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入 Controller 映射模板', trigger: 'blur' }],
  httpMethod: [{ required: true, message: '请选择 HTTP 方法', trigger: 'change' }],
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  submitting.value = true
  serverErrors.value = {}
  try {
    const api = await createApi({
      systemId: form.systemId as number,
      apiCode: form.apiCode.trim(),
      apiName: form.apiName.trim(),
      apiPath: form.apiPath.trim(),
      httpMethod: form.httpMethod,
    })
    if (form.permissionId !== undefined) {
      await bindApiToPermission(form.permissionId, api.id)
    }
    ElMessage.success(`API「${form.apiName.trim()}」登记成功`)
    form.apiCode = ''
    form.apiName = ''
    form.apiPath = ''
    formRef.value?.clearValidate()
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card class="api-register-card" shadow="never">
    <template #header>
      <div>
        <h2 class="api-register-card__title">后端 API</h2>
        <p class="api-register-card__hint">API 独立登记，再归属到菜单/按钮权限；未映射 API 默认拒绝</p>
      </div>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="所属系统" prop="systemId">
        <el-select v-model="form.systemId" placeholder="请选择系统" style="width: 100%">
          <el-option v-for="item in props.systems" :key="item.id" :label="item.systemName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属权限 ID（选填，登记后绑定）">
        <el-input-number v-model="form.permissionId" :min="1" :precision="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="API 编码" prop="apiCode" :error="serverErrors.apiCode">
        <el-input v-model="form.apiCode" maxlength="64" placeholder="如 CONTRACT_EXPORT" />
      </el-form-item>
      <el-form-item label="API 名称" prop="apiName" :error="serverErrors.apiName">
        <el-input v-model="form.apiName" maxlength="128" placeholder="如 导出合同" />
      </el-form-item>
      <el-form-item label="Controller 映射模板" prop="apiPath" :error="serverErrors.apiPath">
        <el-input v-model="form.apiPath" maxlength="255" placeholder="如 /contracts/{contractId}" />
      </el-form-item>
      <el-form-item label="HTTP 方法" prop="httpMethod">
        <el-select v-model="form.httpMethod" style="width: 100%">
          <el-option v-for="method in ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']" :key="method" :label="method" :value="method" />
        </el-select>
      </el-form-item>
      <el-button v-perm="FP.API_CREATE" type="primary" :loading="submitting" style="width: 100%" @click="submit">
        登记 API
      </el-button>
    </el-form>
  </el-card>
</template>

<style scoped>
.api-register-card { border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.api-register-card__title { margin: var(--space-0); color: var(--color-text-primary); font-size: var(--font-size-lg); }
.api-register-card__hint { margin: var(--space-2) var(--space-0) var(--space-0); color: var(--color-text-secondary); font-size: var(--font-size-sm); }
</style>
