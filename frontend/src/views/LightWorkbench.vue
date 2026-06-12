<template>
  <div class="app-shell">
    <!-- ==================== NAVBAR ==================== -->
    <nav class="navbar">
      <div class="navbar-inner">
        <div class="navbar-brand">
          <div class="logo-mark">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5z" fill="#1e40af" opacity="0.9"/>
              <path d="M2 17l10 5 10-5" stroke="#1e40af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              <path d="M2 12l10 5 10-5" stroke="#1e40af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
          </div>
          <span class="logo-text">FaceAI</span>
        </div>

        <div class="nav-links">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeView === item.key }"
            @click="navigateTo(item)"
          >
            {{ item.label }}
            <span v-if="activeView === item.key" class="nav-dot"></span>
          </button>
        </div>

        <div class="navbar-actions">
          <div class="search-box">
            <svg width="14" height="14" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path stroke-linecap="round" d="M21 21l-4.35-4.35"/></svg>
            <span>搜索...</span>
          </div>
          <div class="user-ring">
            <span>U</span>
          </div>
        </div>
      </div>
    </nav>

    <!-- ==================== MAIN CONTENT ==================== -->
    <main class="main-grid">

      <!-- ========== MODULE A: HERO BANNER ========== -->
      <section class="hero-card fade-up" style="animation-delay: 0ms">
        <div class="hero-left">
          <span class="hero-eyebrow">Career Operating System</span>
          <h1 class="hero-headline">把零散的面试记录，<br/>提炼成一条清晰的求职成长线。</h1>
          <p class="hero-sub">上传简历、沉淀项目经历，通过 AI 模拟面试持续优化你的表达。每一次练习都是下一次面试的筹码。</p>
          <div class="hero-pills">
            <span class="pill">AI Resume</span>
            <span class="pill">Mock Interview</span>
            <span class="pill">Career Copilot</span>
          </div>
        </div>
        <div class="hero-right">
          <div class="upload-zone" @click="triggerUpload" @mouseenter="uploadHover=true" @mouseleave="uploadHover=false" :class="{ hover: uploadHover }">
            <div class="upload-icon-circle">
              <svg width="28" height="28" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"/></svg>
            </div>
            <p class="upload-label">拖拽简历到这里</p>
            <p class="upload-sub">支持 PDF / DOC / DOCX</p>
          </div>
        </div>
      </section>

      <!-- ========== MODULE C: GROWTH DASHBOARD (5 col) ========== -->
      <section class="col-5 fade-up" style="animation-delay: 80ms">
        <!-- Score Card -->
        <div class="score-card">
          <div class="score-top">
            <span class="score-label">最新综合评分</span>
            <span class="score-badge">良好</span>
          </div>
          <div class="score-number">78</div>
          <p class="score-insight">已击败了全国 <strong>87.4%</strong> 的同岗位竞争者，表现稳定。</p>
          <div class="score-bar-track">
            <div class="score-bar-fill" style="width: 78%"></div>
          </div>
        </div>

        <!-- Radar Chart -->
        <div class="radar-card">
          <div class="card-header">
            <h3>六维能力分析</h3>
            <span class="card-tag">最近一次</span>
          </div>
          <div class="radar-wrap">
            <svg viewBox="0 0 200 200" class="radar-svg">
              <polygon v-for="r in [0.2,0.4,0.6,0.8,1]" :key="r" :points="ring(r)" fill="none" stroke="var(--border-default)" stroke-width="0.8"/>
              <line v-for="(a,i) in axes" :key="'a'+i" x1="100" y1="100" :x2="a.x" :y2="a.y" stroke="var(--border-default)" stroke-width="0.5"/>
              <polygon :points="dataPoly" fill="rgba(37,99,235,0.07)" stroke="#2563eb" stroke-width="2" stroke-linejoin="round"/>
              <circle v-for="(d,i) in dataPts" :key="'d'+i" :cx="d.x" :cy="d.y" r="3" fill="#fff" stroke="#2563eb" stroke-width="2"/>
              <text v-for="(l,i) in labels" :key="'l'+i" :x="l.x" :y="l.y" text-anchor="middle" dominant-baseline="middle" class="radar-text">{{ l.t }}</text>
            </svg>
          </div>
        </div>

        <!-- Trend Chart -->
        <div class="trend-card">
          <div class="card-header">
            <h3>成长趋势</h3>
            <span class="trend-up">↑ 12%</span>
          </div>
          <div class="trend-wrap">
            <svg viewBox="0 0 320 120" class="trend-svg" preserveAspectRatio="none">
              <line v-for="i in 4" :key="'g'+i" x1="0" :y1="i*30" x2="320" :y2="i*30" stroke="#f1f5f9" stroke-width="1"/>
              <path d="M0,96 L40,84 L80,72 L120,78 L160,60 L200,48 L240,36 L280,24 L320,18 L320,120 L0,120 Z" fill="rgba(37,99,235,0.05)"/>
              <polyline points="0,96 40,84 80,72 120,78 160,60 200,48 240,36 280,24 320,18" fill="none" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle v-for="(p,i) in pts" :key="'p'+i" :cx="p.x" :cy="p.y" r="3" fill="#fff" stroke="#2563eb" stroke-width="2"/>
            </svg>
            <div class="trend-labels">
              <span v-for="l in ['1次','2次','3次','4次','5次','6次','7次','8次']" :key="l">{{ l }}</span>
            </div>
          </div>
        </div>

        <!-- Weak Points -->
        <div class="weak-card">
          <div class="card-header"><h3>高频薄弱点</h3></div>
          <div class="weak-list">
            <div v-for="w in weakPoints" :key="w.name" class="weak-row">
              <span class="weak-dot" :class="w.lv"></span>
              <span class="weak-name">{{ w.name }}</span>
              <span class="weak-cnt">{{ w.cnt }}次</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ========== MODULE B: INTERVIEW CONFIG (7 col) ========== -->
      <section class="col-7 fade-up" style="animation-delay: 160ms">
        <!-- Interviewer Matrix -->
        <div class="config-section">
          <label class="section-label">选择 AI 面试官</label>
          <div class="iv-grid">
            <div
              v-for="(iv, idx) in interviewers"
              :key="iv.id"
              class="iv-card fade-up"
              :class="{ selected: currentIV === iv.id }"
              :style="{ animationDelay: (200 + idx * 40) + 'ms' }"
              @click="currentIV = iv.id"
            >
              <div v-if="currentIV === iv.id" class="iv-check">
                <svg width="12" height="12" fill="none" viewBox="0 0 24 24" stroke="#fff" stroke-width="3"><path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/></svg>
              </div>
              <div class="iv-avatar">{{ iv.avatar }}</div>
              <h4 class="iv-name">{{ iv.name }}</h4>
              <p class="iv-style">{{ iv.style }}</p>
              <p class="iv-desc">{{ iv.desc }}</p>
            </div>
          </div>
        </div>

        <!-- Form -->
        <div class="form-card fade-up" style="animation-delay: 400ms">
          <div class="form-group">
            <label class="form-label">目标岗位名称</label>
            <input v-model="jobTitle" type="text" class="form-input" placeholder="例：Java 后端开发工程师" />
          </div>

          <div class="form-group">
            <label class="form-label">企业招聘 JD</label>
            <textarea v-model="jdText" class="form-textarea" rows="5" placeholder="粘贴完整的岗位描述，AI 将根据 JD 生成高针对性的面试问题..."></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">面试难易度</label>
            <div class="slider-wrap">
              <input v-model.number="difficulty" type="range" min="1" max="4" step="1" class="range-input" />
              <div class="slider-labels">
                <span :class="{ active: difficulty === 1 }">入门</span>
                <span :class="{ active: difficulty === 2 }">标准</span>
                <span :class="{ active: difficulty === 3 }">严格</span>
                <span :class="{ active: difficulty === 4 }">高压</span>
              </div>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label class="form-label">重点考察</label>
              <select v-model="focus" class="form-select">
                <option value="basic">基础扎实</option>
                <option value="project">项目深挖</option>
                <option value="system">系统设计</option>
                <option value="mixed">综合考察</option>
              </select>
            </div>
            <div class="form-group flex-1">
              <label class="form-label">面试风格</label>
              <select v-model="style" class="form-select">
                <option value="gentle">温和引导</option>
                <option value="strict">严格追问</option>
                <option value="pressure">压力面试</option>
              </select>
            </div>
          </div>

          <button class="btn-start" @click="startInterview">
            开始 AI 模拟面试
            <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3"/></svg>
          </button>
        </div>
      </section>

    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/* ==================== BODY BG OVERRIDE ==================== */
