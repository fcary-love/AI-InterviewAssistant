<template>
  <section class="home-page">
    <input
      ref="fileInput"
      class="hidden"
      type="file"
      accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/png,image/jpeg,image/jpg,image/bmp,image/webp"
      @change="desk.handleFileChange"
    />

    <!-- Welcome Section -->
    <div class="welcome">
      <div class="welcome-content">
        <div class="welcome-badge">
          <span class="badge-dot"></span>
          AI Interview Copilot
        </div>
        <h1 class="welcome-title">
          你好<span class="wave">👋</span>，<br/>
          <span class="text-gradient-teal">准备下一次面试</span>
        </h1>
        <p class="welcome-desc">
          {{ welcomeMessage }}
        </p>
      </div>
      <div class="welcome-visual">
        <div class="visual-orb orb-teal"></div>
        <div class="visual-orb orb-amber"></div>
        <div class="visual-orb orb-violet"></div>
        <div class="visual-ring"></div>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="stat-card"
        :class="stat.accent"
      >
        <div class="stat-icon" :class="stat.accent">
          <span v-html="stat.icon"></span>
        </div>
        <div class="stat-body">
          <p class="stat-value">{{ stat.value }}</p>
          <p class="stat-label">{{ stat.label }}</p>
        </div>
      </div>
    </div>

    <!-- Main Grid -->
    <div class="main-grid">
      <!-- Quick Actions -->
      <div class="card actions-card">
        <h2 class="card-title">快捷操作</h2>
        <div class="actions-list">
          <div
            v-for="action in quickActions"
            :key="action.key"
            class="action-item"
            :class="action.class"
            @click="runQuickAction(action)"
          >
            <div class="action-icon-wrap" :class="action.class">
              <span v-html="action.icon"></span>
            </div>
            <div class="action-info">
              <strong>{{ action.title }}</strong>
              <span>{{ action.description }}</span>
            </div>
            <svg class="action-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- Upload Zone -->
      <div
        class="card upload-card"
        :class="{ 'has-file': resumeReady, uploading: isUploading }"
        @click="handleUpload"
      >
        <div class="upload-visual">
          <div class="upload-icon-glow">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
        </div>
        <template v-if="isUploading">
          <h3>正在解析简历...</h3>
          <p>AI 正在读取并分析你的材料</p>
          <div class="upload-progress"><span></span></div>
        </template>
        <template v-else-if="resumeReady">
          <h3>{{ desk.pdfData.value?.fileName }}</h3>
          <p>简历已就绪，点击更新</p>
          <span class="ready-dot"></span>
        </template>
        <template v-else>
          <h3>上传你的简历</h3>
          <p>支持 PDF / DOCX / 图片</p>
          <span class="upload-hint">点击或拖拽文件到此处</span>
        </template>
      </div>

      <!-- Module Pills -->
      <div class="card modules-card">
        <h2 class="card-title">功能模块</h2>
        <div class="modules-grid">
          <button
            v-for="m in modules"
            :key="m.key"
            class="module-item"
            @click="navigateTo(m.key)"
          >
            <span class="module-icon" v-html="m.icon"></span>
            <span class="module-label">{{ m.title }}</span>
            <span class="module-desc">{{ m.desc }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Next Action CTA -->
    <div class="card cta-card" @click="handlePrimaryNext">
      <div class="cta-content">
        <span class="cta-eyebrow">{{ nextAction.eyebrow }}</span>
        <h2 class="cta-title">{{ nextAction.title }}</h2>
        <p class="cta-desc">{{ nextAction.description }}</p>
      </div>
      <div class="cta-action">
        <button class="cta-btn" :disabled="isUploading">
          {{ nextAction.button }}
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"/>
            <polyline points="12 5 19 12 12 19"/>
          </svg>
        </button>
      </div>
      <div class="cta-glow"></div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useInterviewDesk } from '../composables/useInterviewDesk'

const router = useRouter()
const desk = useInterviewDesk()
const fileInput = ref(null)

const resumeReady = computed(() => !!desk.pdfData.value)
const isUploading = computed(() => desk.loading.value)
const overview = computed(() => desk.profileOverview.value || {})

