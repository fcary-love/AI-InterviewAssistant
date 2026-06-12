<template>
  <header class="topnav">
    <div class="topnav-inner">
      <router-link to="/" class="topnav-brand">
        <div class="topnav-logo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5z" fill="#1e40af" opacity="0.9"/>
            <path d="M2 17l10 5 10-5" stroke="#1e40af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <path d="M2 12l10 5 10-5" stroke="#1e40af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
        </div>
        <span class="topnav-title">FaceAI</span>
      </router-link>

      <nav class="topnav-links">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="topnav-link"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="isActive(item.path)" class="nav-dot"></span>
        </router-link>
      </nav>

      <div class="topnav-right">
        <LevelBadge
          v-if="levelInfo"
          :level="levelInfo.level"
          :title="levelInfo.title"
          :progress="levelInfo.progress"
          :needed="levelInfo.needed"
          compact
        />
        <div class="topnav-status" v-if="resumeReady">
          <span class="status-dot"></span>
          <span class="status-text">就绪</span>
        </div>
        <router-link to="/profile" class="topnav-user">
          <span class="topnav-avatar">{{ initial }}</span>
          <span class="topnav-name">{{ displayName }}</span>
        </router-link>
        <button class="topnav-logout" @click="$emit('logout')">退出</button>
      </div>
    </div>
  </header>

  <nav class="bottom-nav" aria-label="移动端导航">
    <router-link
      v-for="item in navItems"
      :key="item.path"
      :to="item.path"
      class="bottom-nav-item"
      :class="{ active: isActive(item.path) }"
    >
      <span class="bottom-nav-icon" v-html="item.icon"></span>
      <span class="bottom-nav-label">{{ item.shortLabel || item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useInterviewDesk } from '../../composables/useInterviewDesk'
import LevelBadge from '../gamification/LevelBadge.vue'

const props = defineProps({
  user: { type: Object, default: null }
})

defineEmits(['logout'])

const route = useRoute()
const desk = useInterviewDesk()

const resumeReady = computed(() => !!desk.pdfData.value)
const levelInfo = computed(() => desk.levelInfo?.value || null)

onMounted(() => {
  desk.init?.()
})

const navItems = [
  { path: '/', label: '工作台', shortLabel: '首页', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/></svg>' },
  { path: '/coach', label: '求职助手', shortLabel: '助手', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/></svg>' },
  { path: '/interview', label: '模拟面试', shortLabel: '面试', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/></svg>' },
  { path: '/resources', label: '资料管理', shortLabel: '资料', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z"/></svg>' },
  { path: '/skills', label: '技能图谱', shortLabel: '技能', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z"/></svg>' },
  { path: '/reports', label: '成长', shortLabel: '成长', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/></svg>' },
  { path: '/agent', label: '教练', shortLabel: '教练', icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/></svg>' }
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const initial = computed(() => {
  const name = props.user?.displayName || props.user?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

const displayName = computed(() => {
  return props.user?.displayName || props.user?.username || '用户'
})
</script>
