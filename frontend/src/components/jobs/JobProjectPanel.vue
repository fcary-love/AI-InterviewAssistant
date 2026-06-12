<template>
  <section class="focus-panel job-project-panel">
    <div class="panel-topline">
      <span>Job Projects</span>
      <strong>{{ selectedProject ? '岗位项目详情' : '岗位项目中心' }}</strong>
    </div>

    <article class="project-create-card">
      <div>
        <span>New Project</span>
        <strong>把一个 JD 建成一个求职项目</strong>
        <p>绑定一版简历后，系统会记录匹配分、改写建议、投递版简历和后续面试状态。</p>
      </div>
      <form @submit.prevent="handleCreateProject">
        <input v-model.trim="form.companyName" placeholder="公司名称，例如：阿里云">
        <input v-model.trim="form.jobTitle" placeholder="岗位名称，例如：Java 后端开发工程师">
        <el-select v-model="form.resumeVersionId" placeholder="绑定简历版本">
          <el-option
            v-for="version in resumeVersions"
            :key="version.id"
            :label="`V${version.versionNo} · ${version.fileName}`"
            :value="version.id"
          />
        </el-select>
        <textarea v-model.trim="form.jdText" placeholder="粘贴岗位 JD：岗位职责、任职要求、技术栈、加分项..." />
        <button type="submit" :disabled="creating || !canCreate">
          {{ creating ? '创建中' : '创建岗位项目' }}
        </button>
      </form>
    </article>

    <p v-if="error" class="job-history-notice">{{ error }}</p>

    <section class="project-workspace">
      <aside class="project-list">
        <button
          v-for="project in projects"
          :key="project.id"
          type="button"
          class="project-row"
          :class="{ active: selectedProject?.id === project.id }"
          @click="loadProjectDetail(project.id)"
        >
          <span>{{ project.status }}</span>
          <strong>{{ project.companyName }} · {{ project.jobTitle }}</strong>
          <small>{{ project.matchScore == null ? '待匹配' : `${project.matchScore} 分` }} · {{ project.updatedAt }}</small>
          <p>{{ project.jdExcerpt }}</p>
        </button>
        <article v-if="!loading && !projects.length" class="project-empty">
          还没有岗位项目。先在上方创建一个，把 JD 和简历版本绑定起来。
        </article>
      </aside>

      <article class="project-detail">
        <div v-if="!selectedProject" class="job-history-placeholder">
          <strong>选择一个岗位项目。</strong>
          <p>这里会展示岗位状态、绑定简历、匹配分、缺口关键词、投递版简历和后续动作。</p>
        </div>

        <template v-else>
          <div class="project-detail-head">
            <div class="job-history-score">
              <span>匹配分</span>
              <strong>{{ selectedProject.matchScore ?? '-' }}</strong>
            </div>
            <div>
              <span>{{ selectedProject.status }}</span>
              <strong>{{ selectedProject.companyName }} · {{ selectedProject.jobTitle }}</strong>
              <small>绑定简历：V{{ selectedProject.resumeVersionNo || '-' }} · {{ selectedProject.resumeFileName || '未知简历' }}</small>
            </div>
            <el-select
              :model-value="selectedProject.status"
              size="large"
              @change="handleStatusChange"
            >
              <el-option v-for="status in statuses" :key="status" :label="status" :value="status" />
            </el-select>
          </div>

          <section class="project-actions">
            <button type="button" :disabled="matching" @click="handleMatchProject">
              {{ matching ? '匹配中' : '执行岗位匹配' }}
            </button>
            <button type="button" :disabled="tailoring" @click="handleGenerateTailoredResume">
              {{ tailoring ? '生成中' : '生成投递版简历' }}
            </button>
            <button type="button" @click="$emit('use-project', selectedProject)">带回咨询区</button>
          </section>

          <section class="job-history-sections">
            <article>
              <span>JD 关键词</span>
              <div class="job-history-tags">
                <small v-for="item in selectedProject.jdKeywords" :key="item">{{ item }}</small>
                <small v-if="!selectedProject.jdKeywords?.length">暂无关键词</small>
              </div>
            </article>
            <article>
              <span>简历已覆盖</span>
              <div class="job-history-tags">
                <small v-for="item in selectedProject.resumeKeywords" :key="item">{{ item }}</small>
                <small v-if="!selectedProject.resumeKeywords?.length">暂无覆盖关键词</small>
              </div>
            </article>
            <article>
              <span>需要补强</span>
              <div class="job-history-tags warn">
                <small v-for="item in selectedProject.missingKeywords" :key="item">{{ item }}</small>
                <small v-if="!selectedProject.missingKeywords?.length">暂无明显缺口</small>
              </div>
            </article>
            <article>
              <span>项目状态</span>
              <ol>
                <li>待分析：刚创建，还没有执行匹配</li>
                <li>已匹配：已经获得 JD 匹配分析</li>
                <li>已优化：已经生成投递版简历</li>
                <li>已面试 / 已复盘：后续可绑定面试记录</li>
              </ol>
            </article>
          </section>

          <section class="project-text-grid">
            <article>
              <span>简历修改建议</span>
              <pre>{{ selectedProject.resumeSuggestions || '执行岗位匹配后，这里会保存针对该岗位的简历修改建议。' }}</pre>
            </article>
            <article>
              <span>投递版简历文本</span>
              <pre>{{ selectedProject.tailoredResumeText || '点击“生成投递版简历”后，这里会保存一份面向该岗位的简历文本。' }}</pre>
            </article>
          </section>
        </template>
      </article>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createJobProject,
  fetchJobProjectDetail,
  fetchJobProjects,
  generateTailoredResume,
  matchJobProject,
  updateJobProjectStatus
} from '../../api/jobProjects'
import { fetchResumeVersions } from '../../api/profile'

