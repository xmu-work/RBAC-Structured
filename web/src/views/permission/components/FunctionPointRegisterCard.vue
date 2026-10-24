<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createFunctionPoint } from '@/api/system.api'
import { useDict } from '@/composables/useDict'
import { toFieldErrors } from '@/utils/formErrors'

/**
 * 功能点登记卡片（契约 8.7）：action 用 12 取值字典下拉；
 * [待后端] 功能点/模块列表查询端点缺失，moduleId 暂为手工录入。
 */
const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const { operationActionOptions } = useDict()

const form = reactive<{
  moduleId: number | undefined
  pointCode: string
  pointName: string
  action: string
  apiPath: string
  apiMethod: string
}>({
  moduleId: undefined,
  pointCode: '',
  pointName: '',
  action: '',
  apiPath: '',
  apiMethod: '',
})

const rules: FormRules = {
  moduleId: [{ required: true, message: '请输入所属模块 ID', trigger: 'blur' }],
  pointCode: [
    { required: true, message: '请输入功能点编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_.-]{1,63}$/, message: '编码以大写字母开头，2-64 位大写字母/数字/下划线/点/中划线', trigger: 'blur' },
  ],
  pointName: [
    { required: true, message: '请输入功能点名称', trigger: 'blur' },
    { max: 50, message: '名称不超过 50 个字符', trigger: 'blur' },
  ],
  action: [{ required: true, message: '请选择操作类型', trigger: 'change' }],
  apiPath: [{ max: 200, message: 'API 路径不超过 200 个字符', trigger: 'blur' }],
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
    await createFunctionPoint({
      moduleId: form.moduleId as number,
      pointCode: form.pointCode.trim(),
      pointName: form.pointName.trim(),
      action: form.action as never,
      apiPath: form.apiPath.trim() || undefined,
      apiMethod: form.apiMethod || undefined,
    })
    ElMessage.success(`功能点「${form.pointName.trim()}」登记成功（ID 待列表端点补齐后可查）`)
    form.pointCode = ''
    form.pointName = ''
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
  <!-- 视觉组件（契约 9.1-3）：承载「功能点登记」这一块视觉区域 -->
  <el-card class="point-register-card" shadow="never">
    <template #header>
      <div>
        <h2 class="point-register-card__title">功能点登记</h2>
        <p class="point-register-card__hint">
          功能点列表端点待后端补齐，模块 ID 暂为手工录入（登记成功的功能点 ID 见接口返回）
        </p>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="所属模块 ID" prop="moduleId" :error="serverErrors.moduleId">
        <el-input-number
          v-model="form.moduleId"
          :min="1"
          :precision="0"
          placeholder="如 5"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="功能点编码" prop="pointCode" :error="serverErrors.pointCode">
        <el-input v-model="form.pointCode" maxlength="64" placeholder="如 BTN-CONTRACT-EXPORT" />
      </el-form-item>
      <el-form-item label="功能点名称" prop="pointName" :error="serverErrors.pointName">
        <el-input v-model="form.pointName" maxlength="50" placeholder="如 合同导出按钮" />
      </el-form-item>
      <el-form-item label="操作类型" prop="action">
        <el-select v-model="form.action" filterable placeholder="12 取值之一" style="width: 100%">
          <el-option
            v-for="option in operationActionOptions"
            :key="option.value"
            :label="`${option.label}（${option.value}）`"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="API 路径（选填）" prop="apiPath">
        <el-input v-model="form.apiPath" maxlength="200" placeholder="如 /contracts/export" />
      </el-form-item>
      <el-form-item label="API 方法（选填）">
        <el-select v-model="form.apiMethod" clearable placeholder="选填" style="width: 100%">
          <el-option v-for="method in ['GET', 'POST', 'PUT', 'DELETE']" :key="method" :label="method" :value="method" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :loading="submitting" style="width: 100%" @click="submit">
        登记功能点
      </el-button>
    </el-form>
  </el-card>
</template>

<style scoped>
.point-register-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.point-register-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.point-register-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
