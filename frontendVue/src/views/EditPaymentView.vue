<!--
  Edit payment page – ported from React EditItemPage.tsx.
-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaymentItemForm from '@/components/PaymentItemForm.vue'
import { paymentApi } from '@/api/payment.api'
import type { PaymentItem, PaymentItemFormData } from '@/types/models'

const route = useRoute()
const router = useRouter()
const paymentId = Number(route.params.id)

const existingItem = ref<PaymentItem | null>(null)
const isLoading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    existingItem.value = await paymentApi.getById(paymentId)
  } catch (e) {
    error.value = 'Failed to load payment item.'
    console.error(e)
  } finally {
    isLoading.value = false
  }
})

async function handleSubmit(data: PaymentItemFormData) {
  try {
    await paymentApi.update(paymentId, data)
    router.push('/')
  } catch (e) {
    console.error('Failed to update payment:', e)
    alert('Failed to update payment item. Please try again.')
  }
}
</script>

<template>
  <div class="edit-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← Back</button>
      <h2>Edit Payment</h2>
    </div>

    <div v-if="isLoading" class="status">Loading…</div>
    <div v-else-if="error" class="status error">{{ error }}</div>

    <PaymentItemForm
      v-else-if="existingItem"
      :initial-data="{
        amount: existingItem.amount,
        date: existingItem.date.split('T')[0],
        periodic: existingItem.periodic,
        description: existingItem.description,
        recipientId: existingItem.recipientId,
        standardCategoryId: existingItem.standardCategoryId,
      }"
      submit-label="Update Payment"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.edit-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.page-header h2 { color: var(--color-text-primary); }

.back-btn {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.95rem;
  padding: 0;
}

.back-btn:hover { color: var(--color-text-primary); }

.status {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 2rem;
}

.status.error { color: var(--color-negative); }
</style>
