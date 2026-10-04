import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: { '@': path.resolve(__dirname, 'src') },
    },
    server: {
      port: 5173,
      // In development the SPA calls /api on its own origin; Vite forwards to Spring Boot (no CORS needed).
      proxy: {
        '/api': {
          target: env.VITE_DEV_API_PROXY ?? 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
