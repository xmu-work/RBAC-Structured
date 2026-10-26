<script setup lang="ts">
import { useAuthStore } from '@/stores/auth.store'
import PingCard from './components/PingCard.vue'

/** 工程骨架自检卡片依赖 mock 专用的 /demo/ping，真实后端下不展示，避免 404 */
const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const authStore = useAuthStore()
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 flex 排列与间距，不含颜色/圆角/阴影/字体等视觉属性 -->
  <div class="home-page">
    <PingCard v-if="useMock" />
    <el-card v-else shadow="never">
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
