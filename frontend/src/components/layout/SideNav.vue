<template>
  <header class="topbar" aria-label="主导航">
    <div class="topbar-inner">
      <!-- Brand -->
      <router-link to="/" class="topbar-brand">
        <div class="topbar-logo">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5z" fill="currentColor" opacity="0.9"/>
            <path d="M2 17l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <path d="M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
        </div>
        <span class="topbar-title">FaceAI</span>
      </router-link>

      <!-- Primary Navigation -->
      <nav class="topbar-nav">
        <router-link
          v-for="item in primaryNav"
          :key="item.path"
          :to="item.path"
          class="topbar-link"
          :class="{ active: isActive(item.path) }"
        >
          <span class="topbar-link-icon" v-html="item.icon"></span>
          <span class="topbar-link-label">{{ item.label }}</span>
          <span v-if="isActive(item.path)" class="topbar-link-glow"></span>
        </router-link>

        <!-- Tools Dropdown -->
        <div
          class="topbar-link tools-trigger"
          :class="{ active: toolsActive }"
          @click="toolsOpen = !toolsOpen"
          @mouseenter="toolsOpen = true"
          @mouseleave="toolsOpen = false"
        >
          <span class="topbar-link-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/>
            </svg>
          </span>
          <span class="topbar-link-label">工具</span>
          <svg class="tools-chevron" :class="{ open: toolsOpen }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"/>
          </svg>

          <!-- Dropdown -->
          <Transition name="drop">
            <div v-if="toolsOpen" class="tools-dropdown" @click.stop>
              <router-link
                v-for="item in toolNav"
                :key="item.path"
                :to="item.path"
                class="tools-dropdown-item"
                :class="{ active: isActive(item.path) }"
              >
                <span class="tools-dropdown-icon" v-html="item.icon"></span>
                <div class="tools-dropdown-text">
                  <strong>{{ item.label }}</strong>
                  <span>{{ item.desc }}</span>
                </div>
              </router-link>
            </div>
          </Transition>
        </div>
      </nav>

      <!-- Right Section -->
      <div class="topbar-right">
        <!-- Level Badge -->
        <div class="topbar-level" v-if="levelInfo">
          <span class="level-pip"></span>
          <span class="level-text">Lv.{{ levelInfo.level }}</span>
        </div>

        <!-- User -->
        <router-link to="/profile" class="topbar-user" :class="{ active: isActive('/profile') }">
          <span class="topbar-avatar">{{ initial }}</span>
          <span class="topbar-name">{{ displayName }}</span>
        </router-link>

        <!-- Logout -->
        <button class="topbar-logout" @click="handleLogout" title="退出登录">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  user: { type: Object, default: null },
  mobileOpen: { type: Boolean, default: false },
  levelInfo: { type: Object, default: null }
})

const emit = defineEmits(['logout', 'close-mobile', 'toggle-collapse'])

const route = useRoute()
const toolsOpen = ref(false)

const primaryNav = [
  {
    path: '/',
    label: '工作台',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>'
  },
  {
    path: '/coach',
    label: '求职助手',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
  },
  {
    path: '/interview',
    label: '模拟面试',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>'
  }
]

const toolNav = [
  {
    path: '/resources',
    label: '资料管理',
    desc: '题库与简历档案',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>'
  },
  {
    path: '/skills',
    label: '技能图谱',
    desc: '能力雷达与评估',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>'
  },
  {
    path: '/reports',
    label: '成长看板',
    desc: '趋势复盘与洞察',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>'
  },
  {
    path: '/agent',
    label: 'AI 教练',
    desc: '智能分析与训练',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/><path d="M17 14h.01"/><path d="M21 12a9 9 0 0 0-9-9"/><path d="M3 12a9 9 0 0 1 9-9"/></svg>'
  }
]

const toolsActive = computed(() => toolNav.some(t => isActive(t.path)))

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleLogout() {
  emit('logout')
}

const initial = computed(() => {
  const name = props.user?.displayName || props.user?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

const displayName = computed(() => {
  return props.user?.displayName || props.user?.username || '用户'
})
</script>

<style scoped>
/* ---- Top Bar Shell ---- */

.topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 110;
  height: 56px;
  background: rgba(13, 13, 26, 0.82);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.topbar::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(45, 212, 191, 0.15), transparent);
  pointer-events: none;
}

.topbar-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
  gap: 8px;
  max-width: 1440px;
  margin: 0 auto;
}

/* ---- Brand ---- */

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin-right: 8px;
  text-decoration: none;
}

.topbar-logo {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, rgba(45, 212, 191, 0.18), rgba(45, 212, 191, 0.04));
  border: 1px solid rgba(45, 212, 191, 0.22);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  transition: transform 0.2s var(--ease-out), box-shadow 0.2s var(--ease-out);
}

