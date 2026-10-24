<script setup lang="ts">
withDefaults(
  defineProps<{
    status: '403' | '404' | '500'
    title: string
    description: string
  }>(),
  {},
)
</script>

<template>
  <main class="result-page">
    <el-card class="result-page__card" shadow="never">
      <el-result :icon="status === '403' ? 'warning' : 'error'" :title="title">
        <template #sub-title>
          <p class="result-page__code">{{ status }}</p>
          <p class="result-page__description">{{ description }}</p>
        </template>
        <template #extra>
          <slot />
        </template>
      </el-result>
    </el-card>
  </main>
</template>

<style scoped>
.result-page {
  display: grid;
  min-height: 100vh;
  padding: var(--space-6);
  place-items: center;
  background: var(--color-bg-page);
}

.result-page__card {
  width: min(var(--layout-result-card-width), 100%);
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.result-page__code {
  margin: var(--space-0);
  color: var(--color-primary);
  font-size: var(--font-size-title);
  font-weight: 600;
}

.result-page__description {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  line-height: 1.7;
}
</style>