const origBg = document.body.style.background
const origBgAtt = document.body.style.backgroundAttachment

onMounted(() => {
  document.body.style.background = '#111122'
  document.body.style.backgroundAttachment = 'fixed'
})

onUnmounted(() => {
  document.body.style.background = origBg
  document.body.style.backgroundAttachment = origBgAtt
})

/* ==================== NAV ==================== */
const activeView = ref('dashboard')
const navItems = [
  { key: 'dashboard', label: '工作台', route: '/light' },
  { key: 'coach', label: '求职助手', route: '/coach' },
  { key: 'interview', label: '模拟面试', route: null },
  { key: 'resources', label: '资料管理', route: '/resources' },
  { key: 'skills', label: '技能图谱', route: '/skills' },
  { key: 'reports', label: '成长报告', route: null },
]

function navigateTo(item) {
  if (item.route) {
    router.push(item.route)
  } else {
    activeView.value = item.key
  }
}

/* ==================== HERO ==================== */
const uploadHover = ref(false)
function triggerUpload() {}

/* ==================== INTERVIEWERS ==================== */
const currentIV = ref(1)
const interviewers = [
  { id: 1, name: '严肃姐 · 严敏', avatar: '👩‍💼', style: '大厂P9 / 追问到底 / 冷酷', desc: '擅长分布式架构深度剥离，专治八股文死记硬背。' },
  { id: 2, name: '温暖哥 · 许朝阳', avatar: '👨‍💼', style: '外企总监 / 引导启发 / 亲和', desc: '注重系统设计与代码洁癖，擅长在聊天中挖掘你的闪光点。' },
  { id: 3, name: '压力王 · 查理', avatar: '🧛', style: 'CTO / 突发场景 / 高压', desc: '高频打断，测试你在极端混乱、无逻辑职场环境下的临场复原力。' },
  { id: 4, name: '幽默叔 · 老麦', avatar: '👨‍🎨', style: '技术合伙人 / 业务导向 / 风趣', desc: '从商业闭环和产品ROI角度切入，看你是否具备真正的Owner意识。' },
]

