<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const useMock = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true'

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const statusMessage = computed(() =>
  typeof route.query.reason === 'string' ? route.query.reason : '',
)

function safeRedirect(): string {
  const redirect = route.query.redirect
  if (
    typeof redirect === 'string' &&
    redirect.startsWith('/') &&
    !redirect.startsWith('//') &&
    redirect !== '/login'
  ) {
    return redirect
  }
  return '/'
}

async function submit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password,
    })
    await router.replace(safeRedirect())
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-page__intro">
      <div class="login-page__intro-content">
        <span class="login-page__badge">RBAC · 统一身份与权限平台</span>
        <h1 class="login-page__title">一个入口，<br />统一管理所有权限</h1>
        <p class="login-page__subtitle">
          面向组织、用户、角色、功能权限和数据范围的统一管理控制台。
        </p>
        <ul class="login-page__features">
          <li>RBAC 权限模型与动态路由</li>
          <li>分级授权与审计留痕</li>
          <li>统一契约、可靠会话与权限同步</li>
        </ul>
      </div>
    </section>

    <section class="login-page__panel">
      <el-card class="login-card" shadow="never">
        <template #header>
          <div>
            <p class="login-card__eyebrow">IAM CONSOLE</p>
            <h2 class="login-card__title">登录管理后台</h2>
            <p class="login-card__hint">请使用统一账号继续</p>
          </div>
        </template>

        <el-alert
          v-if="statusMessage"
          class="login-card__alert"
          :title="statusMessage"
          type="warning"
          :closable="false"
          show-icon
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              placeholder="请输入账号"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>

          <!-- [待后端] 验证码与 RSA 加密插槽在对应端点落地后启用（契约 5.2）。 -->
          <el-button
            class="login-card__submit"
            type="primary"
            native-type="submit"
            :loading="submitting"
          >
            登录
          </el-button>
        </el-form>

        <p v-if="useMock" class="login-card__mock">Mock 模式：输入任意非空账号与密码即可登录</p>
      </el-card>
      <p class="login-page__copyright">统一应用软件权限管理系统</p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(var(--space-0), 1fr) minmax(var(--layout-login-card-width), 0.8fr);
}

.login-page__intro {
  display: flex;
  align-items: center;
  padding: var(--space-10);
  color: var(--color-text-inverse);
  background:
    radial-gradient(circle at 15% 20%, rgb(92 124 240 / 65%), transparent 35%),
    linear-gradient(145deg, var(--color-primary-dark), var(--color-primary));
}

.login-page__intro-content {
  width: min(var(--layout-result-card-width), 100%);
  margin: auto;
}

.login-page__badge {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  border: var(--border-width-base) solid rgb(255 255 255 / 35%);
  border-radius: var(--radius-round);
  color: rgb(255 255 255 / 84%);
  font-size: var(--font-size-sm);
  background: rgb(255 255 255 / 10%);
}

.login-page__title {
  margin: var(--space-6) var(--space-0) var(--space-4);
  font-size: calc(var(--font-size-title) * 1.6);
  font-weight: 600;
  line-height: 1.35;
}

.login-page__subtitle {
  max-width: var(--layout-result-card-width);
  margin: var(--space-0);
  color: rgb(255 255 255 / 76%);
  font-size: var(--font-size-lg);
  line-height: 1.8;
}

.login-page__features {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: var(--space-8) var(--space-0) var(--space-0);
  padding: var(--space-0);
  color: rgb(255 255 255 / 84%);
  list-style: none;
}

.login-page__features li::before {
  margin-right: var(--space-2);
  color: var(--color-success);
  content: '●';
}

.login-page__panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  background: var(--color-bg-page);
}

.login-card {
  width: min(var(--layout-login-card-width), 100%);
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-popover);
}

.login-card__eyebrow {
  margin: var(--space-0);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.12em;
}

.login-card__title {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-title);
}

.login-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.login-card__alert {
  margin-bottom: var(--space-5);
}

.login-card__submit {
  width: 100%;
  margin-top: var(--space-2);
}

.login-card__mock {
  margin: var(--space-5) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  text-align: center;
}

.login-page__copyright {
  margin: var(--space-6) var(--space-0) var(--space-0);
  color: var(--color-text-placeholder);
  font-size: var(--font-size-xs);
}

@media (width <= 860px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-page__intro {
    display: none;
  }
}
</style>
