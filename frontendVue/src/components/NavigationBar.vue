<!--
  Persistent top-navigation bar – ported from React NavigationBar.tsx.

  Displays app title, quick filters (All/Expenses/Incomes/Fees),
  category/statistics links, CSV import/export, ADD button, and logout.
-->
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { paymentApi } from '@/api/payment.api'
import type { ViewFilter, PaymentItem } from '@/types/models'
import { TOKEN_KEY } from '@/utils/constants'

const props = defineProps<{
  active: ViewFilter
}>()

const emit = defineEmits<{
  change: [filter: ViewFilter]
  menu: []
  add: []
  logout: []
}>()

const router = useRouter()
const fileInputRef = ref<HTMLInputElement | null>(null)
const isImporting = ref(false)

const CSV_HEADER_COLUMNS = [
  'amount', 'date', 'description', 'Recipient name',
  'Recipient address', 'standard_category name', 'periodic',
]

function escapeCsvField(rawValue: string): string {
  const normalized = rawValue.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const shouldQuote = /[";\n]/.test(normalized)
  let escaped = normalized.replace(/"/g, '""')
  if (shouldQuote) escaped = `"${escaped}"`
  return escaped
}

function getAuthHeaders(): Record<string, string> {
  const token = localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY)
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function handleExportCSV() {
  try {
    const response = await fetch('/api/payment-items', { headers: getAuthHeaders() })
    if (!response.ok) throw new Error('Failed to fetch payment items')
    const paymentItems: PaymentItem[] = await response.json()

    const sortedItems = paymentItems.sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    )

    const csvRows = await Promise.all(
      sortedItems.map(async (item) => {
        const amount = item.amount.toString()
        const date = new Date(item.date).toISOString().split('T')[0]
        const description = item.description || ''
        const periodic = item.periodic ? 'true' : 'false'

        let recipientName = ''
        let recipientAddress = ''
        if (item.recipientId) {
          try {
            const res = await fetch(`/api/recipients/${item.recipientId}`, { headers: getAuthHeaders() })
            if (res.ok) {
              const r = await res.json()
              recipientName = r.name || ''
              recipientAddress = r.address || ''
            }
          } catch { /* skip */ }
        }

        let standardCategoryName = ''
        if (item.standardCategoryId) {
          try {
            const res = await fetch(`/api/categories/${item.standardCategoryId}`, { headers: getAuthHeaders() })
            if (res.ok) {
              const c = await res.json()
              standardCategoryName = c.name || ''
            }
          } catch { /* skip */ }
        }

        return [amount, date, description, recipientName, recipientAddress, standardCategoryName, periodic]
          .map((v) => escapeCsvField(v ?? ''))
          .join(';')
      })
    )

    const csvContent = [CSV_HEADER_COLUMNS.join(';'), ...csvRows].join('\r\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `payment_items_${new Date().toISOString().split('T')[0]}.csv`
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  } catch (error) {
    console.error('Error exporting CSV:', error)
    alert('Failed to export CSV. Please try again.')
  }
}

function handleImportCSVClick() {
  fileInputRef.value?.click()
}

async function handleImportFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.name.toLowerCase().endsWith('.csv')) {
    alert('Please select a CSV file exported from FinanceBook.')
    target.value = ''
    return
  }

  isImporting.value = true
  try {
    const result = await paymentApi.importCsv(file)
    alert(
      `CSV import completed. Created ${result.created_payments} payments, ` +
      `${result.created_recipients} recipients, updated ${result.updated_recipients} recipients, ` +
      `and created ${result.created_categories} categories.`
    )
  } catch (error: unknown) {
    console.error('Error importing CSV:', error)
    alert('Failed to import CSV. Please ensure the file matches the exported format.')
  } finally {
    isImporting.value = false
    target.value = ''
  }
}
</script>

<template>
  <header class="bar">
    <div class="left-section">
      <h1 class="title">FinanceBook</h1>
      <div class="category-edit-buttons">
        <button @click="router.push('/categories')">Categories</button>
        <button @click="router.push('/category-types')">Category Types</button>
        <button @click="router.push('/statistics')">Statistics</button>
      </div>
    </div>

    <nav class="filters">
      <button :class="{ active: active === 'all' }" @click="emit('change', 'all')">All</button>
      <button :class="{ active: active === 'expenses' }" @click="emit('change', 'expenses')">Expenses</button>
      <button :class="{ active: active === 'incomes' }" @click="emit('change', 'incomes')">Incomes</button>
      <button :class="{ active: active === 'fees' }" @click="emit('change', 'fees')">Fees</button>
    </nav>

    <div class="right-section">
      <div class="csv-buttons">
        <input ref="fileInputRef" type="file" accept=".csv,text/csv" style="display: none" @change="handleImportFileChange" />
        <button type="button" :disabled="isImporting" @click="handleImportCSVClick">
          {{ isImporting ? 'importing…' : 'import CSV' }}
        </button>
        <button type="button" @click="handleExportCSV">export CSV</button>
      </div>
      <button class="add-button" @click="emit('add')">ADD</button>
      <button class="menu-button" aria-label="Open Menu" @click="emit('menu')">☰</button>
      <button class="logout-button" @click="emit('logout')">Logout</button>
    </div>
  </header>
</template>

<style scoped>
.bar {
  background: var(--color-surface);
  padding: var(--spacing-sm) var(--spacing-md);
  display: flex;
  align-items: center;
  border-bottom: 1px solid #272727;
  position: relative;
}

.left-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.filters {
  display: flex;
  gap: var(--spacing-sm);
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.filters button {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  cursor: pointer;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
}

.filters button.active {
  background: #333;
  color: var(--color-text-primary);
}

@media (max-width: 480px) {
  .filters { display: none; }
}

.category-edit-buttons {
  display: flex;
  gap: var(--spacing-sm);
}

.category-edit-buttons button {
  background: none;
  border: 1px solid #444;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  cursor: pointer;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.category-edit-buttons button:hover {
  background: #333;
  color: var(--color-text-primary);
  border-color: #555;
}

@media (max-width: 768px) {
  .category-edit-buttons { display: none; }
}

.right-section {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: var(--spacing-sm);
}

.add-button {
  background: var(--color-positive);
  color: white;
  border: none;
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.add-button:hover { background: #059669; }

.menu-button {
  display: none;
  background: transparent;
  border: none;
  padding: var(--spacing-xs);
  cursor: pointer;
  color: var(--color-text-primary);
  font-size: 1.2rem;
}

@media (max-width: 480px) {
  .menu-button { display: block; }
}

.logout-button {
  background: #2563eb;
  color: white;
  border: none;
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.logout-button:hover { background: #1d4ed8; }

.csv-buttons {
  display: flex;
  gap: var(--spacing-sm);
}

.csv-buttons button {
  background: none;
  border: 1px solid #444;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  cursor: pointer;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.csv-buttons button:hover {
  background: #333;
  color: var(--color-text-primary);
  border-color: #555;
}

.csv-buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .csv-buttons { display: none; }
  .add-button { padding: var(--spacing-xs) var(--spacing-sm); font-size: 0.8rem; }
  .logout-button { padding: var(--spacing-xs) var(--spacing-sm); font-size: 0.8rem; }
}
</style>
