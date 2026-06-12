import { createRequest } from './http'

const request = createRequest({
  timeout: 30000
})

export async function login(payload) {
  const { data } = await request.post('/api/auth/login', payload)
  return data
}

export async function register(payload) {
  const { data } = await request.post('/api/auth/register', payload)
  return data
}

export async function fetchCurrentUser() {
  const { data } = await request.get('/api/auth/me')
  return data
}
