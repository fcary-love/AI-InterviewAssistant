<template>
  <section class="focus-panel replay-view">
    <div class="panel-topline">
      <span>Replay</span>
      <strong>面试回放</strong>
    </div>

    <div class="replay-head">
      <button type="button" class="secondary-text" @click="$emit('close')">返回报告</button>
      <span v-if="turns.length" class="replay-count">共 {{ turns.length }} 题</span>
    </div>

    <div v-if="loading" class="replay-loading">加载中...</div>

    <div v-else-if="!turns.length" class="replay-empty">
      <strong>暂无答题记录</strong>
      <p>这场面试还没有回答记录。</p>
    </div>

    <template v-else>
      <!-- 时间分布图 -->
      <TimeDistributionChart :analysis="timeAnalysis" :turns="turns" />

      <!-- 关键词标注按钮 -->
      <div class="replay-actions">
        <button type="button" class="annotate-btn" :disabled="annotating" @click="handleAnnotate">
          {{ annotating ? '标注中...' : keywordAnnotations.length ? '重新标注' : 'AI 关键词标注' }}
        </button>
      </div>

      <!-- 逐题回放 -->
      <div class="replay-timeline">
        <article v-for="turn in turns" :key="turn.questionNo" class="replay-turn">
          <div class="turn-header">
            <span class="turn-badge">{{ turn.questionNo }}</span>
            <span class="turn-duration">{{ turn.durationSeconds || 0 }}s</span>
            <span class="turn-score" :class="scoreClass(turn.score)">
              {{ turn.score ?? '-' }}分
            </span>
          </div>

          <div class="turn-question">
            <strong>问题</strong>
            <p>{{ turn.question }}</p>
          </div>

          <div class="turn-answer">
            <strong>我的回答</strong>
            <p>{{ turn.answer || '未作答' }}</p>
          </div>

          <!-- 关键词标注 -->
          <div v-if="getTurnKeywords(turn.questionNo).length" class="turn-keywords">
            <strong>关键词</strong>
            <div class="keyword-tags">
              <span
                v-for="kw in getTurnKeywords(turn.questionNo)"
                :key="kw.keyword"
                class="keyword-tag"
                :class="[kw.hit ? 'hit' : 'miss', kw.importance]"
              >
                {{ kw.hit ? '✓' : '✗' }} {{ kw.keyword }}
              </span>
            </div>
          </div>

          <div v-if="turn.aiComment" class="turn-comment">
            <strong>AI 点评</strong>
            <p>{{ turn.aiComment }}</p>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchInterviewTurns, annotateKeywords, fetchKeywords, fetchTimeAnalysis } from '../../api/interviews'
import TimeDistributionChart from '../report/TimeDistributionChart.vue'

const props = defineProps({
  sessionId: { type: String, required: true }
})

defineEmits(['close'])

const turns = ref([])
const loading = ref(false)
const annotating = ref(false)
const keywordAnnotations = ref([])
const timeAnalysis = ref(null)

function scoreClass(score) {
  if (score == null) return 'score-none'
  if (score < 60) return 'score-low'
  if (score < 75) return 'score-mid'
  return 'score-high'
}

function getTurnKeywords(questionNo) {
  return keywordAnnotations.value.filter(k => k.questionNo === questionNo)
}

async function handleAnnotate() {
  annotating.value = true
  try {
    const res = await annotateKeywords(props.sessionId)
    keywordAnnotations.value = res.data || []
  } catch {
    // ignore
  } finally {
    annotating.value = false
  }
}

onMounted(async () => {
  if (!props.sessionId) return
  loading.value = true
  try {
    const [turnsRes, timeRes, kwRes] = await Promise.allSettled([
      fetchInterviewTurns(props.sessionId),
      fetchTimeAnalysis(props.sessionId),
      fetchKeywords(props.sessionId)
    ])
    turns.value = turnsRes.status === 'fulfilled' ? (turnsRes.value.data || []) : []
    timeAnalysis.value = timeRes.status === 'fulfilled' ? timeRes.value.data : null
    keywordAnnotations.value = kwRes.status === 'fulfilled' ? (kwRes.value.data || []) : []
  } catch {
    turns.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.replay-actions {
  margin: 16px 0;
  display: flex;
  gap: 10px;
}

.annotate-btn {
  padding: 8px 18px;
  color: var(--gold);
  background: var(--gold-surface);
  border: 1px solid var(--gold-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast);
}

.annotate-btn:hover:not(:disabled) {
  background: var(--gold-glow);
}

.annotate-btn:disabled {
  opacity: 0.5;
  cursor: wait;
}

/* 关键词标注 */
.turn-keywords {
  border-top: 1px solid var(--border-subtle);
  padding-top: 12px;
}

.turn-keywords strong {
  display: block;
  font-size: 12px;
  color: var(--gold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 8px;
  opacity: 0.7;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}

.keyword-tag.hit {
  color: var(--success);
  background: var(--success-surface);
  border: 1px solid rgba(136, 180, 136, 0.25);
}

.keyword-tag.miss {
  color: var(--danger);
  background: var(--danger-surface);
  border: 1px solid rgba(200, 112, 96, 0.2);
}

.keyword-tag.critical {
  font-weight: 700;
}

.keyword-tag.minor {
  opacity: 0.7;
}
</style>
