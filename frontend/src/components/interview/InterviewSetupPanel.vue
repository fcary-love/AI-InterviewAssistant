<template>
  <section ref="rootRef" class="aurora-interview-panel">
    <div class="panel-header">
      <span class="panel-eyebrow">Session</span>
      <strong class="panel-title">{{ selectedDirectionLabel }} · {{ randomMixEnabled ? '随机搭配' : difficulty }}</strong>
    </div>

    <template v-if="!interviewSession">
      <!-- Two-column layout: Interviewers (7) + Config (5) -->
      <div class="config-layout">
        <!-- Left: Interviewer Selection -->
        <div class="config-left">
          <InterviewerSelector
            :interviewers="interviewers"
            :selected-id="selectedInterviewerId"
            @select="$emit('select-interviewer', $event)"
          />
        </div>

        <!-- Right: Configuration Panel -->
        <div class="config-right">
          <!-- Random Mix Toggle -->
          <div class="mix-toggle">
            <div>
              <strong>随机搭配</strong>
              <span>保留方向，其余配置由系统组合</span>
            </div>
            <button
              type="button"
              class="mix-btn"
              :class="{ active: randomMixEnabled }"
              @click="$emit('toggle-random-mix')"
            >
              {{ randomMixEnabled ? '已开启' : '开启' }}
            </button>
          </div>

          <!-- Form Fields -->
          <div class="config-form">
            <label class="form-field">
              <span class="form-label">面试岗位</span>
              <el-select :model-value="selectedDirection" @update:model-value="$emit('update:selectedDirection', $event)">
                <el-option label="后端开发" value="backend" />
                <el-option label="前端开发" value="frontend" />
                <el-option label="测试工程师" value="qa" />
                <el-option label="运维工程师" value="ops" />
                <el-option label="软件开发" value="software" />
              </el-select>
            </label>

            <label class="form-field">
              <span class="form-label">难度级别</span>
              <el-segmented
                :model-value="difficulty"
                :options="difficultyOptions"
                :disabled="randomMixEnabled"
                @update:model-value="$emit('update:difficulty', $event)"
              />
            </label>

            <label class="form-field">
              <span class="form-label">重点考察</span>
              <el-select
                :model-value="interviewFocus"
                :disabled="randomMixEnabled"
                @update:model-value="$emit('update:interviewFocus', $event)"
              >
                <el-option v-for="item in focusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>

            <label class="form-field">
              <span class="form-label">面试风格</span>
              <el-select
                :model-value="interviewStyle"
                :disabled="randomMixEnabled"
                @update:model-value="$emit('update:interviewStyle', $event)"
              >
                <el-option label="温和引导" value="gentle" />
                <el-option label="严格追问" value="strict" />
                <el-option label="压力面试" value="pressure" />
              </el-select>
            </label>

            <label class="form-field form-full">
              <span class="form-label">题库来源</span>
              <el-radio-group
                :model-value="questionMode"
                :disabled="randomMixEnabled"
                @update:model-value="$emit('update:questionMode', $event)"
              >
                <el-radio-button value="improvised">即兴</el-radio-button>
                <el-radio-button value="online">在线题库</el-radio-button>
                <el-radio-button value="custom">自定义</el-radio-button>
              </el-radio-group>
            </label>

            <label class="form-field form-full">
              <span class="form-label">岗位描述 (JD)</span>
              <textarea
                :value="jdText"
                placeholder="粘贴岗位描述，AI 会根据 JD 和你的简历生成更有针对性的问题（可选）"
                rows="3"
                @input="$emit('update:jdText', $event.target.value)"
              />
            </label>

            <div class="form-row">
              <label class="form-field form-inline">
                <span class="form-label">流式模式</span>
                <el-switch
                  :model-value="useStreaming"
                  active-text="逐字输出"
                  inactive-text="普通模式"
                  @update:model-value="$emit('update:useStreaming', $event)"
                />
              </label>

              <label class="form-field form-inline">
                <span class="form-label">自适应难度</span>
                <el-switch
                  :model-value="adaptiveDifficulty"
                  active-text="自动调整"
                  inactive-text="手动选择"
                  @update:model-value="$emit('update:adaptiveDifficulty', $event)"
                />
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Start Button Row -->
      <div class="start-row" ref="startRef">
        <div class="start-summary">
          <span class="start-eyebrow">Session Draft</span>
          <strong>{{ sessionDraftText }}</strong>
        </div>
        <button
          type="button"
          class="start-btn"
          :disabled="interviewStarting"
          @click="$emit('start')"
        >
          {{ interviewStarting ? '准备中...' : '开始面试' }}
          <svg v-if="!interviewStarting" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
            <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd"/>
          </svg>
        </button>
      </div>
    </template>

    <!-- Active Interview Session -->
    <article v-if="interviewSession" class="session-stage">
      <div class="session-header">
        <span class="session-eyebrow">{{ interviewFinished ? '面试已完成' : `当前问题 · 已保存 ${answeredCount} 题` }}</span>
        <span v-if="isFollowUp" class="follow-up-tag">AI 追问</span>
        <DifficultyIndicator v-if="currentDifficulty" :difficulty="currentDifficulty" />
      </div>

      <!-- Streaming Mode -->
      <div v-if="streamingQuestion" class="streaming-block">
        <p>{{ streamingText || 'AI 正在思考下一个问题...' }}</p>
        <span class="cursor-blink">|</span>
      </div>
      <!-- Normal Mode -->
      <p v-else-if="currentQuestion" class="question-text">{{ currentQuestion }}</p>
      <p v-else class="question-text">本轮问题已经完成。系统会汇总每题批改结果，生成最终报告。</p>

      <!-- Six-Dimension Scores -->
      <div v-if="currentScores && !streamingQuestion" class="score-chips">
        <span v-if="currentScores.technical" class="score-chip">技术 <strong>{{ currentScores.technical }}</strong></span>
        <span v-if="currentScores.expression" class="score-chip">表达 <strong>{{ currentScores.expression }}</strong></span>
        <span v-if="currentScores.match" class="score-chip">匹配 <strong>{{ currentScores.match }}</strong></span>
        <span v-if="currentScores.problem_solving" class="score-chip">解决 <strong>{{ currentScores.problem_solving }}</strong></span>
        <span v-if="currentScores.follow_up" class="score-chip">追问 <strong>{{ currentScores.follow_up }}</strong></span>
        <span v-if="currentScores.stress_resistance" class="score-chip">抗压 <strong>{{ currentScores.stress_resistance }}</strong></span>
      </div>

      <!-- Answer Input -->
      <div v-if="!interviewFinished" class="answer-area">
        <textarea
          ref="answerTextarea"
          :value="answerText"
          placeholder="像真实面试一样回答即可，系统会记录问题、回答和用时。"
          @input="$emit('update:answerText', $event.target.value)"
        />
        <button
          v-if="speechSupported"
          type="button"
          class="mic-btn"
          :class="{ listening: isListening }"
          :title="isListening ? '停止语音输入' : '语音输入'"
          @click="handleMicClick"
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5z"/>
            <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
          </svg>
        </button>
      </div>

      <!-- Action Buttons -->
      <div class="session-actions">
        <button
          v-if="!interviewFinished"
          type="button"
          class="action-primary"
          :disabled="answerSubmitting || !answerText.trim() || streamingQuestion"
          @click="$emit('submit-answer')"
        >
          {{ answerSubmitting ? '保存中' : streamingQuestion ? 'AI 思考中' : '保存并进入下一题' }}
        </button>
        <button
          v-if="interviewFinished"
          type="button"
          class="action-primary"
          :disabled="reportGenerating"
          @click="$emit('generate-report')"
        >
          {{ reportGenerating ? '生成中...' : '生成报告' }}
        </button>
        <button type="button" class="action-secondary" @click="$emit('reset')">
          退出面试
        </button>
      </div>
    </article>

    <!-- Turn Card -->
    <article v-if="lastTurn && !interviewReport" class="session-stage turn-card">
      <span class="session-eyebrow">答题记录</span>
      <strong>第 {{ lastTurn.questionNo }} 题已保存 · 用时 {{ lastTurn.durationSeconds || 0 }} 秒</strong>
      <p>
        <template v-if="lastTurn.score">本题 {{ lastTurn.score }} 分。</template>
        <template v-if="lastTurn.scores">
          （技术 {{ lastTurn.scores.technical || '-' }} / 表达 {{ lastTurn.scores.expression || '-' }} / 匹配 {{ lastTurn.scores.match || '-' }} / 解决 {{ lastTurn.scores.problem_solving || '-' }} / 追问 {{ lastTurn.scores.follow_up || '-' }} / 抗压 {{ lastTurn.scores.stress_resistance || '-' }}）
        </template>
        {{ lastTurn.aiComment || lastTurn.comment || 'AI 正在后台批改本题，答题过程不会被打断。' }}
      </p>
      <p v-if="lastTurn.followUp" class="follow-up-hint">AI 认为回答不够深入，将进行追问。</p>
    </article>

    <!-- Report Card -->
    <article v-if="interviewReport" class="session-stage report-card">
      <span class="session-eyebrow">报告已保存</span>
      <strong>综合评分：{{ interviewReport.totalScore }} 分</strong>

      <div v-if="reportScores" class="report-scores">
        <div v-for="(val, key) in reportScores" :key="key" class="report-bar">
          <span class="report-bar-label">{{ scoreLabels[key] || key }}</span>
          <el-progress :percentage="val" :stroke-width="12" :format="(p) => p + '分'" />
        </div>
      </div>

      <p v-if="interviewReport.strengths"><strong>亮点：</strong>{{ interviewReport.strengths }}</p>
      <p v-if="interviewReport.weaknesses"><strong>短板：</strong>{{ interviewReport.weaknesses }}</p>
      <p v-if="interviewReport.advice"><strong>建议：</strong>{{ interviewReport.advice }}</p>
      <p v-if="!interviewReport.strengths">{{ interviewReport.reportContent }}</p>

      <div class="session-actions">
        <button type="button" class="action-primary" :disabled="reportRefining || reportOptimized" @click="$emit('refine-report')">
          {{ reportRefining ? '优化中...' : reportOptimized ? '已优化' : 'AI 优化报告' }}
        </button>
      </div>
      <p v-if="reportOptimized" class="optimized-note">优化报告已上传到历史报告。</p>
    </article>

    <p v-if="interviewError" class="error-banner">{{ interviewError }}</p>
  </section>
