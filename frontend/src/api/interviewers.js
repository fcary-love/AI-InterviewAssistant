import { createRequest } from './http'

const request = createRequest({ timeout: 15000 })

export function fetchInterviewers() {
  return request.get('/api/interviewers')
}

export function fetchInterviewer(id) {
  return request.get(`/api/interviewers/${id}`)
}

export function fetchDefaultInterviewer() {
  return request.get('/api/interviewers/default')
}
