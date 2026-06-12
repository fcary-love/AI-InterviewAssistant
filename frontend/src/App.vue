<template>
  <AuthPanel v-if="!ready || !currentUser" @authenticated="handleAuthenticated" />
  <AppLayout v-else :user="currentUser" @logout="handleLogout" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AuthPanel from './components/auth/AuthPanel.vue'
import AppLayout from './components/layout/AppLayout.vue'
import { fetchCurrentUser } from './api/auth'
import { clearAuth, getStoredUser, getToken } from './api/http'

const ready = ref(false)
const currentUser = ref(getStoredUser())

onMounted(async () => {
  window.addEventListener('auth-expired', handleAuthExpired)
  if (!getToken()) {
    currentUser.value = null
    ready.value = true
    return
  }
  try {
    const response = await fetchCurrentUser()
    currentUser.value = response.data
  } catch {
    clearAuth()
    currentUser.value = null
  } finally {
    ready.value = true
  }
})

function handleAuthenticated(user) {
  currentUser.value = user
  ready.value = true
}

function handleLogout() {
  clearAuth()
  currentUser.value = null
  ElMessage.success('已退出登录')
}

function handleAuthExpired() {
  currentUser.value = null
  ElMessage.warning('登录已过期，请重新登录')
}
</script>
