<script setup lang="ts">
import { useOrgLazyLoad } from '@/composables/useOrgTree'

/** 组织树选择器（懒加载）：新建用户、数据权限 BRANCH/DEPT 选择等场景复用 */
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
