<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { get } from '@/utils/request'

interface PingData {
  pong: boolean
  time: string
}

const loading = ref(true)
const failed = ref(false)
const data = ref<PingData | null>(null)

onMounted(async () => {
  // Mock 管道自检：dev 且 VITE_USE_MOCK=true 时由 vite-plugin-mock 返回契约级响应
  try {
    data.value = await get<PingData>('/demo/ping')
  } catch {
    // 错误提示已由响应拦截器统一 toast；此处局部 catch 仅为落地 error 三态（契约 9.2-4）
    failed.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- 视觉组件（契约 9.1-1/9.1-3）：独立承载"骨架自检"这一块视觉区域，样式全部引用令牌 -->
  <el-card class="ping-card">
    <template #header>
      <span class="ping-card__title">工程骨架自检</span>
    </template>

    <el-skeleton v-if="loading" :rows="2" animated />
    <el-result
      v-else-if="failed"
      icon="error"
      title="请求失败"
      sub-title="请确认 dev 服务已启动且 Mock 已开启"
    />
    <template v-else-if="data">
      <p class="ping-card__line">Mock 管道：pong = {{ data.pong }}</p>
      <p class="ping-card__line">响应时间（原样直显）：{{ data.time }}</p>
      <p class="ping-card__hint">Axios 封装 · 错误矩阵 · 设计令牌 · Mock 拦截均已生效</p>
    </template>
    <el-empty v-else description="暂无数据" />
  </el-card>
</template>

<style scoped>
.ping-card {
  max-width: 560px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.ping-card__title {
  font-size: var(--font-size-lg);
  color: var(--color-text-primary);
}

.ping-card__line {
  margin: var(--space-0);
  font-size: var(--font-size-base);
  color: var(--color-text-regular);
  line-height: 2;
}

.ping-card__hint {
  margin: var(--space-3) var(--space-0) var(--space-0);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>
