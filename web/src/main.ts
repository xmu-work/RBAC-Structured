import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router, { installRouterGuards } from './router'
import { setupRequestAuth } from './router/request-auth'
import { registerDirectives } from './directives'
// 设计令牌先于组件加载（契约 9.1-2）：tokens → EP 主题桥接 → 全局基础样式
import './styles/tokens.css'
import './styles/element-theme.css'
import './styles/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
installRouterGuards()
setupRequestAuth(router)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
registerDirectives(app)

app.mount('#app')
