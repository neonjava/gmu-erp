import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Normal build: lives under /app on the Spring Boot server and is written straight into the jar's static folder.
// `npm run dev` proxies API calls to the Java app on :8080.
// Demo build (`--mode demo`): a static preview for GitHub Pages that answers API calls from saved sample data.
const java = 'http://localhost:8080';
export default defineConfig(({ mode }) => {
  const demo = mode === 'demo';
  return {
    base: demo ? '/gmu-erp/' : '/app/',
    plugins: [react()],
    build: demo
      ? { outDir: 'dist-demo', emptyOutDir: true }
      : { outDir: '../target/classes/static/app', emptyOutDir: true },
    server: {
      port: 5173,
      proxy: { '/api': java, '/login': java, '/logout': java, '/css': java, '/js': java, '/img': java },
    },
  };
});
