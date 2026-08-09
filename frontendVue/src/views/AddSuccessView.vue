<!--
  Success page shown after creating a payment – ported from React AddSuccessPage.tsx.
-->
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="success-page">
    <div class="success-icon">✓</div>
    <h2>Payment Added Successfully!</h2>
    <p>Your payment item has been recorded.</p>
    <div class="actions">
      <button class="btn-primary" @click="router.push('/add')">Add Another</button>
      <button class="btn-secondary" @click="router.push('/')">Back to Summary</button>
    </div>
  </div>
</template>

<style scoped>
.success-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  text-align: center;
  gap: 0.75rem;
}

.success-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-positive);
  color: white;
  font-size: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}

h2 { color: var(--color-text-primary); }
p { color: var(--color-text-secondary); }

.actions {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-primary {
  background: var(--color-positive);
  color: white;
  border: none;
  padding: 0.6rem 1.5rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
}

.btn-secondary {
  background: #333;
  color: var(--color-text-primary);
  border: 1px solid #444;
  padding: 0.6rem 1.5rem;
  border-radius: var(--radius-md);
  cursor: pointer;
}
</style>
