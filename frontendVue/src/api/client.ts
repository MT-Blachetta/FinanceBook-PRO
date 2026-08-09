/**
 * Axios instance with sensible defaults and JWT interceptors.
 *
 * – Base URL "/api" so Vite proxy forwards to Spring Boot (port 8000)
 * – 10s timeout to avoid hanging UI
 * – Attaches stored JWT to every outgoing request
 * – On 401, clears token and redirects to login
 */

import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { TOKEN_KEY } from '@/utils/constants'

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor: attach JWT ────────────────────────────────

api.interceptors.request.use((config) => {
  const token =
    localStorage.getItem(TOKEN_KEY) ??
    sessionStorage.getItem(TOKEN_KEY)
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor: handle 401 ───────────────────────────────

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export default api