.topbar-brand:hover .topbar-logo {
  transform: scale(1.06);
  box-shadow: 0 0 20px rgba(45, 212, 191, 0.15);
}

.topbar-title {
  font-family: 'Outfit', var(--font-body);
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

/* ---- Navigation ---- */

.topbar-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
}

.topbar-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 14px;
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.18s var(--ease-out), background 0.18s var(--ease-out);
  white-space: nowrap;
}

.topbar-link:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.04);
}

.topbar-link.active {
  color: var(--primary);
  background: rgba(45, 212, 191, 0.06);
}

.topbar-link-glow {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: var(--primary);
  border-radius: 1px;
  box-shadow: 0 0 10px rgba(45, 212, 191, 0.5), 0 0 20px rgba(45, 212, 191, 0.2);
}

.topbar-link-icon {
  display: flex;
  align-items: center;
  opacity: 0.6;
  transition: opacity 0.18s;
}

.topbar-link:hover .topbar-link-icon,
.topbar-link.active .topbar-link-icon {
  opacity: 1;
}

/* ---- Tools Trigger ---- */

.tools-trigger {
  cursor: pointer;
  user-select: none;
}

.tools-chevron {
  opacity: 0.4;
  transition: transform 0.2s var(--ease-out), opacity 0.18s;
}

.tools-chevron.open {
  transform: rotate(180deg);
  opacity: 0.7;
}

.tools-trigger:hover .tools-chevron {
  opacity: 0.7;
}

/* ---- Tools Dropdown ---- */

.tools-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  width: 260px;
  padding: 8px;
  background: rgba(24, 24, 43, 0.96);
  backdrop-filter: blur(28px) saturate(2);
  -webkit-backdrop-filter: blur(28px) saturate(2);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(0, 0, 0, 0.2);
  z-index: 120;
}

.tools-dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  text-decoration: none;
  color: var(--text-secondary);
  transition: all 0.15s var(--ease-out);
}

.tools-dropdown-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-primary);
}

.tools-dropdown-item.active {
  background: rgba(45, 212, 191, 0.06);
  color: var(--primary);
}

.tools-dropdown-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.55;
  transition: opacity 0.15s;
}

.tools-dropdown-item:hover .tools-dropdown-icon,
.tools-dropdown-item.active .tools-dropdown-icon {
  opacity: 1;
}

.tools-dropdown-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.tools-dropdown-text strong {
  font-size: 13.5px;
  font-weight: 600;
}

.tools-dropdown-text span {
  font-size: 11px;
  color: var(--text-muted);
}

/* ---- Dropdown Transition ---- */

.drop-enter-active {
  transition: opacity 0.18s var(--ease-out), transform 0.2s var(--ease-spring);
}

.drop-leave-active {
  transition: opacity 0.12s var(--ease-out), transform 0.12s var(--ease-out);
}

.drop-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-6px) scale(0.96);
}

.drop-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-4px) scale(0.97);
}

/* ---- Right Section ---- */

.topbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  margin-left: auto;
}

/* Level Badge */

.topbar-level {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: rgba(45, 212, 191, 0.06);
  border: 1px solid rgba(45, 212, 191, 0.14);
  border-radius: 20px;
}

.level-pip {
  width: 5px;
  height: 5px;
  background: var(--primary);
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(45, 212, 191, 0.5);
}

.level-text {
  font-size: 11px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: 0.02em;
}

/* User */

.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 22px;
  text-decoration: none;
  transition: background 0.15s;
}

.topbar-user:hover {
  background: rgba(255, 255, 255, 0.04);
}

.topbar-user.active {
  background: rgba(45, 212, 191, 0.06);
}

.topbar-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-deep), var(--primary));
  color: var(--bg-abyss);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.topbar-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

/* Logout */

.topbar-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s;
}

.topbar-logout:hover {
  color: var(--danger);
  background: rgba(251, 113, 133, 0.08);
  border-color: rgba(251, 113, 133, 0.15);
}

/* ---- Responsive ---- */

@media (max-width: 768px) {
  .topbar-inner {
    padding: 0 12px;
  }

  .topbar-title {
    display: none;
  }

  .topbar-link-label {
    display: none;
  }

  .topbar-link {
    padding: 0 10px;
    gap: 0;
  }

  .topbar-name {
    display: none;
  }

  .topbar-user {
    padding: 4px;
  }

  .tools-chevron {
    display: none;
  }

  .tools-dropdown {
    left: auto;
    right: -80px;
    transform: none;
    width: 220px;
    border-radius: 16px;
  }

  .drop-enter-from,
  .drop-leave-to {
    transform: translateY(-6px) scale(0.96);
  }
}

@media (max-width: 480px) {
  .topbar-link {
    padding: 0 8px;
  }

  .topbar-brand {
    margin-right: 0;
  }

  .topbar-level {
    display: none;
  }
}
</style>
