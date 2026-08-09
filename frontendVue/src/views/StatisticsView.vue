<!--
  Statistics page – ported from React StatisticsPage.tsx.
  Displays charts for income/expense analysis using Chart.js.
-->
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { paymentApi } from '@/api/payment.api'
import type { PaymentItem } from '@/types/models'
import { format, parseISO } from 'date-fns'

const router = useRouter()
const items = ref<PaymentItem[]>([])
const isLoading = ref(true)

onMounted(async () => {
  try {
    items.value = await paymentApi.getAll()
  } catch (e) {
    console.error('Failed to load items for statistics:', e)
  } finally {
    isLoading.value = false
  }
})

// Monthly aggregation
const monthlyData = computed(() => {
  const byMonth: Record<string, { income: number; expense: number }> = {}

  for (const item of items.value) {
    const month = format(parseISO(item.date), 'yyyy-MM')
    if (!byMonth[month]) byMonth[month] = { income: 0, expense: 0 }
    if (item.amount >= 0) {
      byMonth[month].income += item.amount
    } else {
      byMonth[month].expense += Math.abs(item.amount)
    }
  }

  return Object.entries(byMonth)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([month, data]) => ({ month, ...data }))
})

const totalIncome = computed(() =>
  items.value.filter((i) => i.amount >= 0).reduce((s, i) => s + i.amount, 0)
)

const totalExpenses = computed(() =>
  items.value.filter((i) => i.amount < 0).reduce((s, i) => s + Math.abs(i.amount), 0)
)

const totalItems = computed(() => items.value.length)

// Simple bar chart rendering via CSS (no Chart.js dependency needed for basic view)
function barWidth(value: number, max: number): string {
  if (max === 0) return '0%'
  return `${Math.min((value / max) * 100, 100)}%`
}

const maxMonthly = computed(() => {
  let max = 0
  for (const d of monthlyData.value) {
    max = Math.max(max, d.income, d.expense)
  }
  return max
})
</script>

<template>
  <div class="stats-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← Back</button>
      <h2>Statistics</h2>
    </div>

    <div v-if="isLoading" class="status">Loading statistics…</div>

    <template v-else>
      <!-- Summary cards -->
      <div class="summary-cards">
        <div class="card">
          <span class="card-label">Total Items</span>
          <span class="card-value">{{ totalItems }}</span>
        </div>
        <div class="card">
          <span class="card-label">Total Income</span>
          <span class="card-value income">+{{ totalIncome.toFixed(2) }}</span>
        </div>
        <div class="card">
          <span class="card-label">Total Expenses</span>
          <span class="card-value expense">-{{ totalExpenses.toFixed(2) }}</span>
        </div>
        <div class="card">
          <span class="card-label">Net Balance</span>
          <span
            class="card-value"
            :class="{ income: totalIncome - totalExpenses >= 0, expense: totalIncome - totalExpenses < 0 }"
          >
            {{ (totalIncome - totalExpenses).toFixed(2) }}
          </span>
        </div>
      </div>

      <!-- Monthly breakdown -->
      <h3>Monthly Breakdown</h3>
      <div v-if="monthlyData.length === 0" class="status">No data available.</div>

      <div v-else class="monthly-chart">
        <div v-for="d in monthlyData" :key="d.month" class="month-row">
          <span class="month-label">{{ d.month }}</span>
          <div class="bars">
            <div class="bar-container">
              <div class="bar income-bar" :style="{ width: barWidth(d.income, maxMonthly) }">
                <span v-if="d.income > 0">+{{ d.income.toFixed(0) }}</span>
              </div>
            </div>
            <div class="bar-container">
              <div class="bar expense-bar" :style="{ width: barWidth(d.expense, maxMonthly) }">
                <span v-if="d.expense > 0">-{{ d.expense.toFixed(0) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.stats-page {
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
}

.status {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 2rem;
}

/* ─── Summary cards ─── */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
}

.card {
  background: var(--color-surface);
  border: 1px solid #272727;
  border-radius: var(--radius-md);
  padding: 1rem;
  text-align: center;
}

.card-label {
  display: block;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.25rem;
}

.card-value {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.card-value.income { color: var(--color-positive); }
.card-value.expense { color: var(--color-negative); }

/* ─── Monthly chart ─── */
h3 {
  color: var(--color-text-primary);
  font-size: 1.1rem;
}

.monthly-chart {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.month-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.month-label {
  width: 70px;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  text-align: right;
  flex-shrink: 0;
}

.bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.bar-container {
  height: 18px;
  background: #1a1a1a;
  border-radius: 3px;
  overflow: hidden;
}

.bar {
  height: 100%;
  border-radius: 3px;
  display: flex;
  align-items: center;
  padding-left: 6px;
  font-size: 0.7rem;
  color: white;
  font-weight: 600;
  transition: width 0.3s ease;
  min-width: fit-content;
}

.income-bar {
  background: var(--color-positive);
}

.expense-bar {
  background: var(--color-negative);
}
</style>
