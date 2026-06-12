<template>
  <section class="focus-panel home-panel">
    <div class="panel-topline">
      <span>Workspace</span>
      <strong>系统首页</strong>
    </div>

    <section class="home-hero">
      <article class="home-hero-main">
        <span>Career Operating System</span>
        <strong>把简历、岗位、训练和报告放进同一条求职流程。</strong>
        <p>先沉淀材料，再做岗位匹配，然后进入针对性训练。每一次咨询、JD 分析和模拟面试都会变成后续可复用的数据。</p>
      </article>
      <article class="home-hero-status">
        <span>当前状态</span>
        <strong>{{ resumeReady ? '材料已就绪' : '等待简历' }}</strong>
        <p>{{ resumeReady ? '可以继续做岗位匹配、简历诊断或模拟面试。' : '先上传一份 PDF、DOCX 或图片简历，系统会进入完整分析状态。' }}</p>
        <button type="button" @click="$emit(resumeReady ? 'open-coach' : 'upload-resume')">
          {{ resumeReady ? '继续咨询' : '上传简历' }}
        </button>
      </article>
    </section>

    <section class="home-metrics" aria-label="系统数据概览">
      <article>
        <span>简历版本</span>
        <strong>{{ resumeVersionCount }}</strong>
        <p>沉淀过的简历材料</p>
      </article>
      <article>
        <span>岗位匹配</span>
        <strong>{{ jobMatchCount }}</strong>
        <p>已分析过的 JD</p>
      </article>
      <article>
        <span>会话记录</span>
        <strong>{{ conversationCount }}</strong>
        <p>可连续追问</p>
      </article>
      <article>
        <span>最近匹配分</span>
        <strong>{{ latestMatchScore }}</strong>
        <p>来自最新岗位记录</p>
      </article>
    </section>

    <section class="home-module-grid" aria-label="系统模块">
      <button
        v-for="module in modules"
        :key="module.key"
        type="button"
        class="home-module-card"
        @click="$emit('open-mode', module.key)"
      >
        <span>{{ module.eyebrow }}</span>
        <strong>{{ module.title }}</strong>
        <p>{{ module.description }}</p>
        <small>{{ module.action }}</small>
      </button>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  conversationCount: {
    type: Number,
    default: 0
  },
  jdReady: {
    type: Boolean,
    default: false
  },
  profileOverview: {
    type: Object,
    default: null
  },
  resumeReady: {
    type: Boolean,
    default: false
  }
})

defineEmits(['open-coach', 'open-mode', 'upload-resume'])

const resumeVersionCount = computed(() => props.profileOverview?.resumeVersions?.length || 0)
const jobMatchCount = computed(() => props.profileOverview?.jobMatches?.length || 0)
const latestMatchScore = computed(() => {
  const score = props.profileOverview?.jobMatches?.[0]?.matchScore
  return score === null || score === undefined ? '-' : `${score} 分`
})

const modules = computed(() => [
  {
    key: 'coach',
    eyebrow: 'Consult',
    title: '简历咨询',
    description: props.resumeReady ? '继续围绕当前简历做诊断、改写和项目表达优化。' : '上传简历后，可以开始做整体诊断和连续追问。',
    action: props.resumeReady ? '进入咨询' : '先准备材料'
  },
  {
    key: 'matches',
    eyebrow: 'Job Match',
    title: '岗位项目',
    description: props.jdReady ? '当前已有 JD，可以创建项目并绑定简历版本。' : '把每一个 JD 管成一个求职项目，沉淀匹配、优化和复盘。',
    action: '管理投递项目'
  },
  {
    key: 'archive',
    eyebrow: 'Resume',
    title: '简历档案',
    description: '查看简历版本、版本详情和历史变更，让材料迭代有记录。',
    action: '管理版本'
  },
  {
    key: 'training',
    eyebrow: 'Training',
    title: '训练计划',
    description: '根据简历、错题和历史报告，把薄弱项拆成可执行练习。',
    action: '安排训练'
  },
  {
    key: 'interview',
    eyebrow: 'Mock',
    title: '模拟面试',
    description: '选择方向、难度和题库，记录问题、答案、用时和评分。',
    action: '开始面试'
  },
  {
    key: 'dashboard',
    eyebrow: 'Growth',
    title: '成长看板',
    description: '查看匹配次数、报告分数趋势、高频薄弱点和下一步建议。',
    action: '查看成长线'
  }
])
</script>
