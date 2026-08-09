/**
 * Category and CategoryType API services.
 */

import api from './client'
import type { Category, CategoryType } from '@/types/models'

export const categoryApi = {
  /** Fetch all categories (flat or as tree). */
  async getAll(): Promise<Category[]> {
    const { data } = await api.get<Category[]>('/categories')
    return data
  },

  /** Fetch categories as a tree. */
  async getTree(): Promise<Category[]> {
    const { data } = await api.get<Category[]>('/categories/tree')
    return data
  },

  /** Fetch a single category by ID. */
  async getById(id: number): Promise<Category> {
    const { data } = await api.get<Category>(`/categories/${id}`)
    return data
  },

  /** Create a new category. */
  async create(category: { name: string; typeId: number; parentId?: number | null }): Promise<Category> {
    const { data } = await api.post<Category>('/categories', category)
    return data
  },

  /** Update a category. */
  async update(id: number, category: { name?: string; parentId?: number | null }): Promise<Category> {
    const { data } = await api.put<Category>(`/categories/${id}`, category)
    return data
  },

  /** Delete a category. */
  async delete(id: number): Promise<void> {
    await api.delete(`/categories/${id}`)
  },

  /** Upload an icon for a category. */
  async uploadIcon(id: number, file: File): Promise<Category> {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post<Category>(`/categories/${id}/icon`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },
}

export const categoryTypeApi = {
  /** Fetch all category types. */
  async getAll(): Promise<CategoryType[]> {
    const { data } = await api.get<CategoryType[]>('/category-types')
    return data
  },

  /** Create a new category type. */
  async create(categoryType: { name: string; description?: string }): Promise<CategoryType> {
    const { data } = await api.post<CategoryType>('/category-types', categoryType)
    return data
  },

  /** Delete a category type. */
  async delete(id: number): Promise<void> {
    await api.delete(`/category-types/${id}`)
  },
}
