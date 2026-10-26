<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createUser } from '@/api/user.api'
import { toFieldErrors } from '@/utils/formErrors'
import OrgTreeSelect from '@/views/organization/components/OrgTreeSelect.vue'
import type { User } from '@/types/user.types'

/** 新建用户（契约 8.4）：phone、password 选填（填写时前端预校验）；成功即由父级刷新列表 */
const emit = defineEmits<{ success: [user: User] }>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

interface UserCreateForm {
  userNo: string
  realName: string
  password: string
  phone: string
  orgId: number | undefined
  post: string
}

const form = reactive<UserCreateForm>({
  userNo: '',
  realName: '',
  password: '',
  phone: '',
  orgId: undefined,
  post: '',
})

watch(visible, (open) => {
  if (!open) {
    return
  }
  form.userNo = ''
  form.realName = ''
  form.password = ''
  form.phone = ''
  form.orgId = undefined
  form.post = ''
  serverErrors.value = {}
  formRef.value?.clearValidate()
})

watch(form, () => {
  if (Object.keys(serverErrors.value).length > 0) {
    serverErrors.value = {}
  }
})

const rules: FormRules = {
  userNo: [
    { required: true, message: '请输入用户账号', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_-]{2,31}$/, message: '账号以字母开头，3-32 位字母/数字/下划线/中划线', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 30, message: '姓名不超过 30 个字符', trigger: 'blur' },
  ],
  password: [{ min: 6, max: 64, message: '密码 6-64 位；不填则使用初始密码', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
  post: [{ max: 30, message: '岗位不超过 30 个字符', trigger: 'blur' }],
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
    const user = await createUser({
      userNo: form.userNo.trim(),
      realName: form.realName.trim(),
      password: form.password || undefined,
      phone: form.phone.trim() || undefined,
      orgId: form.orgId as number,
      post: form.post.trim() || undefined,
    })
    ElMessage.success(`用户「${user.realName}」创建成功，初始状态为正常`)
    visible.value = false
    emit('success', user)
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="新建用户" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="用户账号" prop="userNo" :error="serverErrors.userNo">
        <el-input v-model="form.userNo" maxlength="32" placeholder="如 U1027" />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName" :error="serverErrors.realName">
        <el-input v-model="form.realName" maxlength="30" placeholder="加密存储" />
      </el-form-item>
      <el-form-item label="初始密码（选填）" prop="password" :error="serverErrors.password">
        <el-input v-model="form.password" type="password" show-password maxlength="64" placeholder="不填则使用系统初始密码" />
      </el-form-item>
      <el-form-item label="手机号（选填）" prop="phone" :error="serverErrors.phone">
        <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号，展示时脱敏" />
      </el-form-item>
      <el-form-item label="所属组织" prop="orgId">
        <OrgTreeSelect v-model="form.orgId" />
      </el-form-item>
      <el-form-item label="岗位（选填）" prop="post" :error="serverErrors.post">
        <el-input v-model="form.post" maxlength="30" placeholder="如 财务主管" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>
