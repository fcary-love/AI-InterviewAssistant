<template>
  <article class="job-match-panel">
    <section class="job-match-entry">
      <div class="job-match-intro">
        <span>Job Match</span>
        <strong>岗位简报</strong>
        <p>粘贴目标岗位 JD，系统会结合当前简历判断匹配度，并给出可直接改进的简历表达。</p>
        <small>{{ resumeReady ? '简历已就绪' : '请先上传简历' }}</small>
      </div>

      <div class="job-match-editor">
        <div class="job-match-editor-head">
          <strong>目标岗位 JD</strong>
          <div class="job-match-tools">
            <span>可粘贴文字，也可上传招聘截图</span>
            <input
              ref="jdImageInput"
              class="hidden-input"
              type="file"
              accept="image/png,image/jpeg,image/jpg,image/bmp,image/webp"
              @change="$emit('jd-image-change', $event)"
            />
            <button
              class="jd-image-button"
              type="button"
              :disabled="jdImageLoading || loading"
              @click="jdImageInput?.click()"
            >
              {{ jdImageLoading ? '识别中' : '上传 JD 图片' }}
            </button>
          </div>
        </div>

        <el-input
          :model-value="jdText"
          type="textarea"
          :autosize="{ minRows: 5, maxRows: 8 }"
          placeholder="例如：岗位职责、任职要求、技术栈、加分项..."
          @update:model-value="$emit('update:jdText', $event)"
        />

        <div class="job-match-actions">
          <span>{{ jdText.trim() ? `${jdText.trim().length} 字` : '等待 JD' }}</span>
          <button
            type="button"
            :disabled="!resumeReady || !jdText.trim() || loading"
            @click="$emit('analyze')"
          >
            {{ loading ? '分析中' : '开始匹配' }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="result" class="job-match-brief">
      <div class="match-score-card">
        <span>匹配分</span>
        <strong>{{ result.matchScore ?? '-' }}</strong>
        <p>{{ scoreHint }}</p>
      </div>

      <div class="match-brief-grid">
        <article>
          <span>已覆盖</span>
          <strong>{{ result.matchedKeywords?.length || 0 }} 个关键词</strong>
          <div class="match-tags">
            <small v-for="item in limited(result.matchedKeywords, 8)" :key="item">{{ item }}</small>
            <small v-if="!result.matchedKeywords?.length">等待分析</small>
          </div>
        </article>
        <article>
          <span>待补强</span>
          <strong>{{ result.missingKeywords?.length || 0 }} 个缺口</strong>
          <div class="match-tags warn">
            <small v-for="item in limited(result.missingKeywords, 8)" :key="item">{{ item }}</small>
            <small v-if="!result.missingKeywords?.length">暂无明显缺口</small>
          </div>
        </article>
      </div>

      <div class="match-detail-grid">
        <article>
          <span>岗位核心要求</span>
          <ol>
            <li v-for="item in limited(result.coreRequirements, 5)" :key="item">{{ item }}</li>
            <li v-if="!result.coreRequirements?.length">暂无结构化要求，建议补充更完整 JD。</li>
          </ol>
        </article>
        <article>
          <span>简历修改方向</span>
          <ol>
            <li v-for="item in limited(result.rewriteSuggestions, 5)" :key="item">{{ item }}</li>
          </ol>
        </article>
        <article>
          <span>面试追问重点</span>
          <ol>
            <li v-for="item in limited(result.interviewFocus, 5)" :key="item">{{ item }}</li>
          </ol>
        </article>
      </div>
    </section>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  jdText: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  jdImageLoading: {
    type: Boolean,
    default: false
  },
  resumeReady: {
    type: Boolean,
    default: false
  },
  result: {
    type: Object,
    default: null
  }
})

defineEmits(['analyze', 'jd-image-change', 'update:jdText'])

const jdImageInput = ref(null)

const scoreHint = computed(() => {
  const score = props.result?.matchScore
  if (score == null) {
    return '等待评分'
  }
  if (score >= 80) {
    return '匹配度较高，可以重点优化表达和证据。'
  }
  if (score >= 60) {
    return '有一定匹配，需要补强关键缺口。'
  }
  return '当前差距明显，建议先调整投递方向或补充项目证明。'
})

function limited(items = [], count = 5) {
  return items.filter(Boolean).slice(0, count)
}
</script>
