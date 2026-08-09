/**
 * Domain TypeScript definitions shared across the Vue.js front-end.
 *
 * These interfaces are kept in sync with the Java Spring Boot backend DTOs.
 * Field names use camelCase to match the JSON serialization from Jackson.
 */

// ─── User ──────────────────────────────────────────────────────────

export interface UserRead {
  id: number
  username: string
  surname: string
  prename: string
  birthDate?: string | null
  phone?: string | null
  road?: string | null
  houseNumber?: string | null
  region?: string | null
  postal?: string | null
  city?: string | null
  state?: string | null
  isAdmin: boolean
  isActive: boolean
  createdAt: string
}

// ─── Recipient ─────────────────────────────────────────────────────

export interface Recipient {
  id: number
  name: string
  address?: string | null
}

// ─── Category ──────────────────────────────────────────────────────

export interface Category {
  id: number
  name: string
  typeId: number
  parentId?: number | null
  iconFile?: string | null
  children?: Category[]
}

export interface CategoryType {
  id: number
  name: string
  description?: string | null
}

// ─── Payment Item ──────────────────────────────────────────────────

export interface PaymentItem {
  id: number
  amount: number
  date: string
  periodic: boolean
  description?: string | null

  recipientId?: number | null
  recipient?: Recipient | null

  categories?: Category[]
  standardCategoryId?: number | null
  standardCategory?: Category | null
  invoicePath?: string | null
  transactionFee?: number | null
}

/** Helper to distinguish incomes and expenses on the client. */
export const isExpense = (item: PaymentItem): boolean => item.amount < 0
export const isIncome = (item: PaymentItem): boolean => item.amount >= 0

// ─── Form Data ─────────────────────────────────────────────────────

export interface PaymentItemFormData {
  amount: number
  date: string
  periodic: boolean
  description?: string | null
  recipientId?: number | null
  categoryIds?: number[]
  standardCategoryId?: number | null
}

export interface SummaryTotals {
  totalIncome: number
  totalExpenses: number
  net: number
}

// ─── View Filter ───────────────────────────────────────────────────

export type ViewFilter = 'all' | 'expenses' | 'incomes' | 'fees'
