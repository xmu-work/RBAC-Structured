<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const items = computed(() =>
  route.matched
    .filter((record) => typeof record.meta.title === 'string')
    .map((record) => ({
      title: String(record.meta.title),
      path: record.path,
    })),
)
</script>

<template>
  <div class="content-breadcrumb">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item v-for="(item, index) in items" :key="`${item.path}-${index}`">
        {{ item.title }}
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<style scoped>
.content-breadcrumb {
  margin-bottom: var(--space-4);
  padding: var(--space-0) var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
