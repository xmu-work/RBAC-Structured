<script setup lang="ts">
import { useRouter } from 'vue-router'
import ResultPage from './ResultPage.vue'

const router = useRouter()
</script>

<template>
  <ResultPage
    status="500"
    title="服务暂时不可用"
    description="请稍后重试；若问题持续存在，请联系系统管理员。"
  >
    <el-button type="primary" @click="router.go(0)">重新加载</el-button>
  </ResultPage>
</template>
