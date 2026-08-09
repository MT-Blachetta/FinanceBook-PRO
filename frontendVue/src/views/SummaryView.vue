<!--
  Summary / payment list page – ported from React SummaryPage.tsx.
  Displays a list of payment items with filtering support.
-->
<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { paymentApi } from '@/api/payment.api'
import ConfirmationDialog from '@/components/ConfirmationDialog.vue'
import type { PaymentItem, ViewFilter } from '@/types/models'
import { format, parseISO } from 'date-fns'

const route = useRoute()
const router = useRouter()

const items = ref<PaymentItem[]>([])
const isLoading = ref(true)
const error = ref('')
const deleteTarget = ref<PaymentItem | null>(null)

const currentFilter = computed<ViewFilter>(() => {
  return (route.query.filter as ViewFilter) || 'all'
})

async function loadItems() {
  isLoading.value = true
  error.value = ''
  try {
    const opts: { expenseOnly?: boolean; incomeOnly?: boolean } = {}
    if (currentFilter.value === 'expenses') opts.expenseOnly = true
    if (currentFilter.value === 'incomes') opts.incomeOnly = true
    items.value = await paymentApi.getAll(opts)
  } catch (e) {
    error.value = 'Failed to load payment items.'
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadItems)
watch(currentFilter, loadItems)

// Summary totals
const totalIncome = computed(() =>
  items.value.filter((i) => i.amount >= 0).reduce((sum, i) => sum + i.amount, 0)
)
const totalExpenses = computed(() =>
  items.value.filter((i) => i.amount < 0).reduce((sum, i) => sum + i.amount, 0)
)
const net = computed(() => totalIncome.value + totalExpenses.value)

function formatDate(dateStr: string): string {
  try {
    return format(parseISO(dateStr), 'yyyy-MM-dd')
  } catch {
    return dateStr
  }
}

function formatAmount(amount: number): string {
  const prefix = amount >= 0 ? '+' : ''
  return `${prefix}${amount.toFixed(2)}`
}

function handleEdit(item: PaymentItem) {
  router.push(`/payment/${item.id}/edit`)
}

function handleDeleteClick(item: PaymentItem) {
  deleteTarget.value = item
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await paymentApi.delete(deleteTarget.value.id)
    items.value = items.value.filter((i) => i.id !== deleteTarget.value!.id)
  } catch (e) {
    console.error('Delete failed:', e)
    alert('Failed to delete payment item.')
  } finally {
    deleteTarget.value = null
  }
}
</script>

<template>
  <div class="summary-page">
    <!-- Totals bar -->
    <div class="totals">
      <div class="total-item income">
        <span class="label">Income</span>
        <span class="value">+{{ totalIncome.toFixed(2) }}</span>
      </div>
      <div class="total-item expense">
        <span class="label">Expenses</span>
        <span class="value">{{ totalExpenses.toFixed(2) }}</span>
      </div>
      <div class="total-item net">
        <span class="label">Net</span>
        <span class="value" :class="{ positive: net >= 0, negative: net < 0 }">
          {{ formatAmount(net) }}
        </span>
      </div>
    </div>

    <!-- Loading / Error -->
    <div v-if="isLoading" class="status">Loading…</div>
    <div v-else-if="error" class="status error">{{ error }}</div>

    <!-- Payment list -->
    <div v-else-if="items.length === 0" class="status">
      No payment items found.
    </div>

    <ul v-else class="payment-list">
      <li v-for="item in items" :key="item.id" class="payment-item">
        <div class="item-left">
          <span v-if="item.periodic" class="periodic-badge" title="Periodic">⟳</span>
          <div class="item-info">
            <span class="item-description">
              {{ item.description || '(no description)' }}
            </span>
            <span class="item-date">{{ formatDate(item.date) }}</span>
            <span v-if="item.recipient" class="item-recipient">
              {{ item.recipient.name }}
            </span>
            <span v-if="item.standardCategory" class="item-category">
              {{ item.standardCategory.name }}
            </span>
          </div>
        </div>
        <div class="item-right">
          <span
            class="item-amount"
            :class="{ positive: item.amount >= 0, negative: item.amount < 0 }"
          >
            {{ formatAmount(item.amount) }}
          </span>
          <div class="item-actions">
            <button class="btn-edit" @click="handleEdit(item)">Edit</button>
            <button class="btn-delete" @click="handleDeleteClick(item)">×</button>
          </div>
        </div>
      </li>
    </ul>

    <!-- Delete confirmation -->
    <ConfirmationDialog
      :open="deleteTarget !== null"
      title="Delete Payment"
      :message="`Delete payment of ${deleteTarget ? formatAmount(deleteTarget.amount) : ''}?`"
      confirm-label="Delete"
      @confirm="confirmDelete"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<style scoped>
.summary-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* ─── Totals ─── */
.totals {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid #272727;
}

.total-item {
  flex: 1;
  text-align: center;
}

.total-item .label {
  display: block;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.25rem;
}

.total-item .value {
  font-size: 1.2rem;
  font-weight: 600;
}

.income .value { color: var(--color-positive); }
.expense .value { color: var(--color-negative); }
.positive { color: var(--color-positive); }
.negative { color: var(--color-negative); }

/* ─── Status ─── */
.status {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 2rem;
}

.status.error { color: var(--color-negative); }

/* ─── Payment list ─── */
.payment-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.payment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid #272727;
  transition: border-color 0.2s;
}

.payment-item:hover {
  border-color: #444;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
  flex: 1;
}

.periodic-badge {
  font-size: 1.2rem;
  color: var(--color-text-secondary);
}

.item-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.item-description {
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-date {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.item-recipient,
.item-category {
  font-size: 0.78rem;
  color: var(--color-text-secondary);
}

.item-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.item-amount {
  font-weight: 600;
  font-size: 1rem;
  white-space: nowrap;
}

.item-actions {
  display: flex;
  gap: 0.25rem;
}

.btn-edit {
  background: #333;
  color: var(--color-text-primary);
  border: 1px solid #444;
  padding: 0.25rem 0.6rem;
  border-radius: var(--radius-md);
  font-size: 0.8rem;
  cursor: pointer;
}

.btn-delete {
  background: var(--color-negative);
  color: white;
  border: none;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  cursor: pointer;
  line-height: 1;
}
</style>
