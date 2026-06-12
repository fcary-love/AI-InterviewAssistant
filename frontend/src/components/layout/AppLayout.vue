<template>
  <div class="app-layout">
    <!-- Top Navigation Bar -->
    <SideNav
      :user="user"
      :level-info="levelInfo"
      @logout="$emit('logout')"
    />

    <!-- Main Content -->
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- FlowDock (desktop only) -->
    <FlowDock v-if="!isMobile" />

    <!-- Mobile bottom nav -->
    <nav v-if="isMobile" class="bottom-nav" aria-label="移动端导航">
      <router-link
        v-for="item in mobileNavItems"
        :key="item.path"
        :to="item.path"
        class="bottom-nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="bottom-nav-icon" v-html="item.icon"></span>
        <span class="bottom-nav-label">{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useInterviewDesk } from '../../composables/useInterviewDesk'
import FlowDock from './FlowDock.vue'
import SideNav from './SideNav.vue'

const props = defineProps({
  user: { type: Object, default: null }
})

defineEmits(['logout'])

const desk = useInterviewDesk()
const route = useRoute()
const isMobile = ref(false)

const levelInfo = computed(() => desk.levelInfo?.value || null)

const mobileNavItems = [
  { path: '/', label: '工作台', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>' },
  { path: '/coach', label: '助手', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' },
  { path: '/interview', label: '面试', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>' },
  { path: '/reports', label: '成长', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' },
  { path: '/profile', label: '我的', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4.4 3.6-8 8-8s8 3.6 8 8"/></svg>' }
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  desk.init?.()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>
