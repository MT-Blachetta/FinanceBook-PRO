<!--
  Add payment page – ported from React AddItemPage.tsx.
-->
<script setup lang="ts">
import { useRouter } from 'vue-router'
import PaymentItemForm from '@/components/PaymentItemForm.vue'
import { paymentApi } from '@/api/payment.api'
import type { PaymentItemFormData } from '@/types/models'

const router = useRouter()

async function handleSubmit(data: PaymentItemFormData) {
  try {
    await paymentApi.create(data)
    router.push('/add-success')
  } catch (e) {
    console.error('Failed to create payment:', e)
    alert('Failed to create payment item. Please try again.')
  }
}
</script>

<template>
  <div class="add-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← Back</button>
      <h2>Add Payment</h2>
    </div>
    <PaymentItemForm submit-label="Create Payment" @submit="handleSubmit" />
  </div>
</template>

<style scoped>
.add-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.page-header h2 {
  color: var(--color-text-primary);
}

.back-btn {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.95rem;
  padding: 0;
}

.back-btn:hover { color: var(--color-text-primary); }
</style>
