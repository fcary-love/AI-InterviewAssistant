<template>
  <div class="skill-tree-panel">
    <div class="skill-tree-header">
      <div class="header-info">
        <h3>技能知识图谱</h3>
        <p>掌握度越高，节点越绿。箭头表示学习依赖关系。</p>
      </div>
      <div class="header-stats" v-if="treeData.length">
        <div class="stat">
          <strong>{{ treeData.length }}</strong>
          <span>知识点</span>
        </div>
        <div class="stat">
          <strong>{{ masteredCount }}</strong>
          <span>已掌握</span>
        </div>
        <div class="stat">
          <strong>{{ weakCount }}</strong>
          <span>待加强</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="skill-tree-loading">
      <p>加载中...</p>
    </div>

    <div v-else-if="!treeData.length" class="skill-tree-empty">
      <p>暂无知识点数据</p>
    </div>

    <div v-else ref="chartRef" class="skill-tree-chart"></div>

    <!-- 节点详情弹窗 -->
    <el-dialog
      v-model="showDetail"
      :title="selectedNode?.name || '知识点详情'"
      width="400px"
      class="skill-detail-dialog"
    >
      <div v-if="selectedNode" class="skill-detail">
        <div class="detail-row">
          <span class="detail-label">分类</span>
          <span class="detail-value">{{ selectedNode.category }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">难度</span>
          <span class="detail-value" :class="difficultyClass(selectedNode.difficulty)">
            {{ selectedNode.difficulty }}
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">掌握度</span>
          <div class="mastery-bar">
            <div class="mastery-fill" :style="{ width: selectedNode.mastery + '%' }"></div>
            <span class="mastery-text">{{ selectedNode.mastery.toFixed(0) }}%</span>
          </div>
        </div>
        <div class="detail-row">
          <span class="detail-label">练习次数</span>
          <span class="detail-value">{{ selectedNode.attemptCount }} 次</span>
        </div>
        <p v-if="selectedNode.description" class="detail-desc">{{ selectedNode.description }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { fetchSkillTree } from '../../api/knowledge'

const props = defineProps({
  direction: { type: String, default: '后端开发' }
})

const chartRef = ref(null)
const treeData = ref([])
const loading = ref(true)
const showDetail = ref(false)
const selectedNode = ref(null)
let chart = null

const masteredCount = computed(() =>
  treeData.value.filter(n => n.mastery >= 70).length
)

const weakCount = computed(() =>
  treeData.value.filter(n => n.mastery < 40 && n.attemptCount > 0).length
)

function getMasteryColor(mastery) {
  if (mastery >= 70) return '#88b498' // 绿色
  if (mastery >= 40) return '#d4a36a' // 金色
  return '#d07868' // 红色
}

function getNodeSize(dependencyCount) {
  return Math.max(20, 20 + dependencyCount * 5)
}

function difficultyClass(difficulty) {
  switch (difficulty) {
    case '简单': return 'diff-easy'
    case '标准': return 'diff-standard'
    case '困难': return 'diff-hard'
    case '专家': return 'diff-expert'
    default: return 'diff-standard'
  }
}

async function loadTree() {
  loading.value = true
  try {
    const data = await fetchSkillTree(props.direction)
    treeData.value = data.data || []
    nextTick(renderChart)
  } catch (e) {
    console.error('加载技能树失败', e)
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || !treeData.value.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const nodes = treeData.value.map(node => ({
    id: String(node.id),
    name: node.name,
    symbolSize: getNodeSize(node.dependencyCount),
    itemStyle: {
      color: getMasteryColor(node.mastery),
      borderColor: 'rgba(255,255,255,0.2)',
      borderWidth: 1
    },
    label: {
      show: true,
      position: 'bottom',
      fontSize: 10,
      color: '#aba79e'
    },
    category: node.category,
    value: node.mastery,
    _raw: node
  }))

  // 构建连线
  const links = []
  for (const node of treeData.value) {
    if (node.dependents) {
      for (const depId of node.dependents) {
        links.push({
          source: String(node.id),
          target: String(depId),
          lineStyle: {
            color: 'rgba(212, 163, 106, 0.3)',
            curveness: 0.2
          }
        })
      }
    }
  }

  // 提取分类
  const categories = [...new Set(treeData.value.map(n => n.category))].map(name => ({ name }))

  const option = {
    tooltip: {
      backgroundColor: 'rgba(39, 37, 47, 0.95)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: { color: '#f0ece3' },
      formatter(params) {
        if (params.dataType === 'node') {
          const raw = params.data._raw
          return `<strong>${raw.name}</strong><br/>` +
            `分类：${raw.category}<br/>` +
            `难度：${raw.difficulty}<br/>` +
            `掌握度：<strong>${raw.mastery.toFixed(0)}%</strong><br/>` +
            `练习：${raw.attemptCount} 次`
        }
        return ''
      }
    },
    legend: {
      data: categories.map(c => c.name),
      top: 0,
      textStyle: { color: '#aba79e', fontSize: 11 }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: categories,
      roam: true,
      draggable: true,
      force: {
        repulsion: 300,
        edgeLength: [80, 160],
        gravity: 0.1
      },
      lineStyle: {
        opacity: 0.6
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3 }
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8]
    }]
  }

  chart.setOption(option)

  // 点击节点显示详情
  chart.on('click', (params) => {
    if (params.dataType === 'node' && params.data._raw) {
      selectedNode.value = params.data._raw
      showDetail.value = true
    }
  })
}

watch(() => props.direction, loadTree)

onMounted(() => {
  loadTree()
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.skill-tree-panel {
  background: rgba(39, 37, 47, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-top: 20px;
}

.skill-tree-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 16px;
}

.header-info h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--gold);
}

.header-info p {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.header-stats {
  display: flex;
  gap: 16px;
}

.stat {
  text-align: center;
}

.stat strong {
  display: block;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 700;
}

.stat span {
  font-size: 11px;
  color: var(--text-muted);
}

.skill-tree-chart {
  width: 100%;
  height: 500px;
}

.skill-tree-loading,
.skill-tree-empty {
  display: grid;
  place-items: center;
  min-height: 300px;
}

.skill-tree-loading p,
.skill-tree-empty p {
  color: var(--text-muted);
  font-size: 14px;
}

/* 详情弹窗 */
.skill-detail {
  display: grid;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.detail-label {
  font-size: 13px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.detail-value {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.diff-easy { color: var(--success); }
.diff-standard { color: var(--gold); }
.diff-hard { color: var(--warning); }
.diff-expert { color: var(--danger); }

.mastery-bar {
  flex: 1;
  max-width: 200px;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.mastery-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--gold-deep), var(--gold));
  border-radius: 4px;
  transition: width 0.3s ease;
}

.mastery-text {
  position: absolute;
  right: -40px;
  top: -4px;
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 600;
}

.detail-desc {
  margin: 8px 0 0;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  background: rgba(35, 34, 41, 0.5);
  border-radius: var(--radius-md);
}

@media (max-width: 640px) {
  .skill-tree-panel {
    padding: 16px;
  }

  .skill-tree-header {
    flex-direction: column;
  }

  .skill-tree-chart {
    height: 400px;
  }
}
</style>
