import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发期把 /api 代理到 Nginx 网关（不直连后端端口，口径同设计报告 5.2.7）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    // S104：emptyOutDir=false——WorkBuddy 安全钩子会拦截 vite 清空 dist 的批量删除导致构建中断；
    // 覆盖式构建（旧 hash chunk 残留不影响 index.html 引用，定期手动清理）
    emptyOutDir: false,
    sourcemap: false
  }
})