</template>

<script setup>
import { computed, ref, watch, nextTick, onMounted } from 'vue'
import { useSpeechRecognition } from '../../composables/useSpeechRecognition'
import { useGsapMotion } from '../../composables/useGsapMotion'
import InterviewerSelector from './InterviewerSelector.vue'
import DifficultyIndicator from './DifficultyIndicator.vue'

const emit = defineEmits([
  'generate-report',
  'refine-report',
  'reset',
  'select-interviewer',
  'start',
  'submit-answer',
  'toggle-random-mix',
  'update:adaptiveDifficulty',
  'update:answerText',
  'update:difficulty',
  'update:interviewFocus',
  'update:interviewStyle',
  'update:jdText',
  'update:questionMode',
  'update:selectedDirection',
  'update:use-streaming'
])

const { isListening, isSupported: speechSupported, transcript, toggleListening: toggleSpeechRecognition } = useSpeechRecognition()

const answerTextarea = ref(null)
const rootRef = ref(null)
const startRef = ref(null)
const motion = useGsapMotion(rootRef)

watch(transcript, (newVal) => {
  if (newVal) {
    const current = props.answerText || ''
    const separator = current && !current.endsWith(' ') && !current.endsWith('\n') ? ' ' : ''
    emit('update:answerText', current + separator + newVal)
    nextTick(() => {
      if (answerTextarea.value) {
        answerTextarea.value.scrollTop = answerTextarea.value.scrollHeight
      }
    })
  }
})