/* ==================== FORM ==================== */
const jobTitle = ref('')
const jdText = ref('')
const difficulty = ref(2)
const focus = ref('project')
const style = ref('strict')
function startInterview() {}

/* ==================== RADAR ==================== */
const dims = ['技术深度', '表达逻辑', '岗位匹配', '问题解决', '追问应对', '抗压能力']
const scores = [82, 75, 90, 68, 72, 65]
const R = 72
function pt(i, v) {
  const a = (Math.PI * 2 * i) / 6 - Math.PI / 2
  return { x: 100 + (v / 100) * R * Math.cos(a), y: 100 + (v / 100) * R * Math.sin(a) }
}
const axes = dims.map((_, i) => pt(i, 100))
function ring(s) { return dims.map((_, i) => { const p = pt(i, 100 * s); return `${p.x},${p.y}` }).join(' ') }
const dataPts = scores.map((s, i) => pt(i, s))
const dataPoly = dataPts.map(p => `${p.x},${p.y}`).join(' ')
const labels = dims.map((t, i) => { const p = pt(i, 118); return { x: p.x, y: p.y, t } })

/* ==================== TREND ==================== */
const pts = [
  { x: 0, y: 96 }, { x: 40, y: 84 }, { x: 80, y: 72 }, { x: 120, y: 78 },
  { x: 160, y: 60 }, { x: 200, y: 48 }, { x: 240, y: 36 }, { x: 280, y: 24 }, { x: 320, y: 18 },
]

/* ==================== WEAK POINTS ==================== */
const weakPoints = [
  { name: 'Redis 缓存穿透', cnt: 5, lv: 'high' },
  { name: 'MySQL 索引优化', cnt: 4, lv: 'high' },
  { name: 'JVM 垃圾回收', cnt: 3, lv: 'mid' },
  { name: 'Spring AOP 原理', cnt: 2, lv: 'mid' },
  { name: '分布式事务', cnt: 3, lv: 'high' },
  { name: 'TCP 三次握手', cnt: 1, lv: 'low' },
]
</script>

