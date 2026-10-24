<script setup lang="ts">
import { onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { useDict } from '@/composables/useDict'

/**
 * 数据范围字典卡片（契约 8.8）：只读展示为主，新增属管理动作；
 * 字典全局共享（dict store 唯一来源，契约第 7 章）。
 */
const emit = defineEmits<{ create: [] }>()

const { dataScopes, dataScopesLoading, dataScopesError, loadDataScopes } = useDict()

onMounted(() => {
  void loadDataScopes()
})
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「数据范围字典」这一块视觉区域 -->
  <el-card class="scope-dict-card" shadow="never">
    <template #header>
      <div class="scope-dict-card__header">
        <div>
          <h2 class="scope-dict-card__title">数据范围字典</h2>
          <p class="scope-dict-card__hint">全局只读字典（{{ dataScopes.length }} 项），新增属管理动作</p>
        </div>
        <el-button type="primary" plain :icon="Plus" @click="emit('create')">新增范围</el-button>
      </div>
    </template>

    <el-result
      v-if="dataScopesError"
      icon="error"
      title="字典加载失败"
      :sub-title="dataScopesError"
    >
      <template #extra>
        <el-button type="primary" @click="loadDataScopes">重试</el-button>
      </template>
    </el-result>
    <el-table v-else v-loading="dataScopesLoading" :data="dataScopes">
      <el-table-column prop="scopeCode" label="范围编码" min-width="110" />
      <el-table-column prop="scopeName" label="范围名称" min-width="110" />
      <el-table-column label="说明" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="sortNo" label="排序" min-width="70" />
      <el-table-column label="状态" min-width="90">
        <template #default>
          <el-tag type="success" effect="light">有效</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.scope-dict-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.scope-dict-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.scope-dict-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.scope-dict-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