function handleMicClick() {
  toggleSpeechRecognition()
}

const scoreLabels = {
  technical: '技术深度',
  expression: '表达逻辑',
  match: '岗位匹配'
}

const props = defineProps({
  answeredCount: { type: Number, default: 0 },
  adaptiveDifficulty: { type: Boolean, default: false },
  answerSubmitting: { type: Boolean, default: false },
  answerText: { type: String, default: '' },
  currentDifficulty: { type: String, default: '标准' },
  currentQuestion: { type: String, default: '' },
  currentScores: { type: Object, default: null },
  difficulty: { type: String, required: true },
  difficultyOptions: { type: Array, required: true },
  focusOptions: { type: Array, required: true },
  interviewFocus: { type: String, required: true },
  interviewStyle: { type: String, required: true },
  interviewSession: { type: Object, default: null },
  interviewError: { type: String, default: '' },
  interviewFinished: { type: Boolean, default: false },
  interviewReport: { type: Object, default: null },
  interviewStarting: { type: Boolean, default: false },
  interviewers: { type: Array, default: () => [] },
  isFollowUp: { type: Boolean, default: false },
  jdText: { type: String, default: '' },
  lastTurn: { type: Object, default: null },
  questionMode: { type: String, required: true },
  randomMixEnabled: { type: Boolean, required: true },
  selectedDirection: { type: String, required: true },
  selectedDirectionLabel: { type: String, required: true },
  selectedInterviewerId: { type: Number, default: null },
  sessionDraftText: { type: String, required: true },
  reportGenerating: { type: Boolean, default: false },
  reportOptimized: { type: Boolean, default: false },
  reportRefining: { type: Boolean, default: false },
  streamingQuestion: { type: Boolean, default: false },
  streamingText: { type: String, default: '' },
  useStreaming: { type: Boolean, default: true }
})

