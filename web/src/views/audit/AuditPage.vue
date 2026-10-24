<script setup lang="ts">
import { ref } from 'vue'
import AuditTableCard from './components/AuditTableCard.vue'
import AuditDetailDrawer from './components/AuditDetailDrawer.vue'
import type { AuditLog } from '@/types/audit.types'

/** 审计日志主页（契约 8.9）：查询卡 + 详情抽屉 */
const tableReloadSignal = ref(0)
const detailDrawerVisible = ref(false)
const detailLog = ref<AuditLog | null>(null)

function handleDetail(log: AuditLog): void {
  detailLog.value = log
  detailDrawerVisible.value = true
}
</script>

<template>
  <!-- 纯布局容器（契约 9.1-1）：只负责纵向排列与间距 -->
  <div class="audit-page">
    <AuditTableCard :reload-signal="tableReloadSignal" @detail="handleDetail" />
    <AuditDetailDrawer v-model="detailDrawerVisible" :log="detailLog" />
  </div>
</template>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
