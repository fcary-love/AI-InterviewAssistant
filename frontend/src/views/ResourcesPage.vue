<template>
  <section class="page-container">
    <div class="page-topline">
      <strong>资料管理</strong>
      <span>题库 · 训练计划 · 简历档案</span>
    </div>

    <el-tabs v-model="activeTab" class="coach-tabs">
      <el-tab-pane label="题库" name="questions">
        <QuestionBankPanel
          @ask-question="handleQuestionBankAsk"
        />
      </el-tab-pane>
      <el-tab-pane label="训练计划" name="training">
        <TrainingPlanPanel
          @open-coach="handleOpenCoach"
          @open-interview="handleOpenInterview"
          @open-question-bank="handleOpenQuestionBank"
        />
      </el-tab-pane>
      <el-tab-pane label="简历档案" name="archive">
        <ResumeArchivePanel />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import QuestionBankPanel from '../components/questions/QuestionBankPanel.vue'
import TrainingPlanPanel from '../components/training/TrainingPlanPanel.vue'
import ResumeArchivePanel from '../components/archive/ResumeArchivePanel.vue'
import { explainQuestion } from '../api/questions'
import { useInterviewDesk } from '../composables/useInterviewDesk'

const router = useRouter()
const desk = useInterviewDesk()
const activeTab = ref('questions')

function handleOpenCoach() {
  router.push('/coach')
}

function handleOpenInterview() {
  router.push('/interview')
}

async function handleQuestionBankAsk(prompt) {
  router.push('/coach')
  desk.addConversationMessage({
    role: 'user',
    label: '我',
    content: prompt,
    typing: false,
    kind: 'question-bank'
  })
  const pendingMessage = desk.addConversationMessage({
    role: 'assistant',
    label: '题库讲解',
    content: '正在拆这道题的考点和回答思路。',
    typing: false,
    pending: true,
    kind: 'question-bank'
  })
  try {
    const response = await explainQuestion(prompt)
    desk.updateConversationMessage(pendingMessage.id, {
      content: response.data?.answer || '这道题暂时没有生成讲解。',
      typing: true,
      pending: false
    })
  } catch (error) {
    desk.updateConversationMessage(pendingMessage.id, {
      content: error?.response?.data?.message || error.message || '题目讲解失败了，可以稍后再试一次。',
      typing: false,
      pending: false,
      kind: 'error'
    })
  }
}

function handleOpenQuestionBank(category) {
  activeTab.value = 'questions'
  if (category) {
    desk.addConversationMessage({
      role: 'assistant',
      label: '训练建议',
      content: `可以优先练"${category}"这一类题。建议先自己口述，再看参考思路，最后让助手帮你把答案改成面试表达。`,
      typing: true,
      kind: 'guide'
    })
  }
}
</script>
