import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // Libera acesso para celulares no Wi-Fi pelo IP da máquina
    port: 5173
  }
})
