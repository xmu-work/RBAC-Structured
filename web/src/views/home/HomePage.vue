<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.store'

const authStore = useAuthStore()
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 flex 排列与间距，不含颜色/圆角/阴影/字体等视觉属性 -->
  <div class="home-page">
    <el-card shadow="never">
      <el-result icon="success" :title="`欢迎，${authStore.displayName ?? ''}`" sub-title="请从左侧菜单进入各管理模块" />
    </el-card>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-6);
}
</style>
