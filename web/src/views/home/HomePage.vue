<script setup lang="ts">
import PingCard from './components/PingCard.vue'
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责 flex 排列与间距，不含颜色/圆角/阴影/字体等视觉属性 -->
  <div class="home-page">
    <PingCard />
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
