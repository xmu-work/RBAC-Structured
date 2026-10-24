<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'

interface OrgBrief {
  id: number
  orgName: string
}

defineProps<{
  source: OrgBrief | null
  targetParent: OrgBrief | null
  loading: boolean
}>()

const emit = defineEmits<{ confirm: [] }>()

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
    { required: true, message: '请填写移动原因（高危操作留痕）', trigger: 'blur' },
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
  // 原因仅做 L2 高危留痕展示：move 端点暂无 reason 参数（契约 8.3 [缺]），不随请求提交
  emit('confirm')
}
</script>

<template>
  <el-dialog v-model="visible" title="移动组织确认" width="520px">
    <el-descriptions v-if="source && targetParent" :column="1" border size="small">
      <el-descriptions-item label="移动节点">{{ source.orgName }}</el-descriptions-item>
      <el-descriptions-item label="新父节点">{{ targetParent.orgName }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="org-move-dialog__alert"
      type="warning"
      :closable="false"
      show-icon
      title="高危操作"
      description="移动后该组织及其全部下级组织将整体挂载到新父节点之下，组织层级与相关用户归属随之变化，请确认业务影响后再继续。"
    />

    <el-form ref="formRef" :model="{ reason }" :rules="rules" label-position="top">
      <el-form-item label="移动原因" prop="reason">
        <el-input
          v-model="reason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请填写移动原因，用于操作留痕"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" :icon="Promotion" @click="handleConfirm">
        确认移动
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.org-move-dialog__alert {
  margin-top: var(--space-4);
  margin-bottom: var(--space-4);
}
</style>
