<script setup lang="ts">
import { useRoute } from 'vue-router'
import ModulePlaceholder from '@/views/common/ModulePlaceholder.vue'

const route = useRoute()
</script>

<template>
  <ModulePlaceholder
    :title="String(route.meta.title)"
    :function-point-id="route.meta.functionPointId"
  />
</template>
