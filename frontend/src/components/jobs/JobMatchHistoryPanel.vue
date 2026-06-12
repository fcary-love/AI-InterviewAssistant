<template>
  <section class="focus-panel job-history-panel">
    <div class="panel-topline">
      <span>Job Archive</span>
      <strong>{{ selected ? 'JD 匹配档案详情' : 'JD 匹配记录' }}</strong>
    </div>

    <article v-if="error" class="job-history-notice">
      {{ error }}
    </article>

    <article v-if="!loading && !records.length" class="job-history-empty">
      <span>还没有岗位记录</span>
      <strong>每一次 JD 匹配，都会沉淀成可复用的求职线索。</strong>
      <p>先上传简历，再粘贴或上传岗位 JD。系统会保存匹配分、岗位要求、缺口关键词和修改建议，后续可以直接拿回来继续咨询。</p>
      <button type="button" @click="$emit('open-coach')">去做一次岗位匹配</button>
    </article>

    <section v-else class="job-history-layout">
      <aside class="job-history-list" aria-label="JD 匹配记录列表">
        <div class="job-history-summary">
          <span>总记录</span>
          <strong>{{ records.length }}</strong>
          <small>平均匹配 {{ averageScore }} 分</small>
        </div>

        <section class="job-history-compare" aria-label="岗位匹配分对比">
          <div class="job-history-compare-head">
            <span>Score Compare</span>
            <small>最近 {{ records.length }} 次</small>
          </div>
          <button
            v-for="item in records"
            :key="`bar-${item.id}`"
            type="button"
            class="job-history-bar"
            :class="{ active: selected?.id === item.id }"
            @click="viewDetail(item.id)"
          >
            <span>{{ item.matchScore ?? 0 }}</span>
            <i :style="{ width: scoreWidth(item.matchScore) }"></i>
          </button>
        </section>

        <button
          v-for="item in records"
          :key="item.id"
          type="button"
          class="job-history-row"
          :class="{ active: selected?.id === item.id }"
          :disabled="loadingDetail"
          @click="viewDetail(item.id)"
        >
          <span>匹配分 {{ item.matchScore ?? '-' }}</span>
          <strong>{{ item.jdExcerpt || '岗位 JD 记录' }}</strong>
          <small>{{ item.createdAt || '暂无时间' }}</small>
        </button>
      </aside>

      <article class="job-history-detail">
        <div v-if="loading && !selected" class="job-history-placeholder">
          正在读取岗位记录。
        </div>

        <div v-else-if="!selected" class="job-history-placeholder">
          <strong>选择一条记录查看详情。</strong>
          <p>你可以回看当时的岗位要求、匹配分和简历修改建议，也可以把这份 JD 带回咨询区继续追问。</p>
        </div>

        <template v-else>
          <div class="job-history-detail-head">
            <div class="job-history-score">
              <span>匹配分</span>
              <strong>{{ selected.matchScore ?? '-' }}</strong>
            </div>
            <div>
              <span>匹配材料</span>
              <strong>{{ selected.resumeFileName || '简历材料' }}</strong>
              <small>{{ selected.createdAt || '暂无时间' }}</small>
            </div>
            <div class="job-history-actions">
              <button type="button" @click="$emit('use-match', selected)">带回咨询区</button>
              <button type="button" @click="$emit('start-interview', selected)">根据该 JD 面试</button>
              <button type="button" class="ghost" @click="downloadReport">导出报告</button>
            </div>
          </div>

          <section class="job-history-brief">
            <article>
              <span>最高匹配</span>
              <strong>{{ bestRecord ? `${bestRecord.matchScore} 分` : '-' }}</strong>
              <small>{{ bestRecord?.jdExcerpt || '暂无记录' }}</small>
            </article>
            <article>
              <span>当前位置</span>
              <strong>{{ currentRankText }}</strong>
              <small>用于观察不同 JD 的投递优先级</small>
            </article>
            <article>
              <span>低分提醒</span>
              <strong>{{ weakestRecord ? `${weakestRecord.matchScore} 分` : '-' }}</strong>
              <small>{{ weakestRecord?.jdExcerpt || '暂无记录' }}</small>
            </article>
          </section>

          <section class="job-history-sections">
            <article>
              <span>岗位核心要求</span>
              <ol>
                <li v-for="item in selected.coreRequirements" :key="item">{{ item }}</li>
                <li v-if="!selected.coreRequirements?.length">暂无结构化要求。</li>
              </ol>
            </article>
            <article>
              <span>简历已覆盖</span>
              <div class="job-history-tags">
                <small v-for="item in selected.matchedKeywords" :key="item">{{ item }}</small>
                <small v-if="!selected.matchedKeywords?.length">暂无明显覆盖关键词</small>
              </div>
            </article>
            <article>
              <span>需要补强</span>
              <div class="job-history-tags warn">
                <small v-for="item in selected.missingKeywords" :key="item">{{ item }}</small>
                <small v-if="!selected.missingKeywords?.length">暂无明显缺口</small>
              </div>
            </article>
            <article>
              <span>面试追问方向</span>
              <ol>
                <li v-for="item in selected.interviewFocus" :key="item">{{ item }}</li>
                <li v-if="!selected.interviewFocus?.length">暂无追问方向。</li>
              </ol>
            </article>
          </section>

          <section class="job-history-analysis">
            <span>完整分析</span>
            <pre>{{ selected.analysisContent || '暂无分析内容。' }}</pre>
          </section>

          <section class="job-history-analysis">
            <span>原始 JD</span>
            <pre>{{ selected.jdText || '暂无 JD 内容。' }}</pre>
          </section>
        </template>
      </article>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchJobMatchDetail, fetchJobMatches } from '../../api/jobs'

