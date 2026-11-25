import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
    watch: {
      usePolling: true,
    },
  },
  build: {
    outDir: '../eterna-ingest-service/src/main/resources/static',
    emptyOutDir: true
  }
})
