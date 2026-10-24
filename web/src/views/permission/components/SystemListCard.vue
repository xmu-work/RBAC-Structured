<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { listSystems } from '@/api/system.api'
import { useDict } from '@/composables/useDict'
import type { System } from '@/types/system.types'

/** 接入系统列表卡片（契约 8.7）：系统登记入口；系统编辑/停用 [缺] 端点 */
const props = defineProps<{ reloadSignal: number }>()

const emit = defineEmits<{ create: []; loaded: [systems: System[]] }>()

const { dictLabel, commonStatusOptions } = useDict()

const systems = ref<System[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    systems.value = await listSystems()
    emit('loaded', systems.value)
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '系统列表加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

watch(
  () => props.reloadSignal,
  () => void load(),
)
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「接入系统列表」这一块视觉区域 -->
  <el-card class="system-list-card" shadow="never">
    <template #header>
      <div class="system-list-card__header">
        <div>
          <h2 class="system-list-card__title">接入系统</h2>
          <p class="system-list-card__hint">共 {{ systems.length }} 个系统接入统一权限平台</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="emit('create')">登记系统</el-button>
      </div>
    </template>

    <el-alert
      v-if="error"
      class="system-list-card__error"
      type="error"
      :title="error"
      :closable="false"
      show-icon
    >
      <el-button size="small" type="primary" @click="load">重试</el-button>
    </el-alert>
    <el-table v-else v-loading="loading" :data="systems">
      <el-table-column prop="systemCode" label="系统编码" min-width="110" />
      <el-table-column prop="systemName" label="系统名称" min-width="170" />
      <el-table-column label="描述" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" min-width="90">
        <template #default="{ row }">
          <el-tag type="success" effect="light">
            {{ dictLabel(commonStatusOptions, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="接入时间" min-width="160" />
    </el-table>
  </el-card>
</template>

<style scoped>
.system-list-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.system-list-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.system-list-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.system-list-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.system-list-card__error {
  margin-bottom: var(--space-3);
}
</style>
