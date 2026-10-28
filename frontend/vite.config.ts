import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import './server/env.mjs'
import { createApiHandler } from './server/api.mjs'

export default defineConfig({
  plugins: [react(), {
    name: 'soltech-paper-api',
    configureServer(server) {
      const api = createApiHandler()
      server.middlewares.use((req, res, next) => {
        if (req.url?.startsWith('/api/')) void api(req, res)
        else next()
      })
    },
  }],
  server: { host: '127.0.0.1' },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
