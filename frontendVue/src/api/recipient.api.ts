/**
 * Recipient API service.
 */

import api from './client'
import type { Recipient } from '@/types/models'

export const recipientApi = {
  /** Fetch all recipients. */
  async getAll(): Promise<Recipient[]> {
    const { data } = await api.get<Recipient[]>('/recipients')
    return data
  },

  /** Fetch a single recipient by ID. */
  async getById(id: number): Promise<Recipient> {
    const { data } = await api.get<Recipient>(`/recipients/${id}`)
    return data
  },

  /** Create a new recipient. */
  async create(recipient: { name: string; address?: string }): Promise<Recipient> {
    const { data } = await api.post<Recipient>('/recipients', recipient)
    return data
  },

  /** Update a recipient. */
  async update(id: number, recipient: { name?: string; address?: string }): Promise<Recipient> {
    const { data } = await api.put<Recipient>(`/recipients/${id}`, recipient)
    return data
  },

  /** Delete a recipient. */
  async delete(id: number): Promise<void> {
    await api.delete(`/recipients/${id}`)
  },
}
