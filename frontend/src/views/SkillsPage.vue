<template>
  <section class="page-container">
    <div class="page-topline">
      <strong>技能图谱</strong>
      <span>知识点 · 掌握度 · 学习路径</span>
    </div>

    <SkillTreePanel direction="后端开发" />
  </section>
</template>

<script setup>
import SkillTreePanel from '../components/skill-tree/SkillTreePanel.vue'
</script>