defineEmits(['use-project'])

const statuses = ['待分析', '已匹配', '已优化', '已面试', '已复盘']
const projects = ref([])
const resumeVersions = ref([])
const selectedProject = ref(null)
const loading = ref(false)
const creating = ref(false)
const matching = ref(false)
const tailoring = ref(false)
const error = ref('')
const form = reactive({
  companyName: '',
  jobTitle: '',
  jdText: '',
  resumeVersionId: ''
})

const canCreate = computed(() => {
  return form.companyName && form.jobTitle && form.jdText && form.resumeVersionId
})

onMounted(async () => {
  await Promise.all([loadProjects(), loadResumeVersions()])
})

async function loadResumeVersions() {
  const response = await fetchResumeVersions()
  resumeVersions.value = response.data || []
  if (!form.resumeVersionId && resumeVersions.value.length) {
    form.resumeVersionId = resumeVersions.value[0].id
  }
}

async function loadProjects() {
  loading.value = true
  error.value = ''
  try {
    const response = await fetchJobProjects(50)
    projects.value = response.data || []
    if (!selectedProject.value && projects.value.length) {
      await loadProjectDetail(projects.value[0].id)
    }
  } catch (err) {
    error.value = err?.response?.data?.message || err.message || '岗位项目加载失败'
  } finally {
    loading.value = false
  }
}

async function loadProjectDetail(id) {
  error.value = ''
  try {
    const response = await fetchJobProjectDetail(id)
    selectedProject.value = response.data || null
  } catch (err) {
    error.value = err?.response?.data?.message || err.message || '岗位项目详情加载失败'
  }
}

async function handleCreateProject() {
  if (!canCreate.value) {
    ElMessage.warning('请补全公司、岗位、JD 和简历版本')
    return
  }
  creating.value = true
  try {
    const response = await createJobProject({ ...form })
    selectedProject.value = response.data
    form.companyName = ''
    form.jobTitle = ''
    form.jdText = ''
    await loadProjects()
    ElMessage.success('岗位项目已创建')
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err.message || '岗位项目创建失败')
  } finally {
    creating.value = false
  }
}

async function handleMatchProject() {
  if (!selectedProject.value?.id) {
    return
  }
  matching.value = true
  try {
    const response = await matchJobProject(selectedProject.value.id)
    selectedProject.value = response.data
    await loadProjects()
    ElMessage.success('岗位匹配完成')
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err.message || '岗位匹配失败')
  } finally {
    matching.value = false
  }
}

async function handleGenerateTailoredResume() {
  if (!selectedProject.value?.id) {
    return
  }
  tailoring.value = true
  try {
    const response = await generateTailoredResume(selectedProject.value.id)
    selectedProject.value = response.data
    await loadProjects()
    ElMessage.success('投递版简历已生成')
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err.message || '投递版简历生成失败')
  } finally {
    tailoring.value = false
  }
}

async function handleStatusChange(status) {
  if (!selectedProject.value?.id) {
    return
  }
  try {
    const response = await updateJobProjectStatus(selectedProject.value.id, status)
    selectedProject.value = response.data
    await loadProjects()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err.message || '状态更新失败')
  }
}
</script>
