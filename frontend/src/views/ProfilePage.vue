<template>
  <div class="profile-page">
    <!-- 顶部横幅 -->
    <div class="profile-banner">
      <div class="banner-particles"></div>
      <div class="banner-content">
        <div class="profile-avatar-large">
          <span class="avatar-letter">{{ initial }}</span>
          <div class="avatar-ring" :class="levelTier"></div>
        </div>
        <div class="profile-info">
          <h1 class="profile-name">{{ displayName }}</h1>
          <p class="profile-username">@{{ user?.username || 'user' }}</p>
          <div class="profile-meta">
            <span class="meta-item" v-if="levelInfo">
              <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                <path d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z"/>
              </svg>
              Lv.{{ levelInfo.level }} {{ levelInfo.title }}
            </span>
            <span class="meta-item" v-if="streakInfo.active">
              <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                <path fill-rule="evenodd" d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.214.33-.403.713-.57 1.116-.334.804-.614 1.768-.84 2.734a31.365 31.365 0 00-.613 3.58 2.64 2.64 0 01-.945-1.067c-.328-.68-.398-1.534-.398-2.654A1 1 0 005.05 6.05 6.981 6.981 0 003 11a7 7 0 1011.95-4.95c-.592-.591-.98-.985-1.348-1.467-.363-.476-.724-1.063-1.207-2.03zM12.12 15.12A3 3 0 017 13s.879.5 2.5.5c0-1 .5-4 1.25-4.5.5 1 .786 1.293 1.371 1.879A2.99 2.99 0 0113 13a2.99 2.99 0 01-.879 2.121z" clip-rule="evenodd"/>
              </svg>
              连续 {{ streakInfo.days }} 天
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
                <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/>
              </svg>
              {{ joinDate }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="profile-body">
      <!-- 左列 -->
      <div class="profile-left">
        <!-- 等级卡片 -->
        <div class="profile-card level-card" v-if="levelInfo">
          <div class="card-header">
            <h3>等级进度</h3>
            <span class="level-badge-tag">Lv.{{ levelInfo.level }}</span>
          </div>
          <div class="level-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: levelInfo.progress + '%' }"></div>
            </div>
            <div class="progress-text">
              <span>{{ levelInfo.exp }} / {{ levelInfo.exp + levelInfo.needed }} XP</span>
              <span class="progress-percent">{{ Math.round(levelInfo.progress) }}%</span>
            </div>
          </div>
          <div class="level-info-grid">
            <div class="level-stat">
              <span class="stat-value">{{ gamificationData.totalInterviews }}</span>
              <span class="stat-label">面试次数</span>
            </div>
            <div class="level-stat">
              <span class="stat-value">{{ levelInfo.exp }}</span>
              <span class="stat-label">总经验值</span>
            </div>
            <div class="level-stat">
              <span class="stat-value">{{ gamificationData.avgScore }}</span>
              <span class="stat-label">平均分</span>
            </div>
          </div>
        </div>

        <!-- 连续签到卡片 -->
        <div class="profile-card streak-card" v-if="streakInfo.active">
          <div class="streak-flame">
            <svg viewBox="0 0 20 20" fill="currentColor" width="32" height="32">
              <path fill-rule="evenodd" d="M12.395 2.553a1 1 0 00-1.45-.385c-.345.23-.614.558-.822.88-.214.33-.403.713-.57 1.116-.334.804-.614 1.768-.84 2.734a31.365 31.365 0 00-.613 3.58 2.64 2.64 0 01-.945-1.067c-.328-.68-.398-1.534-.398-2.654A1 1 0 005.05 6.05 6.981 6.981 0 003 11a7 7 0 1011.95-4.95c-.592-.591-.98-.985-1.348-1.467-.363-.476-.724-1.063-1.207-2.03zM12.12 15.12A3 3 0 017 13s.879.5 2.5.5c0-1 .5-4 1.25-4.5.5 1 .786 1.293 1.371 1.879A2.99 2.99 0 0113 13a2.99 2.99 0 01-.879 2.121z" clip-rule="evenodd"/>
            </svg>
            <span class="streak-number">{{ streakInfo.days }}</span>
          </div>
          <span class="streak-label">天连续练习</span>
        </div>

        <!-- 成就卡片 -->
        <div class="profile-card achievements-card">
          <div class="card-header">
            <h3>成就墙</h3>
            <span class="achievement-count">{{ unlockedAchievements.length }}/{{ achievements.length }}</span>
          </div>
          <div class="achievements-grid" v-if="achievements.length">
            <div
              v-for="ach in achievements"
              :key="ach.id"
              class="achievement-item"
              :class="{ unlocked: ach.unlocked, locked: !ach.unlocked }"
              :title="ach.description"
            >
              <span class="achievement-icon">{{ ach.icon }}</span>
              <span class="achievement-name">{{ ach.name }}</span>
            </div>
          </div>
          <div class="empty-achievements" v-else>
            <span class="empty-icon">🏆</span>
            <p>完成面试解锁成就</p>
          </div>
        </div>
      </div>

      <!-- 右列 -->
      <div class="profile-right">
        <!-- 统计概览 -->
        <div class="profile-card stats-card">
          <div class="card-header">
            <h3>数据概览</h3>
          </div>
          <div class="stats-grid">
            <div class="stat-block" v-for="stat in statBlocks" :key="stat.label">
              <div class="stat-icon" :style="{ background: stat.color }">
                <span v-html="stat.icon"></span>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ stat.value }}</span>
                <span class="stat-desc">{{ stat.label }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近面试 -->
        <div class="profile-card recent-card">
          <div class="card-header">
            <h3>最近面试</h3>
            <router-link to="/reports" class="view-all-link">查看全部</router-link>
          </div>
          <div class="recent-list" v-if="recentInterviews.length">
            <div
              v-for="item in recentInterviews"
              :key="item.id"
              class="recent-item"
            >
              <div class="recent-score" :class="scoreClass(item.overallScore)">
                {{ item.overallScore || '--' }}
              </div>
              <div class="recent-info">
                <span class="recent-title">{{ item.position || '模拟面试' }}</span>
                <span class="recent-date">{{ formatDate(item.createdAt) }}</span>
              </div>
              <div class="recent-tags">
                <span class="tag" v-if="item.difficulty">{{ item.difficulty }}</span>
                <span class="tag" v-if="item.questionMode">{{ item.questionMode }}</span>
              </div>
            </div>
          </div>
          <div class="empty-state" v-else>
            <span class="empty-icon">📋</span>
            <p>暂无面试记录</p>
            <router-link to="/interview" class="empty-action">开始第一次面试</router-link>
          </div>
        </div>

        <!-- 每日任务 -->
        <div class="profile-card tasks-card">
          <div class="card-header">
            <h3>每日任务</h3>
            <span class="task-progress" v-if="dailyTasks.length">{{ completedTasksCount }}/{{ dailyTasks.length }}</span>
          </div>
          <div class="task-list" v-if="dailyTasks.length">
            <div
              v-for="task in dailyTasks"
              :key="task.taskType || task.id"
              class="task-item"
              :class="{ completed: task.completed }"
            >
              <span class="task-check">
                <svg v-if="task.completed" viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <svg v-else viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                </svg>
              </span>
              <span class="task-text">{{ task.description || task.title }}</span>
              <span class="task-xp">+{{ task.expReward || task.xp || 10 }} XP</span>
            </div>
          </div>
          <div class="empty-tasks" v-else>
            <p>今日任务已全部完成</p>
          </div>
        </div>

        <!-- 快捷入口 -->
        <div class="profile-card quick-actions">
          <div class="card-header">
            <h3>快捷操作</h3>
          </div>
          <div class="actions-grid">
            <router-link to="/interview" class="action-item">
              <span class="action-icon">🎯</span>
              <span class="action-label">开始面试</span>
            </router-link>
            <router-link to="/skills" class="action-item">
              <span class="action-icon">🌳</span>
              <span class="action-label">技能图谱</span>
            </router-link>
            <router-link to="/reports" class="action-item">
              <span class="action-icon">📊</span>
              <span class="action-label">成长报告</span>
            </router-link>
            <router-link to="/agent" class="action-item">
              <span class="action-icon">🤖</span>
              <span class="action-label">AI教练</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useInterviewDesk } from '../composables/useInterviewDesk'