const reportScores = computed(() => {
  if (!props.interviewReport?.scores) return null
  try {
    return typeof props.interviewReport.scores === 'string'
      ? JSON.parse(props.interviewReport.scores)
      : props.interviewReport.scores
  } catch {
    return null
  }
})

onMounted(() => {
  motion.animateIn('.config-left > *', { stagger: 0.08, delay: 0.1 })
  motion.animateIn('.config-right > *', { stagger: 0.06, delay: 0.15 })
  if (startRef.value) motion.fadeUp(startRef.value, { delay: 0.4 })
})
</script>

<style scoped>
.aurora-interview-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ---- Header ---- */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 18px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-default);
}

.panel-eyebrow {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.panel-title {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
}

/* ---- Two-Column Layout ---- */
.config-layout {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 28px;
}

.config-left,
.config-right {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ---- Mix Toggle ---- */
.mix-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 14px;
  margin-bottom: 20px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.02);
}

.mix-toggle div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mix-toggle strong {
  font-size: 14px;
  color: var(--text-primary);
}

.mix-toggle span {
  color: var(--text-muted);
  font-size: 12px;
}

.mix-btn {
  min-width: 80px;
  height: 36px;
  color: #475569;
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: 999px;
  font-weight: 600;
  font-size: 13px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.mix-btn:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.mix-btn.active {
  color: var(--text-inverse);
  background: #1e40af;
  border-color: #1e40af;
}

/* ---- Form ---- */
.config-form {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.02);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-full {
  grid-column: 1 / -1;
}

.form-label {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.form-field textarea {
  min-height: 80px;
  padding: 14px;
  color: #1e293b;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.form-field textarea::placeholder {
  color: var(--text-muted);
}

.form-field textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.06);
}

.form-row {
  grid-column: 1 / -1;
  display: flex;
  gap: 24px;
}

.form-inline {
  flex: 1;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: 12px;
}

