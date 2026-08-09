<!--
  Reusable payment item form – ported from React PaymentItemForm.tsx.
  Used by both AddPaymentView and EditPaymentView.
-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { categoryApi } from '@/api/category.api'
import { recipientApi } from '@/api/recipient.api'
import type { PaymentItemFormData, Category, Recipient } from '@/types/models'
import { DESCRIPTION_MAX_LENGTH } from '@/utils/constants'

const props = defineProps<{
  initialData?: Partial<PaymentItemFormData>
  submitLabel?: string
}>()

const emit = defineEmits<{
  submit: [data: PaymentItemFormData]
}>()

const amount = ref(props.initialData?.amount ?? 0)
const date = ref(props.initialData?.date ?? new Date().toISOString().split('T')[0])
const periodic = ref(props.initialData?.periodic ?? false)
const description = ref(props.initialData?.description ?? '')
const recipientId = ref<number | null>(props.initialData?.recipientId ?? null)
const standardCategoryId = ref<number | null>(props.initialData?.standardCategoryId ?? null)

const categories = ref<Category[]>([])
const recipients = ref<Recipient[]>([])
const isLoading = ref(true)

onMounted(async () => {
  try {
    const [cats, recs] = await Promise.all([
      categoryApi.getAll(),
      recipientApi.getAll(),
    ])
    categories.value = cats
    recipients.value = recs
  } catch (e) {
    console.error('Failed to load form data:', e)
  } finally {
    isLoading.value = false
  }
})

function handleSubmit() {
  const data: PaymentItemFormData = {
    amount: Number(amount.value),
    date: date.value,
    periodic: periodic.value,
    description: description.value || undefined,
    recipientId: recipientId.value,
    standardCategoryId: standardCategoryId.value,
  }
  emit('submit', data)
}
</script>

<template>
  <form class="payment-form" @submit.prevent="handleSubmit">
    <div v-if="isLoading" class="loading">Loading form data…</div>

    <template v-else>
      <div class="form-group">
        <label for="amount">Amount *</label>
        <input
          id="amount"
          v-model.number="amount"
          type="number"
          step="0.01"
          required
          placeholder="e.g. -49.99 for expense, 1200 for income"
        />
        <span class="hint">Negative = expense, Positive = income</span>
      </div>

      <div class="form-group">
        <label for="date">Date *</label>
        <input id="date" v-model="date" type="date" required />
      </div>

      <div class="form-group">
        <label for="description">Description</label>
        <textarea
          id="description"
          v-model="description"
          :maxlength="DESCRIPTION_MAX_LENGTH"
          rows="3"
          placeholder="Optional description…"
        />
      </div>

      <div class="form-group">
        <label for="recipient">Recipient</label>
        <select id="recipient" v-model="recipientId">
          <option :value="null">— None —</option>
          <option v-for="r in recipients" :key="r.id" :value="r.id">
            {{ r.name }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label for="category">Standard Category</label>
        <select id="category" v-model="standardCategoryId">
          <option :value="null">— None —</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>
      </div>

      <div class="form-group checkbox-group">
        <label>
          <input v-model="periodic" type="checkbox" />
          Periodic payment
        </label>
      </div>

      <button type="submit" class="btn-submit">
        {{ submitLabel || 'Save' }}
      </button>
    </template>
  </form>
</template>

<style scoped>
.payment-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.loading {
  color: var(--color-text-secondary);
  padding: 2rem;
  text-align: center;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-group label {
  color: var(--color-text-secondary);
  font-size: 0.85rem;
}

.form-group input[type="number"],
.form-group input[type="date"],
.form-group input[type="text"],
.form-group textarea,
.form-group select {
  width: 100%;
  background: #111;
  border: 1px solid #333;
  border-radius: var(--radius-md);
  padding: 0.6rem 0.9rem;
  color: var(--color-text-primary);
  font-size: 0.95rem;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--color-positive);
}

.hint {
  font-size: 0.78rem;
  color: var(--color-text-secondary);
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: var(--color-text-primary);
}

.checkbox-group input[type="checkbox"] {
  accent-color: var(--color-positive);
}

.btn-submit {
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
</style>