<style scoped>
/* ==================== APP SHELL ==================== */
.app-shell {
  min-height: 100vh;
  background: var(--bg-surface);
  font-family: "Outfit", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", -apple-system, sans-serif;
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
}

/* ==================== NAVBAR ==================== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(226,232,240,0.8);
  z-index: 50;
}

.navbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-mark {
  width: 32px;
  height: 32px;
  background: var(--primary-surface);
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.03em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-item {
  position: relative;
  background: none;
  border: none;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.nav-item:hover {
  color: #2563eb;
}

.nav-item.active {
  color: #2563eb;
  font-weight: 600;
}

.nav-dot {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background: #2563eb;
  border-radius: 50%;
  animation: dot-in 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes dot-in {
  from { opacity: 0; transform: translateX(-50%) scale(0); }
  to { opacity: 1; transform: translateX(-50%) scale(1); }
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  background: #f1f5f9;
  border: 1px solid var(--border-default);
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.search-box:hover {
  border-color: var(--border-strong);
}

.user-ring {
  width: 32px;
  height: 32px;
  background: #1e40af;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  ring: 2px solid var(--border-default);
  box-shadow: 0 0 0 2px var(--border-default);
}

/* ==================== MAIN GRID ==================== */
.main-grid {
  max-width: 1280px;
  margin: 0 auto;
  padding: 88px 32px 60px;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 24px;
  align-items: start;
}

/* ==================== HERO CARD ==================== */
.hero-card {
  grid-column: 1 / -1;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 40px;
  display: flex;
  gap: 40px;
  align-items: center;
}

.hero-left {
  flex: 1.5;
}

.hero-eyebrow {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  background: var(--primary-surface);
  padding: 4px 12px;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 16px;
}

