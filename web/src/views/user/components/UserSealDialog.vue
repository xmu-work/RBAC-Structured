<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import type { User } from '@/types/user.types'

/** 封存用户 L2 高危确认（契约 8.4 / 9.2-2）：原因必填 + 红色后果文案，对应 UC-02 */
defineProps<{
  user: User | null
  loading: boolean
}>()

const emit = defineEmits<{ confirm: [reason: string] }>()

const visible = defineModel<boolean>({ default: false })

const formRef = ref<FormInstance>()
const reason = ref('')

watch(visible, (open) => {
  if (open) {
    reason.value = ''
    formRef.value?.clearValidate()
  }
})

const rules: FormRules = {
  reason: [
    { required: true, message: '请填写封存原因（离职留痕）', trigger: 'blur' },
    { max: 200, message: '原因不超过 200 个字符', trigger: 'blur' },
  ],
}

async function handleConfirm(): Promise<void> {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false)
  if (!valid) {
    return
  }
  emit('confirm', reason.value.trim())
}
</script>

<template>
  <el-dialog v-model="visible" title="封存用户确认" width="520px">
    <el-descriptions v-if="user" :column="1" border size="small">
      <el-descriptions-item label="用户账号">{{ user.userNo }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ user.realName }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="user-seal-dialog__alert"
      type="error"
      :closable="false"
      show-icon
      :title="`高危操作：封存用于员工离职`"
    >
      <p class="user-seal-dialog__consequence">
        封存后系统将解绑该用户的全部角色与数据权限、吊销其登录令牌，且该账号将无法再登录（UC-02）。此操作用于离职场景，请与人事流程核对后继续。
      </p>
    </el-alert>

    <el-form ref="formRef" :model="{ reason }" :rules="rules" label-position="top">
      <el-form-item label="封存原因" prop="reason">
        <el-input
          v-model="reason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="如：2026 年 10 月离职，工号 XXX"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" :icon="WarningFilled" @click="handleConfirm">
        确认封存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-seal-dialog__alert {
  margin-top: var(--space-4);
  margin-bottom: var(--space-4);
}

.user-seal-dialog__consequence {
  margin: var(--space-0);
  font-size: var(--font-size-base);
  line-height: 1.7;
}
</style>
