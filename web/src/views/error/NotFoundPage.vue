<script setup lang="ts">
import { useRouter } from 'vue-router'
import ResultPage from './ResultPage.vue'

const router = useRouter()
</script>

<template>
  <ResultPage
    status="404"
    title="页面不存在"
    description="访问地址无效，或对应页面尚未在管理后台中开放。"
  >
    <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
  </ResultPage>
</template>
