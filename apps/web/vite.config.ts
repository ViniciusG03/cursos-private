import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// A API local roda na 8080; o proxy mantém navegador e API na mesma origem (cookie de sessão e
// CSRF sem CORS). Os links de convite e recuperação apontam para a 5173, por isso strictPort.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
  },
})
