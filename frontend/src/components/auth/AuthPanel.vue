<template>
  <main class="auth-shell">
    <section class="auth-card">
      <div class="auth-brand">
        <span>Interview Desk</span>
        <h1>进入面试准备台</h1>
        <p>登录后，你的简历版本、训练计划、错题和历史报告都会沉淀到自己的求职档案里。</p>
      </div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <div class="auth-tabs">
          <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>

        <label>
          <span>账号</span>
          <input v-model.trim="form.username" autocomplete="username" placeholder="4-30 位字母、数字或下划线">
        </label>

        <label v-if="mode === 'register'">
          <span>昵称</span>
          <input v-model.trim="form.displayName" autocomplete="name" placeholder="例如：张同学">
        </label>

        <label>
          <span>密码</span>
          <div class="password-field">
            <input
              v-model="form.password"
              :type="passwordVisible ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="至少 6 位"
            >
            <button
              type="button"
              :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
              @click="passwordVisible = !passwordVisible"
            >
              <span v-if="passwordVisible">隐藏</span>
              <span v-else>查看</span>
            </button>
          </div>
        </label>

        <button type="submit" :disabled="submitting">
          {{ submitting ? '处理中' : mode === 'login' ? '登录' : '创建账号' }}
        </button>

        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
      </form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { login, register } from '../../api/auth'
import { saveAuth } from '../../api/http'

const emit = defineEmits(['authenticated'])

const mode = ref('login')
const passwordVisible = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: '',
  password: '',
  displayName: ''
})

async function handleSubmit() {
  errorMessage.value = ''
  if (!form.username || !form.password || (mode.value === 'register' && !form.displayName)) {
    errorMessage.value = '请把账号、密码和必要信息填写完整'
    return
  }
  submitting.value = true
  try {
    const response = mode.value === 'login'
      ? await login({ username: form.username, password: form.password })
      : await register({ username: form.username, password: form.password, displayName: form.displayName })
    saveAuth(response.data.token, response.data.user)
    emit('authenticated', response.data.user)
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error.message || '认证失败'
  } finally {
    submitting.value = false
  }
}
</script>
