<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { listAuditLogs } from '@/api/audit.api'
import { listSystems } from '@/api/system.api'
import { listUsers } from '@/api/user.api'
import { useTable } from '@/composables/useTable'
import { dateTimeColumnFormatter, toLocalISO } from '@/utils/time'
import { DigitalBool } from '@/constants/enums'
import type { AuditLog } from '@/types/audit.types'
import type { System } from '@/types/system.types'
import type { User } from '@/types/user.types'

/**
 * 审计日志查询卡（契约 8.9 / 9.2-1）：时间范围必填（默认当天），
 * 时间参数以 ISO 本地格式传参（契约 3.3）；日志永久保存，无任何删除入口（UC-06）。
 */
const props = defineProps<{ reloadSignal: number }>()

const emit = defineEmits<{ detail: [log: AuditLog] }>()

const users = ref<User[]>([])
const systems = ref<System[]>([])

/* ---------------- 查询区：时间范围必填（默认当天），显式触发 ---------------- */

/** date-picker 使用 value-format（字符串模式），区间值为 [开始, 结束] 字符串 */
type TimeRange = [string, string]

const draftTimeRange = ref<TimeRange | null>(defaultRange())
const draftUserId = ref<number | undefined>(undefined)
const draftSystemCode = ref('')

function defaultRange(): TimeRange {
  const today = dayjs()
  return [today.startOf('day').format('YYYY-MM-DD HH:mm:ss'), today.endOf('day').format('YYYY-MM-DD HH:mm:ss')]
}

const appliedQuery = ref<{
  startTime: string
  endTime: string
  userId?: number
  systemCode?: string
}>({
  startTime: toLocalISO(dayjs().startOf('day')),
  endTime: toLocalISO(dayjs().endOf('day')),
})

const fetcher = (params: Record<string, unknown>) =>
  listAuditLogs({
    page: params.page as number,
    size: params.size as number,
    ...appliedQuery.value,
  })

const { rows, total, page, size, loading, error, load } = useTable<AuditLog>(fetcher)

async function refresh(nextPage = page.value): Promise<void> {
  await load(nextPage)
}

function applyQuery(): void {
  if (!draftTimeRange.value) {
    return
  }
  appliedQuery.value = {
    startTime: toLocalISO(draftTimeRange.value[0]),
    endTime: toLocalISO(draftTimeRange.value[1]),
    userId: draftUserId.value,
    systemCode: draftSystemCode.value || undefined,
  }
  void refresh(1)
}

function resetQuery(): void {
  draftTimeRange.value = defaultRange()
  draftUserId.value = undefined
  draftSystemCode.value = ''
  applyQuery()
}

onMounted(() => {
  void refresh(1)
  void listUsers({ page: 1, size: 100 }).then((result) => {
    users.value = result.list
  })
  void listSystems().then((result) => {
    systems.value = result
  })
})

watch(
  () => props.reloadSignal,
  () => void refresh(),
)

function asLog(row: unknown): AuditLog {
  return row as AuditLog
}
</script>

<template>
  <!-- 视觉组件（契约 9.1-3）：承载「审计日志查询」这一块视觉区域 -->
  <el-card class="audit-card" shadow="never">
    <template #header>
      <div class="audit-card__header">
        <div>
          <h2 class="audit-card__title">审计日志</h2>
          <p class="audit-card__hint">
            共 {{ total }} 条 · 日志永久保存，不提供删除入口（UC-06）；导出能力待后端补齐
          </p>
        </div>
      </div>
    </template>

    <el-form class="audit-card__query" inline @submit.prevent="applyQuery">
      <el-form-item label="操作用户">
        <el-select
          v-model="draftUserId"
          placeholder="全部用户"
          clearable
          filterable
          class="audit-card__user-select"
        >
          <el-option
            v-for="user in users"
            :key="user.id"
            :label="`${user.userNo} · ${user.realName}`"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="系统">
        <el-select v-model="draftSystemCode" placeholder="全部系统" clearable class="audit-card__system-select">
          <el-option
            v-for="system in systems"
            :key="system.id"
            :label="system.systemName"
            :value="system.systemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围（必填）">
        <el-date-picker
          v-model="draftTimeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          :clearable="false"
          class="audit-card__range"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="applyQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="error" class="audit-card__error" type="error" :title="error" :closable="false" show-icon>
      <el-button size="small" type="primary" @click="refresh()">重试</el-button>
    </el-alert>
    <el-table v-else v-loading="loading" :data="rows">
      <el-table-column prop="operateTime" label="操作时间" min-width="165" :formatter="dateTimeColumnFormatter" />
      <el-table-column label="操作人" min-width="110">
        <template #default="{ row }">{{ asLog(row).username ?? `#${asLog(row).userId ?? '—'}` }}</template>
      </el-table-column>
      <el-table-column prop="action" label="操作" min-width="140" />
      <el-table-column prop="systemCode" label="所属系统" min-width="90">
        <template #default="{ row }">{{ asLog(row).systemCode ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="对象" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ asLog(row).target ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="结果" min-width="80">
        <template #default="{ row }">
          <el-tag :type="asLog(row).success === DigitalBool.YES ? 'success' : 'danger'" effect="light">
            {{ asLog(row).success === DigitalBool.YES ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="来源 IP" min-width="120">
        <template #default="{ row }">{{ asLog(row).ip ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="emit('detail', asLog(row))">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="audit-card__pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="refresh()"
        @size-change="refresh(1)"
      />
    </div>
  </el-card>
</template>

<style scoped>
.audit-card {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.audit-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.audit-card__title {
  margin: var(--space-0);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.audit-card__hint {
  margin: var(--space-2) var(--space-0) var(--space-0);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.audit-card__query {
  margin-bottom: var(--space-2);
}

.audit-card__user-select {
  width: 200px;
}

.audit-card__system-select {
  width: 160px;
}

.audit-card__range {
  width: 360px;
}

.audit-card__error {
  margin-bottom: var(--space-3);
}

.audit-card__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}
</style>
