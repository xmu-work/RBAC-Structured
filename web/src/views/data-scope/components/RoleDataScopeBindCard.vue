<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Coin } from '@element-plus/icons-vue'
import { listRoles } from '@/api/role.api'
import { bindRoleDataScope } from '@/api/system.api'
import { useAuthStore } from '@/stores/auth.store'
import { useDict } from '@/composables/useDict'
import { DataScopeType, type DataScopeTypeCode } from '@/constants/enums'
import { toFieldErrors } from '@/utils/formErrors'
import OrgTreeSelect from '@/views/organization/components/OrgTreeSelect.vue'
import type { Role } from '@/types/role.types'

/**
 * 角色-数据范围授权卡片（契约 8.8）：scopeType 字典联动组织/项目选择器；
 * [待后端] 已授权查询与回收端点；临时授权 expireTime 后端签名缺失。
 */
const formRef = ref<FormInstance>()
const submitting = ref(false)
const serverErrors = ref<Record<string, string>>({})

const authStore = useAuthStore()
const { dataScopeTypeOptions } = useDict()

const roles = ref<Role[]>([])

const form = reactive<{
  roleId: number | undefined
  scopeType: DataScopeTypeCode | ''
  orgId: number | undefined
  projectId: number | undefined
}>({
  roleId: undefined,
  scopeType: '',
  orgId: undefined,
  projectId: undefined,
})

onMounted(() => {
  void listRoles().then((list) => {
    roles.value = list
  })
})

/** 契约 8.8：BRANCH/DEPT 联动组织选择器，PROJECT 联动项目选择器（项目即 PROJECT 型组织节点） */
const needOrg = computed(() => form.scopeType === DataScopeType.BRANCH || form.scopeType === DataScopeType.DEPT)
const needProject = computed(() => form.scopeType === DataScopeType.PROJECT)

// scopeType 变更时清空已选的组织/项目，避免跨类型残留
watch(
  () => form.scopeType,
  () => {
    form.orgId = undefined
    form.projectId = undefined
    formRef.value?.clearValidate(['orgId', 'projectId'])
  },
)

const rules: FormRules = {
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  scopeType: [{ required: true, message: '请选择数据范围类型', trigger: 'change' }],
  orgId: [
    {
      validator: (_rule, _value, callback) => {
        if (needOrg.value && form.orgId === undefined) {
          callback(new Error('分公司/部门范围必须指定组织'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  projectId: [
    {
      validator: (_rule, _value, callback) => {
        if (needProject.value && form.projectId === undefined) {
          callback(new Error('项目范围必须指定项目部'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

async function submit(): Promise<void> {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false)
  if (!valid || form.roleId === undefined || form.scopeType === '') {
    return
  }
  submitting.value = true
  serverErrors.value = {}
  try {
    await bindRoleDataScope({
      roleId: form.roleId,
      scopeType: form.scopeType,
      orgId: needOrg.value ? form.orgId : undefined,
      projectId: needProject.value ? form.projectId : undefined,
      createdBy: authStore.userId ?? undefined,
    })
    const roleName = roles.value.find((role) => role.id === form.roleId)?.roleName ?? `#${form.roleId}`
    ElMessage.success(`已为「${roleName}」配置数据范围（${form.scopeType}）`)
    form.roleId = undefined
    form.scopeType = ''
    form.orgId = undefined
    form.projectId = undefined
    formRef.value?.clearValidate()
  } catch (error) {
    serverErrors.value = toFieldErrors(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「角色数据范围授权」这一块视觉区域 -->
  <el-card class="scope-bind-card" shadow="never">
    <template #header>
      <div>
        <h2 class="scope-bind-card__title">角色-数据范围授权</h2>
        <p class="scope-bind-card__hint">
          已授权列表与回收端点待后端补齐，v1 仅提供授权表单；临时授权（expireTime）端点暂缺
        </p>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="角色" prop="roleId">
        <el-select v-model="form.roleId" filterable placeholder="请选择角色" style="width: 100%">
          <el-option
            v-for="role in roles"
            :key="role.id"
            :label="`${role.roleName}（${role.roleCode}）`"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="数据范围类型" prop="scopeType">
        <el-radio-group v-model="form.scopeType">
          <el-radio-button v-for="option in dataScopeTypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="needOrg" label="指定组织" prop="orgId">
        <OrgTreeSelect v-model="form.orgId" />
      </el-form-item>
      <el-form-item v-if="needProject" label="指定项目部" prop="projectId">
        <OrgTreeSelect v-model="form.projectId" />
        <p class="scope-bind-card__project-hint">项目为组织树中「项目部」类型节点</p>
      </el-form-item>
      <el-button type="primary" :icon="Coin" :loading="submitting" style="width: 100%" @click="submit">
        授权数据范围
      </el-button>
    </el-form>
  </el-card>
</template>

<style scoped>
.scope-bind-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.scope-bind-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.scope-bind-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.scope-bind-card__project-hint {
  margin: var(--space-1) var(--space-0) var(--space-0);
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}
</style>
