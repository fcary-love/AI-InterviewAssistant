<template>
  <div class="streak-counter" :class="{ active: days > 0 }">
    <span class="streak-icon">🔥</span>
    <span class="streak-number">{{ days }}</span>
    <span class="streak-label">天</span>
  </div>
</template>

<script setup>
defineProps({
  days: { type: Number, default: 0 }
})
</script>

<style scoped>
.streak-counter {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  background: rgba(39, 37, 47, 0.6);
  border: 1px solid var(--border-subtle);
  font-size: 13px;
}

.streak-counter.active {
  border-color: rgba(212, 163, 106, 0.3);
  background: rgba(212, 163, 106, 0.08);
}

.streak-icon {
  font-size: 14px;
}

.streak-number {
  font-weight: 700;
  color: var(--text-primary);
}

.streak-label {
  color: var(--text-muted);
  font-size: 11px;
}

@media (max-width: 640px) {
  .streak-label {
    display: none;
  }

  .streak-counter {
    padding: 3px 8px;
  }
}
</style>
