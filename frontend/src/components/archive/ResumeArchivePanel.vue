<template>
  <section class="focus-panel">
    <div class="panel-topline">
      <span>Archive</span>
      <strong>{{ archiveLoading ? '读取中' : `${resumeVersions.length} 个版本` }}</strong>
    </div>

    <section v-if="!archiveLoading && !resumeVersions.length" class="archive-empty">
      <strong>还没有简历版本。</strong>
      <p>上传 PDF 或 DOCX 简历后，系统会自动把它保存为一个版本。后续你可以查看每一版内容，也可以比较两版差异。</p>
    </section>

    <template v-else>
      <section class="archive-compare">
        <div>
          <span>Version Compare</span>
          <strong>简历版本对比</strong>
          <p>选择旧版和新版，系统会比较文本长度、技术关键词变化和整体改动方向。</p>
        </div>
        <div class="archive-compare-controls">
          <el-select
            v-model="leftVersionId"
            placeholder="旧版本"
            :disabled="!hasEnoughVersions"
          >
            <el-option
              v-for="version in resumeVersions"
              :key="version.id"
              :label="`V${version.versionNo} · ${version.fileName}`"
              :value="version.id"
            />
          </el-select>
          <el-select
            v-model="rightVersionId"
            placeholder="新版本"
            :disabled="!hasEnoughVersions"
          >
            <el-option
              v-for="version in resumeVersions"
              :key="version.id"
              :label="`V${version.versionNo} · ${version.fileName}`"
              :value="version.id"
            />
          </el-select>
          <button
            type="button"
            :disabled="!hasEnoughVersions || compareLoading"
            @click="handleCompareResumeVersions"
          >
            {{ compareLoading ? '对比中' : '开始对比' }}
          </button>
        </div>
      </section>

      <section v-if="compareResult" class="archive-compare-result">
        <article>
          <span>文本变化</span>
          <strong>{{ compareResult.textLengthDelta > 0 ? '+' : '' }}{{ compareResult.textLengthDelta }}</strong>
        </article>
        <article>
          <span>新增关键词</span>
          <strong>{{ compareResult.addedKeywords?.length || 0 }}</strong>
          <p>{{ formatKeywordList(compareResult.addedKeywords) }}</p>
        </article>
        <article>
          <span>移除关键词</span>
          <strong>{{ compareResult.removedKeywords?.length || 0 }}</strong>
          <p>{{ formatKeywordList(compareResult.removedKeywords) }}</p>
        </article>
        <p class="archive-compare-summary">{{ compareResult.summary }}</p>
      </section>

      <section class="archive-version-list">
        <article
          v-for="version in resumeVersions"
          :key="version.id"
          class="archive-version-row"
          :class="{ active: selectedResume?.id === version.id }"
        >
          <div class="archive-version-badge">
            <span>V</span>
            <strong>{{ version.versionNo }}</strong>
          </div>
          <div class="archive-version-main">
            <strong>{{ version.fileName }}</strong>
            <span>{{ version.fileType }} · {{ version.textLength }} 字 · {{ version.createdAt }}</span>
            <p>{{ version.contentPreview }}</p>
            <small>{{ version.skillKeywords || '暂未识别到技术关键词' }}</small>
          </div>
          <button type="button" @click="viewResumeVersion(version.id)">查看</button>
        </article>
      </section>

      <section v-if="selectedResume" class="archive-detail">
        <div class="archive-detail-head">
          <div>
            <span>Version Detail</span>
            <strong>V{{ selectedResume.versionNo }} · {{ selectedResume.fileName }}</strong>
          </div>
          <small>{{ detailLoading ? '读取中' : `${selectedResume.textLength} 字` }}</small>
        </div>
        <div class="archive-detail-meta">
          <span>类型：{{ selectedResume.fileType }}</span>
          <span>创建时间：{{ selectedResume.createdAt }}</span>
          <span>关键词：{{ selectedResume.skillKeywords || '暂无' }}</span>
        </div>
        <pre>{{ selectedResume.fullText || selectedResume.contentPreview }}</pre>
      </section>
    </template>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useResumeArchive } from '../../composables/useResumeArchive'

const {
  archiveLoading,
  compareLoading,
  compareResult,
  detailLoading,
  handleCompareResumeVersions,
  hasEnoughVersions,
  leftVersionId,
  loadResumeVersions,
  resumeVersions,
  rightVersionId,
  selectedResume,
  viewResumeVersion
} = useResumeArchive()

onMounted(() => {
  loadResumeVersions()
})

function formatKeywordList(keywords) {
  if (!keywords?.length) {
    return '暂无变化'
  }
  return keywords.join('、')
}
</script>
