/**
 * Payment Items API service.
 */

import api from './client'
import type { PaymentItem, PaymentItemFormData } from '@/types/models'

function buildPaymentQuery(params: {
  expenseOnly?: boolean
  incomeOnly?: boolean
  categoryIds?: number[]
}): string {
  const usp = new URLSearchParams()
  if (params.expenseOnly) usp.set('expense_only', 'true')
  if (params.incomeOnly) usp.set('income_only', 'true')
  if (params.categoryIds && params.categoryIds.length > 0) {
    params.categoryIds.forEach((id) => usp.append('category_ids', id.toString()))
  }
  return usp.toString() ? `?${usp.toString()}` : ''
}

export const paymentApi = {
  /** Fetch all payment items with optional filters. */
  async getAll(opts?: {
    expenseOnly?: boolean
    incomeOnly?: boolean
    categoryIds?: number[]
  }): Promise<PaymentItem[]> {
    const qs = buildPaymentQuery(opts ?? {})
    const { data } = await api.get<PaymentItem[]>(`/payment-items${qs}`)
    return data
  },

  /** Fetch a single payment item by ID. */
  async getById(id: number): Promise<PaymentItem> {
    const { data } = await api.get<PaymentItem>(`/payment-items/${id}`)
    return data
  },

  /** Create a new payment item. */
  async create(item: PaymentItemFormData): Promise<PaymentItem> {
    const { data } = await api.post<PaymentItem>('/payment-items', item)
    return data
  },

  /** Update an existing payment item. */
  async update(id: number, item: Partial<PaymentItemFormData>): Promise<PaymentItem> {
    const { data } = await api.put<PaymentItem>(`/payment-items/${id}`, item)
    return data
  },

  /** Delete a payment item. */
  async delete(id: number): Promise<void> {
    await api.delete(`/payment-items/${id}`)
  },

  /** Import CSV file. */
  async importCsv(file: File): Promise<{ created_payments: number; created_recipients: number; updated_recipients: number; created_categories: number }> {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post('/payment-items/import-csv', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },
}
