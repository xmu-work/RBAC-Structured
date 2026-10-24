<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'

/** 回收授权 L2 高危确认（契约 8.6 / 9.2-2）：原因必填写实；回收端点无 reason 参数，原因本地留痕 */
defineProps<{
  userName: string
  roleName: string
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
    { required: true, message: '请填写回收原因（留痕）', trigger: 'blur' },
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
  <el-dialog v-model="visible" title="回收授权确认" width="520px">
    <el-descriptions :column="1" border size="small">
      <el-descriptions-item label="用户">{{ userName }}</el-descriptions-item>
      <el-descriptions-item label="回收角色">{{ roleName }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="revoke-dialog__alert"
      type="error"
      :closable="false"
      show-icon
      title="高危操作：回收即时生效"
      description="回收后该用户立即失去此角色对应的全部功能权限与数据权限；服务端权限缓存最多 300 秒内完全收敛。"
    />

    <el-form ref="formRef" :model="{ reason }" :rules="rules" label-position="top">
      <el-form-item label="回收原因" prop="reason">
        <el-input
          v-model="reason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="如：岗位调动，不再承担该职责"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" :icon="WarningFilled" @click="handleConfirm">
        确认回收
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.revoke-dialog__alert {
  margin-top: var(--space-4);
  margin-bottom: var(--space-4);
}
</style>
