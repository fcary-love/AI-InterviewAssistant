<template>
  <div class="difficulty-indicator" :class="levelClass">
    <span class="difficulty-icon">{{ icon }}</span>
    <span class="difficulty-label">{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  difficulty: { type: String, default: '标准' }
})

const levelClass = computed(() => {
  switch (props.difficulty) {
    case '简单': return 'easy'
    case '标准': return 'standard'
    case '困难': return 'hard'
    case '专家': return 'expert'
    default: return 'standard'
  }
})

const icon = computed(() => {
  switch (props.difficulty) {
    case '简单': return '🌱'
    case '标准': return '⚡'
    case '困难': return '🔥'
    case '专家': return '💎'
    default: return '⚡'
  }
})

const label = computed(() => props.difficulty || '标准')
</script>

<style scoped>
.difficulty-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  transition: all var(--duration-fast);
}

.difficulty-indicator.easy {
  color: var(--success);
  background: var(--success-surface);
  border: 1px solid rgba(136, 180, 136, 0.25);
}

.difficulty-indicator.standard {
  color: var(--gold);
  background: var(--gold-surface);
  border: 1px solid var(--gold-border);
}

.difficulty-indicator.hard {
  color: var(--warning);
  background: rgba(201, 162, 85, 0.1);
  border: 1px solid rgba(201, 162, 85, 0.25);
}

.difficulty-indicator.expert {
  color: var(--danger);
  background: var(--danger-surface);
  border: 1px solid rgba(200, 112, 96, 0.25);
}

.difficulty-icon {
  font-size: 14px;
}

.difficulty-label {
  letter-spacing: 0.02em;
}
</style>
