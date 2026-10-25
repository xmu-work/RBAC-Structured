import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd())
  const apiBase = env.VITE_API_BASE || '/api/v1'
  // 契约第 12 章「Mock 先行」：仅 dev 且显式开启时拦截，后端就绪后 VITE_USE_MOCK=false 即切真联调
  const useMock = command === 'serve' && env.VITE_USE_MOCK === 'true'

  return {
    plugins: [
      vue(),
      viteMockServe({
        mockPath: 'mock',
        enable: useMock,
        logger: true,
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy: {
        // 契约 10.6：dev 一律走 Vite proxy，禁止浏览器跨域直连 8080
        [apiBase]: {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
