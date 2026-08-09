/**
 * Pinia store for authentication state.
 *
 * Mirrors the React AuthContext:
 * - JWT token storage (localStorage / sessionStorage)
 * - User profile state
 * - Login / logout / token validation flows
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth.api'
import type { UserRead } from '@/types/models'
import { TOKEN_KEY } from '@/utils/constants'

function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY)
}

function storeToken(token: string, remember: boolean): void {
  if (remember) {
    localStorage.setItem(TOKEN_KEY, token)
    sessionStorage.removeItem(TOKEN_KEY)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
    localStorage.removeItem(TOKEN_KEY)
  }
}

function clearStoredToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserRead | null>(null)
  const isLoading = ref(true)

  const isAuthenticated = computed(() => !!token.value)

  /** Validate an existing token by fetching the user profile. */
  async function validateToken(): Promise<boolean> {
    const stored = getStoredToken()
    if (!stored) {
      isLoading.value = false
      return false
    }

    token.value = stored
    try {
      user.value = await authApi.getCurrentUser()
      isLoading.value = false
      return true
    } catch {
      logout()
      isLoading.value = false
      return false
    }
  }

  /** Authenticate and store token. */
  async function login(username: string, password: string, rememberMe: boolean): Promise<void> {
    const response = await authApi.login({ username, password })
    token.value = response.accessToken
    storeToken(response.accessToken, rememberMe)
    user.value = await authApi.getCurrentUser()
  }

  /** Clear auth state. */
  function logout(): void {
    token.value = null
    user.value = null
    clearStoredToken()
  }

  return {
    token,
    user,
    isLoading,
    isAuthenticated,
    validateToken,
    login,
    logout,
  }
})
