<template>
  <section class="focus-panel report-view">
    <div class="panel-topline">
      <span>History</span>
      <strong>{{ selectedReport ? '报告详情' : '历史报告' }}</strong>
    </div>

    <article v-if="selectedReport" class="report-detail">
      <div class="report-detail-head">
        <button type="button" class="secondary-text" @click="$emit('back-list')">返回历史报告</button>
        <div class="report-detail-actions">
          <button type="button" class="danger-text" @click="$emit('delete-report', selectedReport.sessionId)">删除报告</button>
          <button type="button" @click="$emit('view-replay', selectedReport.sessionId)">回放</button>
          <button type="button" @click="$emit('download-report', selectedReport)">下载报告</button>
        </div>
      </div>

      <div class="report-score">
        <span>综合评分</span>
        <strong>{{ selectedReport.totalScore ?? '-' }}</strong>
      </div>
      <div class="report-meta">
        <span>{{ selectedReport.updatedAt || '暂无时间' }}</span>
        <span>{{ selectedReport.summary || '模拟面试报告' }}</span>
      </div>

      <!-- 雷达图 + 分项评分 -->
      <div v-if="reportScores" class="report-scores-section">
        <h4>分项评分</h4>
        <div class="report-scores-visual">
          <ScoreRadar :scores="reportScores" />
          <div class="report-scores-grid">
            <div v-for="(val, key) in reportScores" :key="key" class="report-score-item">
              <span class="score-label">{{ scoreLabels[key] || key }}</span>
              <el-progress
                :percentage="val"
                :stroke-width="14"
                :color="val >= 75 ? '#88b498' : val >= 60 ? '#d4a36a' : '#d07868'"
                :format="(p) => p + '分'"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 结构化内容 -->
      <div v-if="selectedReport.strengths" class="report-section">
        <h4>表现亮点</h4>
        <p>{{ selectedReport.strengths }}</p>
      </div>
      <div v-if="selectedReport.weaknesses" class="report-section">
        <h4>主要短板</h4>
        <p>{{ selectedReport.weaknesses }}</p>
      </div>
      <div v-if="selectedReport.advice" class="report-section">
        <h4>训练建议</h4>
        <p>{{ selectedReport.advice }}</p>
      </div>

      <pre class="report-content">{{ selectedReport.reportContent }}</pre>
    </article>

    <template v-else>
      <!-- 趋势图 -->
      <ScoreTrend v-if="reportHistory.length" :history="reportHistory" />

      <article v-if="!reportHistory.length" class="report-empty">
        <strong>还没有历史报告。</strong>
        <p>完成一场模拟面试并生成报告后，这里会显示报告时间、分数和面试配置。</p>
        <div class="report-actions">
          <button
            type="button"
            :disabled="!interviewSession || reportGenerating"
            @click="$emit('generate-report')"
          >
            {{ reportGenerating ? '生成中' : '生成当前报告' }}
          </button>
        </div>
      </article>

      <div v-else class="report-list">
        <article
          v-for="report in reportHistory"
          :key="report.sessionId"
          class="report-row"
        >
          <div class="report-row-score">
            <span>评分</span>
            <strong>{{ report.totalScore ?? '-' }}</strong>
          </div>
          <div class="report-row-main">
            <strong>{{ report.summary || '模拟面试报告' }}</strong>
            <span>{{ report.updatedAt || '暂无时间' }}</span>
          </div>
          <div class="report-row-actions">
            <button type="button" :disabled="reportLoading" @click="$emit('view-report', report.sessionId)">
              查看
            </button>
            <button type="button" class="danger-text" :disabled="reportLoading" @click="$emit('delete-report', report.sessionId)">
              删除
            </button>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import ScoreRadar from './ScoreRadar.vue'
import ScoreTrend from './ScoreTrend.vue'

const scoreLabels = {
  technical: '技术深度',
  expression: '表达逻辑',
  match: '岗位匹配',
  problem_solving: '问题解决',
  follow_up: '追问表现',
  stress_resistance: '抗压能力'
}

const props = defineProps({
  interviewSession: {
    type: Object,
    default: null
  },
  reportGenerating: {
    type: Boolean,
    default: false
  },
  reportHistory: {
    type: Array,
    default: () => []
  },
  reportLoading: {
    type: Boolean,
    default: false
  },
  selectedReport: {
    type: Object,
    default: null
  }
})

const reportScores = computed(() => {
  if (!props.selectedReport?.scores) return null
  try {
    return typeof props.selectedReport.scores === 'string'
      ? JSON.parse(props.selectedReport.scores)
      : props.selectedReport.scores
  } catch {
    return null
  }
})

defineEmits(['back-list', 'delete-report', 'download-report', 'generate-report', 'view-report', 'view-replay'])
</script>

<style scoped>
.report-scores-section {
  margin: 16px 0;
  padding: 20px 24px;
  background: rgba(39, 37, 47, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
}

.report-scores-section h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--gold);
  font-weight: 600;
}

.report-scores-visual {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  align-items: center;
}

@media (max-width: 720px) {
  .report-scores-visual {
    grid-template-columns: 1fr;
  }
}

.report-scores-grid {
  display: grid;
  gap: 16px;
}

.report-score-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.report-score-item .score-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
}

.report-section {
  margin: 16px 0;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  background: rgba(35, 34, 41, 0.5);
  border: 1px solid var(--border-subtle);
}

.report-section h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: var(--gold);
  font-weight: 600;
}

.report-section p {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.report-content {
  margin-top: 16px;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  background: rgba(35, 34, 41, 0.5);
  border: 1px solid var(--border-subtle);
  padding: 20px;
  border-radius: var(--radius-md);
}
</style>
