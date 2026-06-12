<template>
  <section class="focus-panel training-panel">
    <div class="panel-topline">
      <span>Training Plan</span>
      <strong>{{ loading ? '同步中' : '训练计划' }}</strong>
    </div>

    <section class="training-hero">
      <div class="training-score-card">
        <span>完成度</span>
        <strong>{{ overview?.completionRate || 0 }}%</strong>
        <p>{{ overview?.latestAction || '先生成一份训练计划' }}</p>
      </div>
      <div class="training-overview-grid">
        <article>
          <span>全部任务</span>
          <strong>{{ overview?.total || 0 }}</strong>
        </article>
        <article>
          <span>待开始</span>
          <strong>{{ overview?.todo || 0 }}</strong>
        </article>
        <article>
          <span>进行中</span>
          <strong>{{ overview?.doing || 0 }}</strong>
        </article>
        <article>
          <span>已完成</span>
          <strong>{{ overview?.done || 0 }}</strong>
        </article>
      </div>
    </section>

    <section class="training-command">
      <div>
        <span>Plan Builder</span>
        <strong>根据错题和低分项生成训练任务</strong>
        <p>如果还没有模拟面试记录，系统会先给一份通用入门路线；有历史数据后，会优先围绕薄弱分类生成。</p>
      </div>
      <button type="button" :disabled="generating" @click="handleGenerate">
        {{ generating ? '生成中' : '生成训练计划' }}
      </button>
    </section>

    <section v-if="overview?.weakCategories?.length" class="weak-strip">
      <span>薄弱方向</span>
      <button
        v-for="category in overview.weakCategories"
        :key="category"
        type="button"
        @click="$emit('open-question-bank', category)"
      >
        {{ category }}
      </button>
    </section>

    <section class="training-board">
      <article
        v-for="column in columns"
        :key="column.status"
        class="training-column"
      >
        <div class="training-column-head">
          <span>{{ column.label }}</span>
          <strong>{{ tasksByStatus[column.status]?.length || 0 }}</strong>
        </div>

        <div v-if="tasksByStatus[column.status]?.length" class="training-task-list">
          <section
            v-for="task in tasksByStatus[column.status]"
            :key="task.id"
            class="training-task-card"
          >
            <div class="training-task-head">
              <span>{{ task.category || task.taskType }}</span>
              <small>{{ task.dueDate ? `截止 ${task.dueDate}` : '灵活安排' }}</small>
            </div>
            <strong>{{ task.title }}</strong>
            <p>{{ task.description }}</p>
            <div class="training-progress">
              <span>{{ task.finishedCount || 0 }} / {{ task.targetCount || 1 }}</span>
              <i :style="{ width: progressWidth(task) }"></i>
            </div>
            <div class="training-actions">
              <button
                v-if="task.status !== 'DOING'"
                type="button"
                @click="handleStatus(task, 'DOING')"
              >
                开始
              </button>
              <button
                v-if="task.status !== 'DONE'"
                type="button"
                @click="handleStatus(task, 'DONE')"
              >
                完成
              </button>
              <button
                v-if="task.status !== 'TODO'"
                type="button"
                class="ghost"
                @click="handleStatus(task, 'TODO')"
              >
                重置
              </button>
              <button type="button" class="ghost" @click="handleDelete(task)">删除</button>
            </div>
          </section>
        </div>
        <p v-else class="training-empty">{{ column.empty }}</p>
      </article>
    </section>

    <section class="training-next">
      <button type="button" @click="$emit('open-question-bank')">去题库练题</button>
      <button type="button" @click="$emit('open-interview')">开始模拟面试</button>
      <button type="button" @click="$emit('open-coach')">回到咨询修改</button>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteTrainingTask,
  fetchTrainingOverview,
  fetchTrainingTasks,
  generateTrainingPlan,
  updateTrainingTaskStatus
} from '../../api/training'

defineEmits(['open-question-bank', 'open-interview', 'open-coach'])

const loading = ref(false)
const generating = ref(false)
const overview = ref(null)
const tasks = ref([])

const columns = [
  { status: 'TODO', label: '待开始', empty: '还没有待开始的任务。' },
  { status: 'DOING', label: '进行中', empty: '当前没有进行中的训练。' },
  { status: 'DONE', label: '已完成', empty: '完成的任务会沉淀在这里。' }
]

const tasksByStatus = computed(() => {
  return tasks.value.reduce((grouped, task) => {
    const status = task.status || 'TODO'
    if (!grouped[status]) {
      grouped[status] = []
    }
    grouped[status].push(task)
    return grouped
  }, { TODO: [], DOING: [], DONE: [] })
})

onMounted(() => {
  loadTraining()
})

async function loadTraining() {
  loading.value = true
  try {
    const [overviewResponse, tasksResponse] = await Promise.all([
      fetchTrainingOverview(),
      fetchTrainingTasks()
    ])
    overview.value = overviewResponse.data
    tasks.value = tasksResponse.data || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '读取训练计划失败')
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    const response = await generateTrainingPlan()
    tasks.value = response.data?.tasks || []
    ElMessage.success(response.data?.message || '训练计划已生成')
    await refreshOverview()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '生成训练计划失败')
  } finally {
    generating.value = false
  }
}

async function handleStatus(task, status) {
  try {
    const response = await updateTrainingTaskStatus(task.id, status)
    replaceTask(response.data)
    await refreshOverview()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '更新任务失败')
  }
}

async function handleDelete(task) {
  try {
    await ElMessageBox.confirm(`确定删除“${task.title}”吗？`, '删除训练任务', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTrainingTask(task.id)
    tasks.value = tasks.value.filter((item) => item.id !== task.id)
    await refreshOverview()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error.message || '删除任务失败')
    }
  }
}

async function refreshOverview() {
  const response = await fetchTrainingOverview()
  overview.value = response.data
}

function replaceTask(task) {
  tasks.value = tasks.value.map((item) => (item.id === task.id ? task : item))
}

function progressWidth(task) {
  const target = Math.max(task.targetCount || 1, 1)
  const finished = Math.min(task.finishedCount || 0, target)
  return `${Math.round((finished / target) * 100)}%`
}
</script>
