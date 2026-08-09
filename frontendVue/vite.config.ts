/**
 * Vite configuration for FinanceBook Vue.js Front-End.
 *
 * - Uses '@vitejs/plugin-vue' for Vue 3 SFC support.
 * - Adds a development proxy so API calls to '/api/...' are transparently
 *   forwarded to the Java Spring Boot backend listening on port 8000.
 *   The 'rewrite' strips the '/api' prefix so routes match the backend exactly.
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
