/**
 * Vue Router configuration.
 *
 * Mirrors the React Router routes from App.tsx:
 *   /           → SummaryView (main payment list)
 *   /login      → LoginView
 *   /add        → AddPaymentView
 *   /add-success→ AddSuccessView
 *   /payment/:id/edit → EditPaymentView
 *   /categories → CategoriesView
 *   /category-types → CategoryTypesView
 *   /statistics → StatisticsView
 *   /404        → NotFoundView
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      name: 'summary',
      component: () => import('@/views/SummaryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/add',
      name: 'add-payment',
      component: () => import('@/views/AddPaymentView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/add-success',
      name: 'add-success',
      component: () => import('@/views/AddSuccessView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/payment/:id/edit',
      name: 'edit-payment',
      component: () => import('@/views/EditPaymentView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('@/views/CategoriesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/category-types',
      name: 'category-types',
      component: () => import('@/views/CategoryTypesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: () => import('@/views/StatisticsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/404',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/404',
    },
  ],
})

// ── Auth guard ──────────────────────────────────────────────────────

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  // Wait for initial token validation if still loading
  if (authStore.isLoading) {
    await authStore.validateToken()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return '/login'
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return '/'
  }
})

export default router
