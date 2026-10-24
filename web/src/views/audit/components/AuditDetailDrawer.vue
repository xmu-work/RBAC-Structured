<script setup lang="ts">
import type { AuditLog } from '@/types/audit.types'

/** 审计详情抽屉（契约 8.9）：展示 detail / ip / decisionId；只读不可修改 */
defineProps<{ log: AuditLog | null }>()

const visible = defineModel<boolean>({ default: false })

function dash(value: string | number | null | undefined): string {
  return value === null || value === undefined || value === '' ? '—' : String(value)
}
</script>

<template>
  <el-drawer v-model="visible" title="审计日志详情" size="460px">
    <template v-if="log">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="日志 ID">{{ log.logId }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ log.operateTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ dash(log.username) }}（#{{ dash(log.userId) }}）</el-descriptions-item>
        <el-descriptions-item label="操作">{{ log.action }}</el-descriptions-item>
        <el-descriptions-item label="所属系统">{{ dash(log.systemCode) }}</el-descriptions-item>
        <el-descriptions-item label="对象">{{ dash(log.target) }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ log.success === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="来源 IP">{{ dash(log.ip) }}</el-descriptions-item>
        <el-descriptions-item label="决策 ID">{{ dash(log.decisionId) }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ dash(log.detail) }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        class="audit-detail-drawer__note"
        type="info"
        :closable="false"
        show-icon
        title="日志永久保存且不可修改（UC-06）；导出功能上线后按钮文案将注明「导出将被记录」。"
      />
    </template>
    <el-empty v-else description="暂无数据" />
  </el-drawer>
</template>

<style scoped>
.audit-detail-drawer__note {
  margin-top: var(--space-4);
}
</style>
