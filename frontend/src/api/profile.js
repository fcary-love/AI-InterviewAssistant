import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 30000
})

export async function fetchProfileOverview() {
  const { data } = await request.get('/api/profile/overview')
  return data
}

export async function fetchResumeVersions() {
  const { data } = await request.get('/api/profile/resumes')
  return data
}

export async function fetchResumeVersionDetail(id) {
  const { data } = await request.get(`/api/profile/resumes/${id}`)
  return data
}

export async function compareResumeVersions(leftId, rightId) {
  const { data } = await request.get('/api/profile/resumes/compare', {
    params: {
      leftId,
      rightId
    }
  })
  return data
}
