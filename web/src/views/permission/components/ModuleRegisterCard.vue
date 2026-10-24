<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createModule } from '@/api/system.api'
import { toFieldErrors } from '@/utils/formErrors'
import type { System } from '@/types/system.types'

/**
 * 模块登记卡片（契约 8.7）：[待后端] 模块列表查询端点缺失（D8），
 * 已登记模块暂无法回显，登记成功以 toast 确认。
 */
const props = defineProps<{ systems: System[] }>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const form = reactive<{ systemId: number | undefined; moduleCode: string; moduleName: string; sortNo: number | undefined }>({
  systemId: undefined,
  moduleCode: '',
  moduleName: '',
  sortNo: undefined,
})

const rules: FormRules = {
  systemId: [{ required: true, message: '请选择所属系统', trigger: 'change' }],
  moduleCode: [
    { required: true, message: '请输入模块编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_-]{1,31}$/, message: '编码以大写字母开头，2-32 位大写字母/数字/下划线/中划线', trigger: 'blur' },
  ],
  moduleName: [
    { required: true, message: '请输入模块名称', trigger: 'blur' },
    { max: 50, message: '模块名称不超过 50 个字符', trigger: 'blur' },
  ],
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
    await createModule({
      systemId: form.systemId as number,
      moduleCode: form.moduleCode.trim(),
      moduleName: form.moduleName.trim(),
      sortNo: form.sortNo,
    })
    ElMessage.success(`模块「${form.moduleName.trim()}」登记成功`)
    form.moduleCode = ''
    form.moduleName = ''
    form.sortNo = undefined
    formRef.value?.clearValidate()
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「模块登记」这一块视觉区域 -->
  <el-card class="module-register-card" shadow="never">
    <template #header>
      <div>
        <h2 class="module-register-card__title">模块登记</h2>
        <p class="module-register-card__hint">模块列表查询端点待后端补齐（D8），已登记模块暂不回显</p>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="所属系统" prop="systemId">
        <el-select v-model="form.systemId" placeholder="请选择系统" style="width: 100%">
          <el-option
            v-for="system in props.systems"
            :key="system.id"
            :label="`${system.systemName}（${system.systemCode}）`"
            :value="system.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="模块编码" prop="moduleCode" :error="serverErrors.moduleCode">
        <el-input v-model="form.moduleCode" maxlength="32" placeholder="如 M-CONTRACT" />
      </el-form-item>
      <el-form-item label="模块名称" prop="moduleName" :error="serverErrors.moduleName">
        <el-input v-model="form.moduleName" maxlength="50" placeholder="如 合同管理" />
      </el-form-item>
      <el-form-item label="排序号（选填）" prop="sortNo">
        <el-input-number v-model="form.sortNo" :min="0" :max="9999" :precision="0" style="width: 100%" />
      </el-form-item>
      <el-button type="primary" :loading="submitting" style="width: 100%" @click="submit">
        登记模块
      </el-button>
    </el-form>
  </el-card>
</template>

<style scoped>
.module-register-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.module-register-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.module-register-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
