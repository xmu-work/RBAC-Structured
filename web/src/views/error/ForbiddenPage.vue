<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ResultPage from './ResultPage.vue'
import { refreshAccessRoutes } from '@/router/access'
import { usePermissionStore } from '@/stores/permission.store'

const router = useRouter()
const permissionStore = usePermissionStore()
const refreshing = ref(false)

async function retryPermissions() {
  refreshing.value = true
  try {
    await refreshAccessRoutes()
    if (permissionStore.count > 0) {
      await router.replace('/')
    } else {
      ElMessage.warning('当前账号仍无可访问的管理后台功能点')
    }
  } finally {
    refreshing.value = false
  }
}
</script>

<template>
  <ResultPage
    status="403"
    title="暂无访问权限"
    description="当前账号未获授权访问该页面。权限变更后，可重新拉取权限。"
  >
    <el-button type="primary" :loading="refreshing" @click="retryPermissions">
      重新拉取权限
    </el-button>
    <el-button @click="router.push('/')">返回首页</el-button>
  </ResultPage>
</template>