.hero-headline {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
  color: var(--text-primary);
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.hero-sub {
  margin: 14px 0 0;
  font-size: 15px;
  color: #64748b;
  line-height: 1.7;
  max-width: 480px;
}

.hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.pill {
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid var(--border-default);
  background: var(--bg-surface);
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

.hero-right {
  flex: 1;
  max-width: 340px;
}

.upload-zone {
  border: 2px dashed var(--border-default);
  border-radius: 16px;
  padding: 36px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  background: rgba(248,250,252,0.5);
}

.upload-zone.hover {
  border-color: #93c5fd;
  background: rgba(239,246,255,0.2);
}

.upload-icon-circle {
  width: 52px;
  height: 52px;
  background: var(--primary-surface);
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  color: #2563eb;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.upload-zone.hover .upload-icon-circle {
  background: #dbeafe;
  transform: translateY(-2px);
}

.upload-label {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.upload-sub {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

/* ==================== COL LAYOUT ==================== */
.col-5 {
  grid-column: 1 / 6;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.col-7 {
  grid-column: 6 / -1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ==================== SCORE CARD ==================== */
.score-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 28px;
}

.score-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.score-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.score-badge {
  font-size: 11px;
  font-weight: 700;
  color: #92400e;
  background: #fef3c7;
  padding: 3px 10px;
  border-radius: 999px;
}

.score-number {
  font-size: 72px;
  font-weight: 900;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.04em;
}

.score-insight {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

.score-insight strong {
  color: #d97706;
  font-weight: 700;
}

.score-bar-track {
  margin-top: 16px;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.score-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  border-radius: 3px;
  transition: width 1s cubic-bezier(0.16, 1, 0.3, 1);
}

/* ==================== RADAR CARD ==================== */
.radar-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.card-tag {
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  background: var(--primary-surface);
  padding: 3px 10px;
  border-radius: 999px;
}

.radar-wrap {
  display: flex;
  justify-content: center;
}

.radar-svg {
  width: 200px;
  height: 200px;
}

.radar-text {
  font-size: 8px;
  fill: #64748b;
  font-weight: 500;
}

/* ==================== TREND CARD ==================== */
.trend-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 24px;
}

.trend-up {
  font-size: 11px;
  font-weight: 700;
  color: #059669;
  background: #ecfdf5;
  padding: 3px 10px;
  border-radius: 999px;
}

.trend-wrap {
  position: relative;
}

.trend-svg {
  width: 100%;
  height: 120px;
}

.trend-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
}

.trend-labels span {
  font-size: 10px;
  color: var(--text-muted);
}

/* ==================== WEAK CARD ==================== */
.weak-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 24px;
}

.weak-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weak-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--bg-surface);
  border: 1px solid #f1f5f9;
  border-radius: 10px;
}

.weak-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.weak-dot.high { background: #ef4444; }
.weak-dot.mid { background: #f59e0b; }
.weak-dot.low { background: #3b82f6; }

.weak-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.weak-cnt {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

/* ==================== CONFIG SECTION ==================== */
.config-section {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 28px;
}

.section-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 16px;
}

.iv-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.iv-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.iv-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.iv-card.selected {
  border: 2px solid #2563eb;
  background: rgba(239,246,255,0.3);
  box-shadow: 0 2px 12px rgba(37,99,235,0.08);
}

.iv-check {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 22px;
  height: 22px;
  background: #2563eb;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: check-in 0.25s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes check-in {
  from { opacity: 0; transform: scale(0.5); }
  to { opacity: 1; transform: scale(1); }
}

.iv-avatar {
  font-size: 36px;
  margin-bottom: 10px;
}

.iv-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.iv-style {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  letter-spacing: 0.02em;
}

.iv-desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

/* ==================== FORM CARD ==================== */
.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(15,23,42,0.015);
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  padding: 11px 16px;
  font-size: 14px;
  color: #1e293b;
  outline: none;
  font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

.form-input::placeholder { color: var(--text-muted); }
.form-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 4px rgba(59,130,246,0.06); }

.form-textarea {
  width: 100%;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  color: #1e293b;
  outline: none;
  resize: none;
  line-height: 1.6;
  font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

.form-textarea::placeholder { color: var(--text-muted); }
.form-textarea:focus { border-color: #3b82f6; box-shadow: 0 0 0 4px rgba(59,130,246,0.06); }

.form-select {
  width: 100%;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  padding: 11px 16px;
  font-size: 14px;
  color: #1e293b;
  outline: none;
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 12px center;
  background-repeat: no-repeat;
  background-size: 16px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

.form-select:focus { border-color: #3b82f6; box-shadow: 0 0 0 4px rgba(59,130,246,0.06); }

.form-row {
  display: flex;
  gap: 14px;
}

.flex-1 { flex: 1; }

/* ==================== RANGE SLIDER ==================== */
.slider-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.range-input {
  width: 100%;
  height: 6px;
  appearance: none;
  background: var(--border-default);
  border-radius: 3px;
  outline: none;
  cursor: pointer;
}

.range-input::-webkit-slider-thumb {
  appearance: none;
  width: 20px;
  height: 20px;
  background: #1e40af;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(30,64,175,0.3);
  cursor: pointer;
}

.range-input::-moz-range-thumb {
  width: 20px;
  height: 20px;
  background: #1e40af;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(30,64,175,0.3);
  cursor: pointer;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
}

.slider-labels span {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  transition: color 0.2s;
}

.slider-labels span.active {
  color: #1e40af;
  font-weight: 700;
}

/* ==================== START BUTTON ==================== */
.btn-start {
  width: 100%;
  padding: 16px;
  background: #1e40af;
  color: white;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.03em;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: inherit;
  box-shadow: 0 8px 24px rgba(30,64,175,0.15);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-start:hover {
  background: #1d4ed8;
  box-shadow: 0 12px 32px rgba(30,64,175,0.25);
  transform: translateY(-1px);
}

.btn-start:active {
  transform: scale(0.98);
}

/* ==================== ANIMATIONS ==================== */
.fade-up {
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ==================== RESPONSIVE ==================== */
@media (max-width: 1024px) {
  .main-grid {
    grid-template-columns: 1fr;
    padding: 80px 20px 40px;
  }
  .hero-card { grid-column: 1 / -1; flex-direction: column; }
  .hero-right { max-width: 100%; width: 100%; }
  .col-5 { grid-column: 1 / -1; }
  .col-7 { grid-column: 1 / -1; }
}

@media (max-width: 640px) {
  .nav-links { display: none; }
  .hero-headline { font-size: 24px; }
  .iv-grid { grid-template-columns: 1fr; }
  .form-row { flex-direction: column; }
  .score-number { font-size: 56px; }
}
</style>
