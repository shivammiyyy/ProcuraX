// vite.config.js
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import path from "path"
import tailwindcss from "@tailwindcss/vite"

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '');
  const backend = env.VITE_DEV_API_PROXY || 'http://localhost:8080';

  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: {
        '/api': { target: backend, changeOrigin: false },
        '/oauth2': { target: backend, changeOrigin: false },
        '/login/oauth2': { target: backend, changeOrigin: false },
        '/logout': { target: backend, changeOrigin: false },
      },
    },
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
  };
});