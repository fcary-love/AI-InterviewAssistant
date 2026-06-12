<template>
  <div class="tool-call-card">
    <div class="tool-call-header" @click="expanded = !expanded">
      <span class="tool-icon">⚙</span>
      <span class="tool-name">{{ toolDisplayName }}</span>
      <span class="tool-toggle">{{ expanded ? '收起' : '详情' }}</span>
    </div>
    <div v-if="expanded" class="tool-call-body">
      <div v-if="toolCall.arguments" class="tool-section">
        <strong>参数：</strong>
        <pre>{{ formatJson(toolCall.arguments) }}</pre>
      </div>
      <div v-if="toolCall.result" class="tool-section">
        <strong>结果：</strong>
        <pre>{{ toolCall.result }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  toolCall: {
    type: Object,
    required: true
  }
})

const expanded = ref(false)

const toolNameMap = {
  query_interview_history: '查询面试历史',
  query_weak_points: '分析薄弱点',
  query_question_bank: '搜索题库',
  search_knowledge_base: '检索知识库',
  create_training_task: '创建训练任务',
  query_training_tasks: '查看训练任务',
  update_training_task: '更新任务状态'
}

const toolDisplayName = computed(() => {
  return toolNameMap[props.toolCall.toolName] || props.toolCall.toolName
})

function formatJson(str) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}
</script>

<style scoped>
.tool-call-card {
  background: rgba(212, 163, 106, 0.06);
  border: 1px solid rgba(212, 163, 106, 0.2);
  border-radius: var(--radius-md);
  margin: 8px 0;
  overflow: hidden;
}

.tool-call-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--gold);
}

.tool-call-header:hover {
  background: rgba(212, 163, 106, 0.08);
}

.tool-icon {
  font-size: 14px;
}

.tool-name {
  flex: 1;
  font-weight: 600;
}

.tool-toggle {
  font-size: 12px;
  color: var(--text-muted);
}

.tool-call-body {
  padding: 0 12px 10px;
  font-size: 12px;
  line-height: 1.6;
}

.tool-section {
  margin-top: 6px;
}

.tool-section strong {
  display: block;
  margin-bottom: 4px;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
}

.tool-section pre {
  background: rgba(35, 34, 41, 0.6);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
  font-size: 11px;
  color: var(--text-primary);
  font-family: var(--font-mono);
}
</style>
