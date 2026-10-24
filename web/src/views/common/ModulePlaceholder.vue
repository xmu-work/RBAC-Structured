<script setup lang="ts">
defineProps<{
  title: string
  functionPointId?: number
}>()
</script>

<template>
  <section class="module-placeholder">
    <div class="module-placeholder__heading">
      <div>
        <p class="module-placeholder__eyebrow">管理后台 · 模块入口</p>
        <h1 class="module-placeholder__title">{{ title }}</h1>
      </div>
      <el-tag type="success" effect="plain">框架已接入</el-tag>
    </div>

    <el-card class="module-placeholder__card" shadow="never">
      <el-empty description="业务功能将在本模块中继续实现">
        <template #image>
          <div class="module-placeholder__mark">RBAC</div>
        </template>
        <p class="module-placeholder__hint">路由、权限过滤、主布局、会话与 API 基础层均已就绪。</p>
        <p v-if="functionPointId" class="module-placeholder__meta">
          功能点 ID：{{ functionPointId }}
        </p>
      </el-empty>
    </el-card>
  </section>
</template>

<style scoped>
.module-placeholder {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.module-placeholder__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.module-placeholder__eyebrow {
  margin: var(--space-0) var(--space-0) var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.module-placeholder__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-title);
  line-height: 1.4;
}

.module-placeholder__card {
  min-height: var(--layout-content-min-height);
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.module-placeholder__mark {
  display: grid;
  width: var(--size-placeholder-mark);
  height: var(--size-placeholder-mark);
  place-items: center;
  border-radius: var(--radius-round);
  color: var(--color-text-inverse);
  font-size: var(--font-size-lg);
  font-weight: 600;
  background: var(--color-primary);
}

.module-placeholder__hint,
.module-placeholder__meta {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
