import axios from 'axios'

const TOKEN_KEY = 'face_ai_token'
const USER_KEY = 'face_ai_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function saveAuth(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function createRequest(options = {}) {
  const request = axios.create({
    baseURL: '/',
    timeout: 30000,
    ...options
  })

  request.interceptors.request.use((config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  request.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error?.response?.status === 401) {
        clearAuth()
        window.dispatchEvent(new CustomEvent('auth-expired'))
      }
      return Promise.reject(error)
    }
  )

  return request
}
