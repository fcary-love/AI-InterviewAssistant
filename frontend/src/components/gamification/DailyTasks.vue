<template>
  <div class="daily-tasks">
    <div class="tasks-header">
      <h4>今日任务</h4>
      <StreakCounter :days="streakDays" />
    </div>
    <div v-if="!tasks.length" class="tasks-empty">
      <p>暂无任务</p>
    </div>
    <div v-else class="task-list">
      <div
        v-for="task in tasks"
        :key="task.taskType"
        class="task-item"
        :class="{ completed: task.completed, claimed: task.claimed }"
      >
        <div class="task-status">
          <span v-if="task.claimed" class="status-icon claimed">✅</span>
          <span v-else-if="task.completed" class="status-icon completed">⬜</span>
          <span v-else class="status-icon pending">⬜</span>
        </div>
        <div class="task-content">
          <span class="task-desc">{{ task.description }}</span>
          <span class="task-progress">{{ task.currentCount }}/{{ task.targetCount }}</span>
        </div>
        <button
          v-if="task.completed && !task.claimed"
          class="claim-btn"
          @click="$emit('claim', task.taskType)"
        >
          领取 +{{ task.expReward }} EXP
        </button>
        <span v-else-if="task.claimed" class="claimed-label">已领取</span>
        <span v-else class="reward-label">+{{ task.expReward }} EXP</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import StreakCounter from './StreakCounter.vue'

defineProps({
  tasks: { type: Array, default: () => [] },
  streakDays: { type: Number, default: 0 }
})

defineEmits(['claim'])
</script>

<style scoped>
.daily-tasks {
  padding: 16px 20px;
  background: rgba(39, 37, 47, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
}

.tasks-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.tasks-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--gold);
}

.tasks-empty {
  text-align: center;
  padding: 20px 0;
}

.tasks-empty p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.task-list {
  display: grid;
  gap: 10px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(35, 34, 41, 0.5);
  border: 1px solid var(--border-subtle);
  transition: all var(--duration-fast);
}

.task-item.completed {
  border-color: rgba(136, 180, 152, 0.3);
}

.task-item.claimed {
  opacity: 0.6;
}

.task-status {
  flex-shrink: 0;
}

.status-icon {
  font-size: 16px;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.task-desc {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.task-progress {
  font-size: 11px;
  color: var(--text-muted);
}

.claim-btn {
  flex-shrink: 0;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--gold-deep), var(--gold));
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.claim-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.claimed-label {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--success);
  font-weight: 600;
}

.reward-label {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}
</style>
