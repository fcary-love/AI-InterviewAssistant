<template>
  <section class="focus-panel question-bank-panel">
    <div class="panel-topline">
      <span>Question Bank</span>
      <strong>{{ loading ? '读取中' : '题库中心' }}</strong>
    </div>

    <section class="question-bank-hero">
      <div>
        <span>Total</span>
        <strong>{{ overview?.total || 0 }} 道题</strong>
        <p>把常考八股、项目追问、系统设计和错题复盘放在一起，面试训练不再只靠随机问。</p>
      </div>
      <article>
        <span>Wrong Book</span>
        <strong>{{ wrongQuestions.length }} 条错题</strong>
        <p>来自历史模拟面试中低于 70 分的回答，可继续追问、重练和复盘。</p>
      </article>
    </section>

    <section class="question-filters">
      <label>
        <span>方向</span>
        <select v-model="filters.direction" @change="loadQuestions">
          <option value="">全部方向</option>
          <option v-for="item in overview?.directions || []" :key="item.name" :value="item.name">
            {{ item.name }}（{{ item.count }}）
          </option>
        </select>
      </label>
      <label>
        <span>分类</span>
        <select v-model="filters.category" @change="loadQuestions">
          <option value="">全部分类</option>
          <option v-for="item in overview?.categories || []" :key="item.name" :value="item.name">
            {{ item.name }}（{{ item.count }}）
          </option>
        </select>
      </label>
      <label>
        <span>难度</span>
        <select v-model="filters.difficulty" @change="loadQuestions">
          <option value="">全部难度</option>
          <option v-for="item in overview?.difficulties || []" :key="item.name" :value="item.name">
            {{ item.name }}（{{ item.count }}）
          </option>
        </select>
      </label>
      <label class="question-keyword">
        <span>搜索</span>
        <input
          v-model.trim="filters.keyword"
          type="search"
          placeholder="搜 Redis、事务、Vue3、项目..."
          @keydown.enter="loadQuestions"
        >
      </label>
      <button type="button" @click="loadQuestions">筛选</button>
    </section>

    <section class="question-layout">
      <article class="question-column">
        <div class="question-section-head">
          <span>Practice Pool</span>
          <strong>题目列表</strong>
        </div>

        <div v-if="questions.length" class="question-list">
          <section v-for="question in questions" :key="question.id" class="question-card">
            <div class="question-card-head">
              <div>
                <span>{{ question.direction }} / {{ question.category }}</span>
                <strong>{{ question.questionText }}</strong>
              </div>
              <small>{{ question.difficulty }}</small>
            </div>
            <details>
              <summary>查看参考思路</summary>
              <p>{{ question.referenceAnswer }}</p>
              <a v-if="question.sourceUrl" :href="question.sourceUrl" target="_blank" rel="noreferrer">
                来源：{{ question.sourceName || '公开资料' }}
              </a>
            </details>
            <button type="button" @click="askQuestion(question.questionText)">拿这题问助手</button>
          </section>
        </div>
        <p v-else class="question-empty">暂时没有匹配的题目，换个分类或关键词试试。</p>
      </article>

      <aside class="wrong-column">
        <div class="question-section-head">
          <span>Review</span>
          <strong>错题本</strong>
        </div>

        <div v-if="wrongQuestions.length" class="wrong-list">
          <section
            v-for="item in wrongQuestions"
            :key="item.sessionId + item.questionNo"
            class="wrong-card"
            :class="{ 'wrong-reviewed': item.reviewed }"
          >
            <div class="wrong-score">
              <span>得分</span>
              <strong>{{ item.score }}</strong>
            </div>
            <div>
              <small>{{ item.createdAt }} / 第 {{ item.questionNo }} 题</small>
              <strong>{{ item.question }}</strong>
              <p>{{ item.aiComment || '还没有点评。' }}</p>
              <div class="wrong-actions">
                <button type="button" @click="askWrongQuestion(item)">让助手讲这道错题</button>
                <button
                  v-if="!item.reviewed"
                  type="button"
                  class="btn-reviewed"
                  @click="handleMarkReviewed(item)"
                >已掌握</button>
                <span v-else class="reviewed-badge">已掌握</span>
              </div>
            </div>
          </section>
        </div>
        <p v-else class="question-empty">暂时没有低分错题。完成几轮模拟面试后，这里会自动沉淀。</p>
      </aside>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchQuestionOverview, fetchQuestions, fetchWrongQuestions, markQuestionReviewed } from '../../api/questions'

const emit = defineEmits(['ask-question'])

const overview = ref(null)
const questions = ref([])
const wrongQuestions = ref([])
const loading = ref(false)
const filters = reactive({
  direction: '',
  category: '',
  difficulty: '',
  keyword: ''
})

onMounted(() => {
  loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    const [overviewResponse, questionsResponse, wrongResponse] = await Promise.all([
      fetchQuestionOverview(),
      fetchQuestions({ limit: 80 }),
      fetchWrongQuestions({ limit: 30 })
    ])
    overview.value = overviewResponse.data
    questions.value = questionsResponse.data || []
    wrongQuestions.value = wrongResponse.data || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '读取题库失败')
  } finally {
    loading.value = false
  }
}

async function loadQuestions() {
  loading.value = true
  try {
    const response = await fetchQuestions({
      direction: filters.direction || undefined,
      category: filters.category || undefined,
      difficulty: filters.difficulty || undefined,
      keyword: filters.keyword || undefined,
      limit: 120
    })
    questions.value = response.data || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '筛选题库失败')
  } finally {
    loading.value = false
  }
}

function askQuestion(questionText) {
  emit('ask-question', `请用面试官能听懂的方式讲解这道题，并给我一版适合面试回答的答案：${questionText}`)
}

function askWrongQuestion(item) {
  emit('ask-question', `请帮我复盘这道错题。题目：${item.question}。我的回答：${item.answer || '未填写'}。历史点评：${item.aiComment || '无'}。请告诉我为什么丢分、正确思路和一版更好的回答。`)
}

async function handleMarkReviewed(item) {
  try {
    await markQuestionReviewed(item.sessionId, item.questionNo)
    item.reviewed = true
    ElMessage.success('已标记为掌握')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '标记失败')
  }
}
</script>
