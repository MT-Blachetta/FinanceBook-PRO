<!--
  Root component – mirrors React App.tsx.

  Authentication gating:
    - Unauthenticated users see only the LoginView (no NavigationBar)
    - Authenticated users see the full app with NavigationBar + RouterView

  The layout uses a dark theme with a max-width content area.
-->
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NavigationBar from '@/components/NavigationBar.vue'
import NavigationDrawer from '@/components/NavigationDrawer.vue'
import type { ViewFilter } from '@/types/models'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isDrawerOpen = ref(false)

// Current filter from URL query
const currentFilter = computed<ViewFilter>(() => {
  return (route.query.filter as ViewFilter) || 'all'
})

// Validate stored token on mount
onMounted(async () => {
  await authStore.validateToken()
})

function handleFilterChange(filter: ViewFilter) {
  const query = { ...route.query }
  if (filter === 'all') {
    delete query.filter
  } else {
    query.filter = filter
  }
  router.push({ path: '/', query })
  isDrawerOpen.value = false
}

function handleMenuClick() {
  isDrawerOpen.value = !isDrawerOpen.value
}

function handleAddClick() {
  router.push('/add')
  isDrawerOpen.value = false
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function closeDrawer() {
  if (isDrawerOpen.value) {
    isDrawerOpen.value = false
  }
}
</script>

<template>
  <!-- Loading state while auth is being initialized -->
  <div v-if="authStore.isLoading" class="loading-screen">
    Loading…
  </div>

  <!-- Authenticated: full app layout -->
  <div v-else-if="authStore.isAuthenticated" class="wrapper">
    <NavigationBar
      :active="currentFilter"
      @change="handleFilterChange"
      @menu="handleMenuClick"
      @add="handleAddClick"
      @logout="handleLogout"
    />
    <NavigationDrawer
      :open="isDrawerOpen"
      @close="isDrawerOpen = false"
      @navigate="(path: string) => { router.push(path); isDrawerOpen = false }"
    />

    <main class="content" @click="closeDrawer">
      <RouterView />
    </main>
  </div>

  <!-- Unauthenticated: just router (login page) -->
  <RouterView v-else />
</template>

<style scoped>
.loading-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100dvh;
  background: #000;
  color: var(--color-text-secondary);
  font-size: 1.1rem;
}

.wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  background: #000;
}

.content {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 48rem;
  margin-inline: auto;
  padding: 1rem;
  color: #eaeaea;
}
</style>
