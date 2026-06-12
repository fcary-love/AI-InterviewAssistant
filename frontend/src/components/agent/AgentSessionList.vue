<template>
  <aside class="session-list">
    <div class="session-list-header">
      <strong>对话记录</strong>
      <button type="button" class="btn-new" @click="$emit('new-session')">+ 新对话</button>
    </div>
    <div class="session-items">
      <div
        v-for="session in sessions"
        :key="session.sessionId"
        class="session-item"
        :class="{ active: session.sessionId === activeSessionId }"
        @click="$emit('switch-session', session.sessionId)"
      >
        <span class="session-title">{{ session.title || '新对话' }}</span>
        <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
        <button
          type="button"
          class="btn-delete"
          @click.stop="$emit('delete-session', session.sessionId)"
          title="删除会话"
        >×</button>
      </div>
      <div v-if="sessions.length === 0" class="session-empty">
        暂无对话记录
      </div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  activeSessionId: {
    type: String,
    default: null
  }
})

defineEmits(['new-session', 'switch-session', 'delete-session'])

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr.replace(' ', 'T'))
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return timeStr.substring(5, 16)
}
</script>

<style scoped>
.session-list {
  width: 240px;
  min-width: 200px;
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  background: rgba(30, 29, 36, 0.5);
}

.session-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.session-list-header strong {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
}

.btn-new {
  background: none;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  padding: 4px 10px;
  font-size: 12px;
  color: var(--gold);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.btn-new:hover {
  background: rgba(212, 163, 106, 0.1);
  border-color: var(--gold-border);
}

.session-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-bottom: 4px;
  position: relative;
  transition: background var(--duration-fast);
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.session-item.active {
  background: rgba(212, 163, 106, 0.1);
  border: 1px solid var(--gold-border);
}

.session-title {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}

.btn-delete {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 2px 4px;
  border-radius: var(--radius-sm);
  opacity: 0;
  transition: opacity var(--duration-fast), color var(--duration-fast);
}

.session-item:hover .btn-delete {
  opacity: 1;
}

.btn-delete:hover {
  color: var(--danger);
  background: var(--danger-surface);
}

.session-empty {
  padding: 24px 12px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