const welcomeMessage = computed(() => {
  if (isUploading.value) return '正在解析你的简历，请稍候...'
  if (!resumeReady.value) return '上传简历，开启你的 AI 求职之旅。智能分析、模拟面试、个性化成长建议，一站式搞定。'
  if (overview.value.interviewCount > 0) return `已进行 ${overview.value.interviewCount} 次模拟面试，继续加油！每一步都在靠近理想 offer。`
  return '简历已就绪。试试 AI 求职咨询，或者开始一次模拟面试吧。'
})

const resumeVersionCount = computed(() => firstNum(overview.value.resumeVersionCount, overview.value.resumeCount, overview.value.resumeVersions?.length, resumeReady.value ? 1 : 0))
const jobMatchCount = computed(() => firstNum(overview.value.jobMatchCount, overview.value.matchCount, overview.value.jobMatches?.length, 0))
const interviewCount = computed(() => firstNum(overview.value.interviewCount, overview.value.interviews?.length, 0))
const averageScore = computed(() => firstNum(overview.value.averageInterviewScore, overview.value.avgScore, overview.value.averageScore, null))

const stats = computed(() => [
  {
    label: '简历版本',
    value: resumeVersionCount.value,
    accent: 'teal',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>'
  },
  {
    label: '岗位匹配',
    value: jobMatchCount.value,
    accent: 'amber',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>'
  },
  {
    label: '面试训练',
    value: interviewCount.value,
    accent: 'violet',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/><path d="M2 21c0-4.4 3.6-8 8-8 .9 0 1.7.1 2.5.4"/></svg>'
  },
  {
    label: '平均面试分',
    value: averageScore.value === null ? '-' : `${averageScore.value}分`,
    accent: 'teal',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>'
  }
])

const nextAction = computed(() => {
  if (isUploading.value) return { eyebrow: '解析中', title: '正在分析你的材料', description: '稍等一下，完成后会自动刷新。', button: '请稍候...' }
  if (!resumeReady.value) return { eyebrow: '第一步', title: '上传一份简历', description: '把基础材料放进来，后续分析才会更准。', button: '上传简历' }
  if (jobMatchCount.value === 0) return { eyebrow: '下一步', title: '补充目标岗位', description: '粘贴 JD 或上传招聘截图，看看当前简历和岗位的差距。', button: '去做岗位匹配' }
  if (interviewCount.value === 0) return { eyebrow: '开始练习', title: '开始一次模拟面试', description: '用当前材料生成面试问题，记录答案和评分。', button: '开始面试' }
  return { eyebrow: '复盘成长', title: '查看你的成长看板', description: '复盘分数趋势、高频薄弱点和下一步行动建议。', button: '查看成长' }
})

const quickActions = computed(() => [
  {
    key: 'upload',
    title: isUploading.value ? '解析中...' : resumeReady.value ? '更新简历' : '上传简历',
    description: 'PDF、DOCX 或图片格式',
    class: 'teal',
    upload: true,
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>'
  },
  {
    key: 'coach',
    title: 'AI 求职咨询',
    description: '简历诊断 · JD 匹配 · 定制建议',
    class: 'amber',
    route: '/coach',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
  },
  {
    key: 'interview',
    title: '开始模拟面试',
    description: '出题 · 评分 · 复盘 · 报告',
    class: 'violet',
    route: '/interview',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>'
  }
])

