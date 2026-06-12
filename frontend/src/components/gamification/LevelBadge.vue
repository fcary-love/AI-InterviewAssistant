<template>
  <div class="level-badge" :class="{ compact }" @click="$emit('click')">
    <div class="level-circle">
      <span class="level-number">{{ level }}</span>
    </div>
    <div v-if="!compact" class="level-info">
      <span class="level-title">{{ title }}</span>
      <div class="exp-bar">
        <div class="exp-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: { type: Number, default: 1 },
  title: { type: String, default: '面试新手' },
  progress: { type: Number, default: 0 },
  needed: { type: Number, default: 100 },
  compact: { type: Boolean, default: false }
})

defineEmits(['click'])

const progressPercent = computed(() => {
  if (props.needed <= 0) return 100
  return Math.min(100, Math.round((props.progress / props.needed) * 100))
})
</script>

<style scoped>
.level-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  background: rgba(39, 37, 47, 0.6);
  border: 1px solid var(--border-subtle);
  transition: all var(--duration-fast);
}

.level-badge:hover {
  border-color: var(--gold-border);
  background: rgba(39, 37, 47, 0.8);
}

.level-badge.compact {
  padding: 4px 8px;
  gap: 6px;
}

.level-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--gold-deep), var(--gold));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(212, 163, 106, 0.3);
}

.compact .level-circle {
  width: 24px;
  height: 24px;
}

.level-number {
  color: var(--text-inverse);
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
}

.compact .level-number {
  font-size: 11px;
}

.level-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.level-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1;
}

.exp-bar {
  width: 60px;
  height: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
}

.exp-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--gold-deep), var(--gold-bright));
  border-radius: 2px;
  transition: width 0.3s ease;
}
</style>
