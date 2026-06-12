<template>
  <aside
    class="flow-dock"
    :class="{ open: showPanel }"
    aria-label="求职流程导航"
  >
    <div class="flow-dock-panel" @mouseenter="handlePanelEnter" @mouseleave="handlePanelLeave">
      <header class="flow-dock-head">
        <strong>求职流程</strong>
        <span>按步骤完成准备</span>
        <button class="flow-dock-close" @click="closePanel" aria-label="关闭">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </header>

      <nav class="flow-dock-list" aria-label="流程步骤">
        <button
          v-for="(step, index) in flowSteps"
          :key="step.key"
          type="button"
          class="flow-dock-item"
          :class="{ active: isActive(step), done: step.done }"
          @click="navigateStep(step)"
        >
          <span class="flow-step-index">
            <template v-if="step.done">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </template>
            <template v-else>{{ index + 1 }}</template>
          </span>
          <span class="flow-step-copy">
            <strong>{{ step.label }}</strong>
            <small>{{ step.hint }}</small>
          </span>
          <span class="flow-step-arrow" aria-hidden="true">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </span>
        </button>
      </nav>

      <!-- Progress bar -->
      <div class="flow-progress">
        <div class="flow-progress-bar" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <div class="flow-progress-text">{{ completedSteps }}/{{ totalSteps }} 完成</div>
    </div>

    <!-- Floating trigger button -->
    <button
      class="flow-dock-trigger"
      @click="togglePanel"
      @mouseenter="handleTriggerEnter"
      aria-label="打开求职流程"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/>
      </svg>
      <span class="trigger-badge" v-if="remainingSteps > 0">{{ remainingSteps }}</span>
    </button>
  </aside>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useInterviewDesk } from '../../composables/useInterviewDesk'

const route = useRoute()
const router = useRouter()
const desk = useInterviewDesk()
const showPanel = ref(false)
const isTouchLike = ref(false)
let closeTimer = null

const flowSteps = computed(() => [
  {
    key: 'resume',
    label: '上传简历',
    hint: '工作台准备材料',
    done: !!desk.pdfData.value,
    target: { name: 'home', hash: '#resume-upload' }
  },
  {
    key: 'jd',
    label: '补充岗位 JD',
    hint: '求职助手做匹配',
    done: !!desk.jdText.value.trim(),
    target: { name: 'coach' }
  },
  {
    key: 'coach',
    label: '咨询与修改',
    hint: '诊断简历与表达',
    done: desk.conversationMessages.value.length > 0,
    target: { name: 'coach' }
  },
  {
    key: 'interview',
    label: '进入模拟面试',
    hint: '开始针对性训练',
    done: !!desk.interviewSession.value,
    target: { name: 'interview' }
  },
  {
    key: 'resources',
    label: '资料管理',
    hint: '管理题库和档案',
    done: false,
    target: { name: 'resources' }
  },
  {
    key: 'review',
    label: '复盘成长',
    hint: '查看趋势和薄弱点',
    done: desk.reportHistory.value.length > 0,
    target: { name: 'reports' }
  },
  {
    key: 'agent',
    label: 'AI 教练',
    hint: '智能分析与训练',
    done: false,
    target: { name: 'agent' }
  }
])

const completedSteps = computed(() => flowSteps.value.filter(s => s.done).length)
const totalSteps = computed(() => flowSteps.value.length)
const remainingSteps = computed(() => totalSteps.value - completedSteps.value)
const progressPercent = computed(() => Math.round((completedSteps.value / totalSteps.value) * 100))

onMounted(() => {
  isTouchLike.value = window.matchMedia?.('(hover: none), (pointer: coarse)').matches || false
})

onBeforeUnmount(() => {
  clearCloseTimer()
})

watch(() => route.fullPath, () => {
  showPanel.value = false
})

function clearCloseTimer() {
  if (closeTimer) {
    window.clearTimeout(closeTimer)
    closeTimer = null
  }
}

function togglePanel() {
  showPanel.value = !showPanel.value
}

function openPanel() {
  clearCloseTimer()
  showPanel.value = true
}

function closePanel() {
  showPanel.value = false
}

function scheduleClose() {
  clearCloseTimer()
  closeTimer = window.setTimeout(() => {
    showPanel.value = false
  }, 300)
}

function handleTriggerEnter() {
  if (!isTouchLike.value) openPanel()
}

function handlePanelEnter() {
  clearCloseTimer()
}

function handlePanelLeave() {
  if (!isTouchLike.value) scheduleClose()
}

function isActive(step) {
  const targetName = step.target?.name
  if (step.key === 'resume') return route.name === 'home'
  if (step.key === 'jd' || step.key === 'coach') return route.name === 'coach'
  if (step.key === 'agent') return route.name === 'agent'
  return targetName ? route.name === targetName : false
}

async function navigateStep(step) {
  if (step.target) {
    await router.push(step.target)
    if (step.target.hash) {
      requestAnimationFrame(() => {
        document.querySelector(step.target.hash)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
    }
  }
  showPanel.value = false
}
</script>
