/**
 * Vite configuration for FinanceBook Front-End.
 *
 * Key points
 * ----------
 * - Uses '@vitejs/plugin-react' to enable React Fast Refresh and the new JSX
 *   transform (no need to import React in every file).
 * - Adds a development proxy so API calls to '/api/...' are transparently
 *   forwarded to the Spring Boot backend listening on port 8000.  The 'rewrite'
 *   strips the '/api' prefix so routes match the backend exactly.
 * - 'strictPort' makes the dev server fail if port 5173 is already in use
 *   instead of silently switching to another port.
 */
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://localhost:8000",
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, ""),
      },
    },
  },
});