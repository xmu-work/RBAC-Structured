<script setup lang="ts">
import { useOrgLazyLoad } from '@/composables/useOrgTree'

/** 组织树选择器（懒加载）：用于新建用户和组织管理场景 */
const model = defineModel<number | undefined>()

const { loadChildren } = useOrgLazyLoad()
</script>

<template>
  <el-tree-select
    v-model="model"
    lazy
    node-key="id"
    check-strictly
    :load="loadChildren"
    :props="{ label: 'orgName' }"
    placeholder="请选择组织"
    clearable
    style="width: 100%"
  />
</template>