const modules = [
  { key: 'resources', title: '资料管理', desc: '题库与档案', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>' },
  { key: 'skills', title: '技能图谱', desc: '能力雷达', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>' },
  { key: 'reports', title: '成长看板', desc: '趋势与复盘', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' },
  { key: 'agent', title: 'AI 教练', desc: '智能辅导', icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/><path d="M17 14h.01"/><path d="M21 12a9 9 0 0 0-9-9"/><path d="M3 12a9 9 0 0 1 9-9"/></svg>' }
]

const routeMap = { resources: '/resources', reports: '/reports', agent: '/agent', skills: '/skills' }

onMounted(() => {
  if (fileInput.value) desk.fileInputRef.value = fileInput.value
})

function firstNum(...values) {
  for (const v of values) {
    if (v === null || v === undefined || v === '') continue
    const n = Number(v)
    if (!Number.isNaN(n)) return n
  }
  return 0
}

function handleUpload() { if (!isUploading.value) desk.triggerUpload() }
function handlePrimaryNext() {
  if (isUploading.value) return
  if (!resumeReady.value) { handleUpload(); return }
  if (jobMatchCount.value === 0) { router.push('/coach'); return }
  if (interviewCount.value === 0) { router.push('/interview'); return }
  router.push('/reports')
}
function runQuickAction(action) {
  if (action.upload) { handleUpload(); return }
  if (action.route) router.push(action.route)
}
function navigateTo(key) { const p = routeMap[key]; if (p) router.push(p) }

watch(() => desk.pdfData.value, (val, oldVal) => {
  if (val && val !== oldVal && !isUploading.value) {
    ElMessage.success('简历已解析完成')
    desk.loadProfileOverview?.()
  }
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: fade-in-up 0.4s var(--ease-out);
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.hidden { display: none; }

/* ---- Welcome ---- */

.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  padding: 48px 52px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-2xl);
  position: relative;
  overflow: hidden;
}

.welcome::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(45, 212, 191, 0.3), transparent);
  opacity: 0.5;
}

/* Subtle grid pattern overlay */
.welcome::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.015) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.015) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
  opacity: 0.5;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  background: var(--primary-surface);
  border: 1px solid var(--primary-border);
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 20px;
  letter-spacing: 0.02em;
}

.badge-dot {
  width: 6px;
  height: 6px;
  background: var(--primary);
  border-radius: 50%;
  animation: breathe 2s ease-in-out infinite;
}

.welcome-title {
  margin: 0;
  font-family: 'DM Serif Display', 'Noto Serif SC', var(--font-display);
  font-size: clamp(32px, 4vw, 48px);
  font-weight: 400;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.wave {
  display: inline-block;
  animation: float 1.5s ease-in-out infinite;
}

.welcome-desc {
  margin: 16px 0 0;
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.7;
  max-width: 480px;
}

/* Visual orbs */
.welcome-visual {
  position: relative;
  width: 200px;
  height: 160px;
  flex-shrink: 0;
  display: none;
}

@media (min-width: 768px) {
  .welcome-visual { display: block; }
}

.visual-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  animation: float 4s ease-in-out infinite;
}

.orb-teal {
  width: 80px;
  height: 80px;
  background: rgba(45, 212, 191, 0.15);
  top: 20px;
  left: 60px;
  animation-delay: 0s;
}

.orb-amber {
  width: 60px;
  height: 60px;
  background: rgba(251, 191, 36, 0.1);
  top: 60px;
  left: 100px;
  animation-delay: 1s;
}

.orb-violet {
  width: 50px;
  height: 50px;
  background: rgba(167, 139, 250, 0.1);
  top: 10px;
  left: 110px;
  animation-delay: 2s;
}

.visual-ring {
  position: absolute;
  width: 120px;
  height: 120px;
  top: 20px;
  left: 40px;
  border: 1px solid rgba(45, 212, 191, 0.1);
  border-radius: 50%;
  animation: pulse-glow 3s ease-in-out infinite;
}

/* ---- Stats ---- */

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  transition: all var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  border-color: var(--border-strong);
  transform: translateY(-2px);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.teal {
  background: rgba(45, 212, 191, 0.08);
  color: var(--primary);
}

.stat-icon.amber {
  background: rgba(251, 191, 36, 0.08);
  color: var(--amber);
}

.stat-icon.violet {
  background: rgba(167, 139, 250, 0.08);
  color: var(--violet);
}

.stat-body { min-width: 0; }

.stat-value {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.stat-label {
  margin: 4px 0 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ---- Main Grid ---- */

.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
}

.card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 24px;
  transition: all var(--duration-normal) var(--ease-out);
}

.card:hover {
  border-color: var(--border-strong);
}

.card-title {
  margin: 0 0 18px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

/* ---- Quick Actions ---- */

.actions-card {
  padding: 24px;
}

.actions-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.action-item:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-default);
}

.action-item:hover .action-chevron {
  opacity: 1;
  transform: translateX(0);
}

.action-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-icon-wrap.teal {
  background: rgba(45, 212, 191, 0.08);
  color: var(--primary);
}

.action-icon-wrap.amber {
  background: rgba(251, 191, 36, 0.08);
  color: var(--amber);
}

.action-icon-wrap.violet {
  background: rgba(167, 139, 250, 0.08);
  color: var(--violet);
}

.action-info {
  flex: 1;
  min-width: 0;
}