import { getStoredUser } from '../api/http'

const desk = useInterviewDesk()
const user = computed(() => desk.user?.value || getStoredUser())
const levelInfo = computed(() => desk.levelInfo?.value || null)
const streakInfo = computed(() => desk.streakInfo?.value || { days: 0, active: false })
const achievements = computed(() => desk.achievements?.value || [])
const dailyTasks = computed(() => desk.dailyTasks?.value || [])
const gamification = computed(() => desk.gamification?.value || null)
const profileOverview = computed(() => desk.profileOverview?.value || null)

const initial = computed(() => {
  const name = user.value?.displayName || user.value?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

const displayName = computed(() => {
  return user.value?.displayName || user.value?.username || '用户'
})

const levelTier = computed(() => {
  if (!levelInfo.value) return ''
  const lv = levelInfo.value.level
  if (lv >= 20) return 'tier-diamond'
  if (lv >= 10) return 'tier-gold'
  if (lv >= 5) return 'tier-silver'
  return 'tier-bronze'
})

const joinDate = computed(() => {
  const date = user.value?.createdAt || user.value?.joinDate
  if (!date) return '近日加入'
  return new Date(date).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
})

const gamificationData = computed(() => ({
  totalInterviews: gamification.value?.totalInterviews || 0,
  avgScore: gamification.value?.averageScore || '--'
}))

const statBlocks = computed(() => [
  {
    label: '面试完成',
    value: gamificationData.value.totalInterviews,
    icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="20" height="20"><path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/></svg>',
    color: 'var(--gold-surface)'
  },
  {
    label: '平均评分',
    value: gamificationData.value.avgScore,
    icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="20" height="20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>',
    color: 'var(--teal-surface)'
  },
  {
    label: '总经验值',
    value: levelInfo.value?.exp || 0,
    icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="20" height="20"><path d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z"/></svg>',
    color: 'var(--violet-surface)'
  },
  {
    label: '成就解锁',
    value: unlockedAchievements.value.length,
    icon: '<svg viewBox="0 0 20 20" fill="currentColor" width="20" height="20"><path fill-rule="evenodd" d="M5 2a2 2 0 00-2 2v14l3.5-2 3.5 2 3.5-2 3.5 2V4a2 2 0 00-2-2H5zm4.707 3.707a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L8.414 9H10a3 3 0 013 3v1a1 1 0 102 0v-1a5 5 0 00-5-5H8.414l1.293-1.293z" clip-rule="evenodd"/></svg>',
    color: 'var(--gold-glow)'
  }
])

const unlockedAchievements = computed(() => achievements.value.filter(a => a.unlocked))

const recentInterviews = computed(() => {
  const overview = profileOverview.value
  return (overview?.recentInterviews || []).slice(0, 5)
})

const completedTasksCount = computed(() => dailyTasks.value.filter(t => t.completed).length)

function scoreClass(score) {
  if (!score) return ''
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-low'
}

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  if (diff < 86400000) return '今天'
  if (diff < 172800000) return '昨天'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(async () => {
  await desk.init?.()
  await desk.loadAchievements?.()
  await desk.loadProfileOverview?.()
})
</script>

<style scoped>
@import '../styles/profile-page.css';
</style>
