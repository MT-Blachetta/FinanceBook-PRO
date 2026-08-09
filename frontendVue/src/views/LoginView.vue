<!--
  Login page – ported from React LoginPage.tsx.
  Provides username/password form with "Stay logged in" checkbox.
-->
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const rememberMe = ref(false)
const error = ref('')
const isSubmitting = ref(false)

async function handleSubmit() {
  error.value = ''
  isSubmitting.value = true
  try {
    await authStore.login(username.value, password.value, rememberMe.value)
    router.push('/')
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'response' in e) {
      const axiosErr = e as { response?: { data?: { message?: string } } }
      error.value = axiosErr.response?.data?.message || 'Login failed. Please check your credentials.'
    } else {
      error.value = 'Login failed. Please check your credentials.'
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="login-wrapper">
    <div class="login-card">
      <h1>FinanceBook</h1>
      <p class="subtitle">Sign in to your account</p>

      <div v-if="error" class="error-message">{{ error }}</div>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="Enter your username"
            autocomplete="username"
            required
            autofocus
          />
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="Enter your password"
            autocomplete="current-password"
            required
          />
        </div>

        <div class="form-group checkbox-group">
          <label>
            <input v-model="rememberMe" type="checkbox" />
            Stay logged in
          </label>
        </div>

        <button type="submit" class="btn-submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Signing in…' : 'Sign In' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100dvh;
  background: #000;
}

.login-card {
  background: var(--color-surface);
  border: 1px solid #333;
  border-radius: var(--radius-lg);
  padding: 2.5rem;
  width: 100%;
  max-width: 400px;
  margin: 1rem;
}

.login-card h1 {
  text-align: center;
  color: var(--color-text-primary);
  margin-bottom: 0.25rem;
}

.subtitle {
  text-align: center;
  color: var(--color-text-secondary);
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
}

.error-message {
  background: rgba(231, 76, 60, 0.15);
  color: var(--color-negative);
  border: 1px solid rgba(231, 76, 60, 0.3);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  color: var(--color-text-secondary);
  font-size: 0.85rem;
  margin-bottom: 0.35rem;
}

.form-group input[type="text"],
.form-group input[type="password"] {
  width: 100%;
  background: #111;
  border: 1px solid #333;
  border-radius: var(--radius-md);
  padding: 0.65rem 0.9rem;
  color: var(--color-text-primary);
  font-size: 0.95rem;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-positive);
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: var(--color-text-secondary);
}

.checkbox-group input[type="checkbox"] {
  accent-color: var(--color-positive);
}

.btn-submit {
  width: 100%;
  background: var(--color-positive);
  color: white;
  border: none;
  padding: 0.75rem;
  border-radius: var(--radius-md);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: 0.5rem;
}

.btn-submit:hover { background: #059669; }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