.action-info strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-info span {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  display: block;
}

.action-chevron {
  flex-shrink: 0;
  color: var(--text-muted);
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--duration-fast);
}

/* ---- Upload ---- */

.upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px 24px;
  cursor: pointer;
  min-height: 240px;
  border: 2px dashed var(--border-default);
  background: var(--bg-surface);
}

.upload-card:hover {
  border-color: var(--primary-border);
  background: var(--bg-card);
}

.upload-card.has-file {
  border-style: solid;
  border-color: rgba(52, 211, 153, 0.2);
  background: rgba(52, 211, 153, 0.02);
}

.upload-card.uploading {
  cursor: default;
  border-style: solid;
  border-color: var(--primary-border);
  animation: border-pulse 2s ease-in-out infinite;
}

.upload-visual { margin-bottom: 16px; }

.upload-icon-glow {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--primary-surface);
  border: 1px solid var(--primary-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  transition: all var(--duration-normal) var(--ease-out);
}

.upload-card:hover .upload-icon-glow {
  background: rgba(45, 212, 191, 0.1);
  transform: translateY(-2px);
}

.upload-card h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.upload-card p {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.upload-hint {
  display: inline-block;
  margin-top: 12px;
  font-size: 11px;
  color: var(--text-muted);
  padding: 4px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
}

.ready-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: var(--success);
  border-radius: 50%;
  margin-top: 8px;
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.4);
}

.upload-progress {
  width: 120px;
  height: 3px;
  background: var(--bg-surface);
  border-radius: var(--radius-full);
  margin-top: 16px;
  overflow: hidden;
}

.upload-progress span {
  display: block;
  width: 60%;
  height: 100%;
  background: var(--primary);
  border-radius: var(--radius-full);
  animation: shimmer 1.5s infinite;
}

/* ---- Modules ---- */

.modules-card { padding: 24px; }

.modules-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.module-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 16px;
  background: var(--bg-surface);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  text-align: left;
  color: inherit;
  font: inherit;
}

.module-item:hover {
  border-color: var(--border-default);
  background: var(--bg-card-hover);
}

.module-icon {
  display: flex;
  color: var(--text-muted);
  transition: color var(--duration-fast);
}

.module-item:hover .module-icon {
  color: var(--primary);
}

.module-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.module-desc {
  font-size: 11px;
  color: var(--text-muted);
}

/* ---- CTA ---- */

.cta-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  background: linear-gradient(135deg, var(--bg-card), var(--bg-card-hover));
  border-color: var(--primary-border);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.cta-card:hover {
  border-color: rgba(45, 212, 191, 0.4);
}

.cta-glow {
  position: absolute;
  right: 80px;
  top: -40px;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.06), transparent);
  pointer-events: none;
}

.cta-content { position: relative; z-index: 1; }

.cta-eyebrow {
  font-size: 11px;
  font-weight: 700;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.cta-title {
  margin: 6px 0 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.cta-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.cta-action { position: relative; z-index: 1; flex-shrink: 0; }

.cta-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: var(--primary);
  color: var(--bg-abyss);
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  white-space: nowrap;
  box-shadow: 0 4px 20px rgba(45, 212, 191, 0.2);
}

.cta-btn:hover {
  background: var(--primary-bright);
  box-shadow: 0 8px 32px rgba(45, 212, 191, 0.3);
  transform: translateY(-1px);
}

.cta-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

/* ---- Responsive ---- */

@media (max-width: 1024px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .main-grid { grid-template-columns: 1fr 1fr; }
  .modules-card { grid-column: span 2; }
  .actions-card { grid-column: span 2; }
}

@media (max-width: 768px) {
  .main-grid { grid-template-columns: 1fr; }
  .modules-card { grid-column: span 1; }
  .actions-card { grid-column: span 1; }
}

@media (max-width: 640px) {
  .welcome { padding: 28px 24px; }
  .welcome-title { font-size: 24px; }
  .stats-row { grid-template-columns: 1fr 1fr; gap: 10px; }
  .stat-card { padding: 16px; gap: 12px; }
  .stat-icon { width: 38px; height: 38px; }
  .stat-value { font-size: 22px; }
  .modules-grid { grid-template-columns: 1fr 1fr; }
  .cta-card { flex-direction: column; align-items: flex-start; padding: 24px; }
}
</style>
