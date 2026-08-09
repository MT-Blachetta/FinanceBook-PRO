<!--
  404 Not Found page – ported from React NotFoundPage.tsx.
-->
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <h1>404</h1>
    <p>Page not found</p>
    <button @click="router.push('/')">Go to Home</button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: var(--color-text-secondary);
}

.not-found h1 {
  font-size: 4rem;
  color: var(--color-text-primary);
  margin-bottom: 0.5rem;
}

.not-found p {
  margin-bottom: 1.5rem;
  font-size: 1.1rem;
}

.not-found button {
  background: var(--color-positive);
  color: white;
  border: none;
  padding: 0.6rem 1.5rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 0.95rem;
}
</style>
