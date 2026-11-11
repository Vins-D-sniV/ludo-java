import { defineConfig } from 'vite';

const proxy = { '/api': process.env.LUDO_API_URL || 'http://localhost:8080' };

export default defineConfig({
  esbuild: { jsx: 'automatic' },
  server: { port: 5173, strictPort: true, proxy },
  preview: { port: 4173, strictPort: true, proxy },
});