/* ---- Start Row ---- */
.start-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.02);
}

.start-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.start-eyebrow {
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.start-summary strong {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

.start-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
  height: 48px;
  padding: 0 28px;
  color: var(--text-inverse);
  background: #1e40af;
  border: 0;
  border-radius: 12px;
  font-weight: 700;
  font-size: 15px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.15);
}

.start-btn:hover:not(:disabled) {
  background: #1d4ed8;
  box-shadow: 0 8px 24px rgba(30, 64, 175, 0.25);
  transform: translateY(-1px);
}

.start-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.start-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ---- Session Stage ---- */
.session-stage {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
  padding: 28px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.015);
  overflow: hidden;
}

.session-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.session-eyebrow {
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.follow-up-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--primary-surface);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #bfdbfe;
}

.streaming-block {
  position: relative;
}

.streaming-block p {
  margin: 0;
  white-space: pre-wrap;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.8;
}

.cursor-blink {
  display: inline-block;
  animation: blink 0.8s infinite;
  color: #2563eb;
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.question-text {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.8;
}

/* Score Chips */
.score-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.score-chip {
  padding: 5px 14px;
  border-radius: 999px;
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  font-size: 13px;
  color: #475569;
}

.score-chip strong {
  color: #1e40af;
  margin-left: 4px;
  font-weight: 700;
}

/* Answer Area */
.answer-area {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.answer-area textarea {
  flex: 1;
  min-height: 150px;
  padding: 18px;
  color: #1e293b;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 14px;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.8;
  resize: vertical;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.answer-area textarea::placeholder {
  color: var(--text-muted);
}

.answer-area textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.06);
}

.mic-btn {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.mic-btn:hover {
  color: #2563eb;
  border-color: #93c5fd;
}

.mic-btn.listening {
  color: #ef4444;
  border-color: #ef4444;
  background: #fef2f2;
  animation: mic-pulse 1.2s ease-in-out infinite;
}

@keyframes mic-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.2); }
  50% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
}

/* Actions */
.session-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-primary {
  min-width: 120px;
  height: 44px;
  padding: 0 24px;
  color: var(--text-inverse);
  background: #1e40af;
  border: 0;
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.12);
}

.action-primary:hover:not(:disabled) {
  background: #1d4ed8;
  box-shadow: 0 6px 20px rgba(30, 64, 175, 0.2);
  transform: translateY(-1px);
}

.action-primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.action-secondary {
  min-width: 100px;
  height: 44px;
  padding: 0 20px;
  color: #475569;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-secondary:hover {
  border-color: var(--border-strong);
  color: var(--text-primary);
  background: var(--bg-surface);
}

/* Turn Card */
.turn-card strong {
  color: var(--text-primary);
  font-size: 16px;
}

.follow-up-hint {
  color: #2563eb;
  font-size: 13px;
}

/* Report Card */
.report-card strong {
  color: var(--text-primary);
  font-size: 18px;
}

.report-card p {
  max-height: 280px;
  overflow: auto;
  padding: 16px;
  white-space: pre-wrap;
  color: #1e293b;
  background: var(--bg-surface);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  line-height: 1.8;
  margin: 0;
}

.report-scores {
  margin: 16px 0;
}

.report-bar {
  margin-bottom: 10px;
}

.report-bar-label {
  display: inline-block;
  width: 72px;
  font-size: 13px;
  color: #475569;
}

.optimized-note {
  color: #059669;
  font-size: 13px;
  margin: 0;
}

/* Error */
.error-banner {
  margin: 16px 0 0;
  padding: 12px 16px;
  color: #ef4444;
  font-size: 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

/* ---- Responsive ---- */
@media (max-width: 920px) {
  .config-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .start-row {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .start-btn {
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .config-form {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .form-row {
    flex-direction: column;
  }

  .session-stage {
    padding: 20px;
  }

  .answer-area {
    flex-direction: column;
  }

  .mic-btn {
    align-self: flex-end;
  }
}
</style>