const emit = defineEmits(['open-coach', 'start-interview', 'use-match'])

const error = ref('')
const loading = ref(false)
const loadingDetail = ref(false)
const records = ref([])
const selected = ref(null)

const averageScore = computed(() => {
  const scores = records.value
    .map((item) => Number(item.matchScore))
    .filter((score) => Number.isFinite(score))
  if (!scores.length) {
    return 0
  }
  return Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
})

const bestRecord = computed(() => {
  return [...records.value].sort((a, b) => Number(b.matchScore || 0) - Number(a.matchScore || 0))[0] || null
})

const weakestRecord = computed(() => {
  return [...records.value].sort((a, b) => Number(a.matchScore || 0) - Number(b.matchScore || 0))[0] || null
})

const currentRankText = computed(() => {
  if (!selected.value) {
    return '-'
  }
  const sorted = [...records.value].sort((a, b) => Number(b.matchScore || 0) - Number(a.matchScore || 0))
  const index = sorted.findIndex((item) => item.id === selected.value.id)
  return index >= 0 ? `第 ${index + 1} / ${sorted.length}` : '-'
})

onMounted(loadRecords)

async function loadRecords() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchJobMatches(50)
    records.value = response.data || []
    if (records.value.length) {
      await viewDetail(records.value[0].id)
    }
  } catch (err) {
    error.value = err?.response?.data?.message || err.message || '岗位记录加载失败'
  } finally {
    loading.value = false
  }
}

async function viewDetail(id) {
  if (!id) {
    return
  }
  loadingDetail.value = true
  error.value = ''
  try {
    const response = await fetchJobMatchDetail(id)
    selected.value = response.data
  } catch (err) {
    error.value = err?.response?.data?.message || err.message || '岗位详情加载失败'
  } finally {
    loadingDetail.value = false
  }
}

function scoreWidth(score) {
  const value = Math.max(4, Math.min(100, Number(score) || 0))
  return `${value}%`
}

function downloadReport() {
  if (!selected.value) {
    return
  }
  const detail = selected.value
  const content = [
    'JD 匹配报告',
    '',
    `匹配分：${detail.matchScore ?? '-'} 分`,
    `匹配材料：${detail.resumeFileName || '简历材料'}`,
    `生成时间：${detail.createdAt || '-'}`,
    '',
    '岗位核心要求',
    ...(detail.coreRequirements?.length ? detail.coreRequirements.map((item, index) => `${index + 1}. ${item}`) : ['暂无结构化要求']),
    '',
    '简历已覆盖关键词',
    detail.matchedKeywords?.length ? detail.matchedKeywords.join('、') : '暂无明显覆盖关键词',
    '',
    '需要补强关键词',
    detail.missingKeywords?.length ? detail.missingKeywords.join('、') : '暂无明显缺口',
    '',
    '面试追问方向',
    ...(detail.interviewFocus?.length ? detail.interviewFocus.map((item, index) => `${index + 1}. ${item}`) : ['暂无追问方向']),
    '',
    '完整分析',
    detail.analysisContent || '暂无分析内容。',
    '',
    '原始 JD',
    detail.jdText || '暂无 JD 内容。'
  ].join('\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `JD匹配报告-${detail.id || Date.now()}.txt`
  link.click()
  URL.revokeObjectURL(url)
}
</script>
