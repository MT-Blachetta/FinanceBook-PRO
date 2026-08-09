/**
 * Authentication API service.
 */

import api from './client'
import type { UserRead } from '@/types/models'

export interface LoginRequest {
  username: string
  password: string
}

export interface JwtResponse {
  accessToken: string
  tokenType: string
}

export const authApi = {
  /** Authenticate with username & password, returns JWT. */
  async login(credentials: LoginRequest): Promise<JwtResponse> {
    const { data } = await api.post<JwtResponse>('/auth/login', credentials)
    return data
  },

  /** Fetch the current user's profile using the stored JWT. */
  async getCurrentUser(): Promise<UserRead> {
    const { data } = await api.get<UserRead>('/auth/me')
    return data
  },

  /** Register a new user account. */
  async register(userData: {
    username: string
    password: string
    surname: string
    prename: string
  }): Promise<void> {
    await api.post('/auth/register', userData)
  },
}
