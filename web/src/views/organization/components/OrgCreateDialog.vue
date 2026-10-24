<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createOrg } from '@/api/org.api'
import { useDict } from '@/composables/useDict'
import { OrgType, OrgTypeLevel, type OrgTypeCode } from '@/constants/enums'
import { toFieldErrors } from '@/utils/formErrors'
import type { Org } from '@/types/org.types'

const props = defineProps<{ parentOrg: Org | null }>()
const emit = defineEmits<{ success: [org: Org] }>()

const visible = defineModel<boolean>({ default: false })

const { orgTypeOptions } = useDict()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

interface OrgCreateForm {
  orgName: string
  orgCode: string
  orgType: OrgTypeCode
  sortNo: number | undefined
}

const form = reactive<OrgCreateForm>({
  orgName: '',
  orgCode: '',
  orgType: OrgType.BRANCH,
  sortNo: undefined,
})

/** orgType → orgLevel 字典内置映射（HQ=1…PROJECT=4），层级禁止手填（契约 8.3） */
const orgLevel = computed(() => OrgTypeLevel[form.orgType])

/** 默认建议下一层级类型（总公司下建议分公司，以此类推；项目部下仍为项目部） */
const NEXT_TYPE: Record<OrgTypeCode, OrgTypeCode> = {
  [OrgType.HQ]: OrgType.BRANCH,
  [OrgType.BRANCH]: OrgType.DEPT,
  [OrgType.DEPT]: OrgType.PROJECT,
  [OrgType.PROJECT]: OrgType.PROJECT,
}

watch(visible, (open) => {
  if (!open) {
    return
  }
  form.orgName = ''
  form.orgCode = ''
  form.orgType = props.parentOrg ? NEXT_TYPE[props.parentOrg.orgType] : OrgType.HQ
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
  orgName: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { max: 50, message: '组织名称不超过 50 个字符', trigger: 'blur' },
  ],
  orgCode: [
    { required: true, message: '请输入组织编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_-]{1,31}$/,
      message: '编码以大写字母开头，2-32 位大写字母/数字/下划线/中划线',
      trigger: 'blur',
    },
  ],
  orgType: [{ required: true, message: '请选择组织类型', trigger: 'change' }],
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
    const org = await createOrg({
      orgName: form.orgName.trim(),
      orgCode: form.orgCode.trim(),
      orgType: form.orgType,
      orgLevel: orgLevel.value,
      parentId: props.parentOrg?.id,
      sortNo: form.sortNo,
    })
    ElMessage.success(`组织「${org.orgName}」创建成功`)
    visible.value = false
    emit('success', org)
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="新增组织节点" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="父组织">
        <el-input :model-value="parentOrg?.orgName ?? '（无）'" disabled />
      </el-form-item>
      <el-form-item label="组织名称" prop="orgName" :error="serverErrors.orgName">
        <el-input v-model="form.orgName" maxlength="50" placeholder="请输入组织名称" />
      </el-form-item>
      <el-form-item label="组织编码" prop="orgCode" :error="serverErrors.orgCode">
        <el-input v-model="form.orgCode" maxlength="32" placeholder="如 DP-EAST-HR" />
      </el-form-item>
      <el-form-item label="组织类型" prop="orgType">
        <el-select v-model="form.orgType" placeholder="请选择组织类型" style="width: 100%">
          <el-option
            v-for="option in orgTypeOptions"
            :key="option.value"
            :label="`${option.label}（第 ${OrgTypeLevel[option.value]} 级）`"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="组织层级">
        <el-input :model-value="`第 ${orgLevel} 级（由组织类型决定，不可修改）`" disabled />
      </el-form-item>
      <el-form-item label="排序号" prop="sortNo">
        <el-input-number
          v-model="form.sortNo"
          :min="0"
          :max="9999"
          :precision="0"
          placeholder="选填，默认 0"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>
