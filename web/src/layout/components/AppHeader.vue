<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Expand, Fold, SwitchButton } from '@element-plus/icons-vue'
import { resetAccessState } from '@/router/access'
import { useAppStore } from '@/stores/app.store'
import { useAuthStore } from '@/stores/auth.store'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const displayName = computed(() => authStore.displayName || '当前用户')

async function logout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await authStore.logout()
  } catch {
    ElMessage.warning('服务端登出失败，本地会话已清除')
  } finally {
    resetAccessState()
    appStore.resetTabs()
    await router.replace('/login')
  }
}
</script>

<template>
  <div class="app-header">
    <div class="app-header__left">
      <el-button
        class="app-header__collapse"
        text
        :aria-label="appStore.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="appStore.toggleSidebar"
      >
        <el-icon :size="20">
          <Expand v-if="appStore.sidebarCollapsed" />
          <Fold v-else />
        </el-icon>
      </el-button>
    </div>

    <div class="app-header__right">
      <div class="app-header__user">
        <span class="app-header__avatar">{{ displayName.slice(0, 1) }}</span>
        <span class="app-header__name">{{ displayName }}</span>
      </div>
      <el-button text :icon="SwitchButton" @click="logout">退出</el-button>
    </div>
  </div>
</template>

<style scoped>
.app-header {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-0) var(--space-6);
  border-bottom: var(--border-width-base) solid var(--color-border-light);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-header);
}

.app-header__left,
.app-header__right,
.app-header__user {
  display: flex;
  align-items: center;
}

.app-header__left {
  gap: var(--space-2);
  min-width: 0;
}

.app-header__right {
  gap: var(--space-4);
}

.app-header__user {
  gap: var(--space-2);
}

.app-header__avatar {
  display: grid;
  width: var(--space-8);
  height: var(--space-8);
  place-items: center;
  border-radius: var(--radius-round);
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-light-9);
}

.app-header__name {
  max-width: var(--layout-sidebar-width);
  overflow: hidden;
  color: var(--color-text-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
