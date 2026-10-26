<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { grantRole, listRoles } from '@/api/role.api'
import { listUsers } from '@/api/user.api'
import { useAuthStore } from '@/stores/auth.store'
import type { BusinessError } from '@/utils/request'
import { toFieldErrors } from '@/utils/formErrors'
import type { Role } from '@/types/role.types'
import type { User } from '@/types/user.types'
import { ADMIN_API_FUNCTION_POINTS as FP } from '@/constants/permissions'

/**
 * 授予表单卡（契约 8.6）：选人 → 单角色授予（后端不支持批量）。
 * expireTime 以 ISO 本地格式传参（契约 3.3），留空 = 长期；grantReason 必填留痕；
 * 3001 互斥冲突弹窗点名冲突角色（UC-05），3005 重复授权由拦截器 toast。
 */
const emit = defineEmits<{ granted: [userId: number]; userChange: [userId: number | null] }>()

const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const users = ref<User[]>([])
const roles = ref<Role[]>([])

interface GrantForm {
  userId: number | undefined
  roleId: number | undefined
  expireTime: string
  grantReason: string
}

const form = reactive<GrantForm>({
  userId: undefined,
  roleId: undefined,
  expireTime: '',
  grantReason: '',
})

onMounted(async () => {
  try {
    const [userPage, roleList] = await Promise.all([
      listUsers({ page: 1, size: 100 }),
      listRoles(),
    ])
    users.value = userPage.list
    roles.value = roleList
  } catch {
    // 选项加载失败由拦截器 toast；表单仍可重试（切换用户触发列表卡提示）
  }
})

const rules: FormRules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  grantReason: [
    { required: true, message: '请填写授权原因（留痕）', trigger: 'blur' },
    { max: 200, message: '原因不超过 200 个字符', trigger: 'blur' },
  ],
}

/** 选人变化即联动下方用户角色列表（清空 = 回到占位态） */
watch(
  () => form.userId,
  (userId) => emit('userChange', userId ?? null),
)

async function submit(): Promise<void> {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false)
  if (!valid || form.userId === undefined || form.roleId === undefined) {
    return
  }
  submitting.value = true
  serverErrors.value = {}
  try {
    const grant = await grantRole({
      userId: form.userId,
      roleId: form.roleId,
      expireTime: form.expireTime || undefined,
      grantBy: authStore.userId ?? undefined,
      grantReason: form.grantReason.trim(),
    })
    const roleName = roles.value.find((role) => role.id === form.roleId)?.roleName ?? `#${form.roleId}`
    ElMessage.success(`已将「${roleName}」授予用户 #${grant.userId}`)
    form.roleId = undefined
    form.expireTime = ''
    form.grantReason = ''
    formRef.value?.clearValidate()
    emit('granted', form.userId)
  } catch (error) {
    const business = error as BusinessError
    if (business && typeof business === 'object' && business.code === 3001) {
      // UC-05：互斥冲突弹窗点名冲突角色（message 含冲突详情）
      await ElMessageBox.alert(business.message, '互斥角色冲突', {
        type: 'error',
        confirmButtonText: '知道了',
      }).catch(() => undefined)
    } else {
      serverErrors.value = toFieldErrors(error)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「授予角色」这一块视觉区域 -->
  <el-card class="grant-form-card" shadow="never">
    <template #header>
      <div class="grant-form-card__header">
        <div>
          <h2 class="grant-form-card__title">授予角色</h2>
          <p class="grant-form-card__hint">
            单角色授予；到期时间留空表示长期有效；互斥角色将被拒绝（UC-05）
          </p>
        </div>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" inline>
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="form.userId"
          filterable
          placeholder="搜索账号 / 姓名"
          class="grant-form-card__select-user"
        >
          <el-option
            v-for="user in users"
            :key="user.id"
            :label="`${user.userNo} · ${user.realName}`"
            :value="user.id"
            :disabled="user.accountStatus !== 'NORMAL'"
          >
            <span>{{ user.userNo }} · {{ user.realName }}</span>
            <span class="grant-form-card__user-status">{{ user.accountStatus }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="角色" prop="roleId">
        <el-select v-model="form.roleId" filterable placeholder="请选择角色" class="grant-form-card__select-role">
          <el-option
            v-for="role in roles"
            :key="role.id"
            :label="`${role.roleName}（${role.roleCode}）`"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="到期时间（选填）">
        <el-date-picker
          v-model="form.expireTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="留空 = 长期有效"
          class="grant-form-card__expire"
        />
      </el-form-item>
      <el-form-item label="授权原因" prop="grantReason" class="grant-form-card__reason">
        <el-input
          v-model="form.grantReason"
          maxlength="200"
          show-word-limit
          placeholder="如：财务专项组工作需要（留痕）"
        />
      </el-form-item>
      <el-form-item class="grant-form-card__submit">
        <el-button v-perm.disable="FP.GRANT_CREATE" type="primary" :icon="Promotion" :loading="submitting" @click="submit">
          授予
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.grant-form-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.grant-form-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.grant-form-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.grant-form-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.grant-form-card__select-user {
  width: 240px;
}

.grant-form-card__select-role {
  width: 220px;
}

.grant-form-card__expire {
  width: 220px;
}

.grant-form-card__reason {
  min-width: 320px;
}

.grant-form-card__user-status {
  float: right;
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>
