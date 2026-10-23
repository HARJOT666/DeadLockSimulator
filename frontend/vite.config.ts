import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// The dev server proxies /api to Spring Boot, so the browser sees one origin and the
// HttpOnly SameSite=Strict auth cookie is sent with every API request.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      proxy: {
        '/api': { target: env.VITE_BACKEND_URL || 'http://localhost:8080', changeOrigin: false },
      },
    },
    build: {
      chunkSizeWarningLimit: 900,
      rollupOptions: {
        output: {
          manualChunks: {
            react: ['react', 'react-dom', 'react-router-dom'],
            flow: ['@xyflow/react'],
            charts: ['recharts'],
            motion: ['framer-motion'],
          },
        },
      },
    },
  }
})
