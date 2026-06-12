<template>
  <section class="focus-panel growth-dashboard">
    <div class="panel-topline">
      <span>Growth</span>
      <strong>{{ loading ? '读取中' : '求职成长看板' }}</strong>
    </div>

    <section v-if="dashboard" class="growth-hero">
      <div>
        <span>Overview</span>
        <strong>把零散记录变成一条清楚的求职成长线</strong>
        <p>{{ dashboard.summary }}</p>
      </div>
      <article>
        <span>Next Action</span>
        <strong>下一步</strong>
        <p>{{ dashboard.latestAction }}</p>
      </article>
    </section>

    <section v-if="dashboard" class="growth-action-strip" aria-label="快捷操作">
      <button type="button" @click="$emit('open-coach')">继续咨询</button>
      <button type="button" @click="$emit('open-archive')">查看简历版本</button>
      <button type="button" @click="$emit('open-interview')">开始面试训练</button>
      <button type="button" @click="$emit('open-report')">历史报告</button>
    </section>

    <section class="growth-metrics">
      <article v-for="metric in dashboard?.metrics || []" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.description }}</p>
      </article>
    </section>

    <section class="growth-grid">
      <article class="growth-card growth-trend-card">
        <div class="growth-card-head">
          <span>Score Trend</span>
          <strong>历史面试分数趋势</strong>
        </div>
        <div v-if="dashboard?.scoreTrend?.length" ref="lineChartRef" class="echart-container"></div>
        <p v-else class="growth-empty-text">完成模拟面试并保存优化报告后，这里会展示分数变化。</p>
      </article>

      <article class="growth-card">
        <div class="growth-card-head">
          <span>Ability Radar</span>
          <strong>能力雷达图</strong>
        </div>
        <div v-if="dashboard?.weakPoints?.length" ref="radarChartRef" class="echart-container"></div>
        <p v-else class="growth-empty-text">暂未发现明显薄弱点。多完成几轮模拟面试后会更准。</p>
      </article>
    </section>

    <section v-if="dashboard?.metrics?.length" class="growth-gauge-strip">
      <article class="growth-card">
        <div class="growth-card-head">
          <span>Average Score</span>
          <strong>平均面试分</strong>
        </div>
        <div ref="gaugeChartRef" class="echart-container echart-gauge"></div>
      </article>
    </section>

  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchGrowthDashboard } from '../../api/dashboard'

const dashboard = ref(null)
const loading = ref(false)
const lineChartRef = ref(null)
const radarChartRef = ref(null)
const gaugeChartRef = ref(null)

let lineChart = null
let radarChart = null
let gaugeChart = null

defineEmits(['open-coach', 'open-archive', 'open-interview', 'open-report'])

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  radarChart?.dispose()
  gaugeChart?.dispose()
})

function handleResize() {
  lineChart?.resize()
  radarChart?.resize()
  gaugeChart?.resize()
}

async function loadDashboard() {
  loading.value = true
  try {
    const response = await fetchGrowthDashboard()
    dashboard.value = response.data
    await nextTick()
    initCharts()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '读取成长看板失败')
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (!dashboard.value) return
  initLineChart()
  initRadarChart()
  initGaugeChart()
}

function initLineChart() {
  if (!lineChartRef.value || !dashboard.value?.scoreTrend?.length) return
  lineChart?.dispose()
  lineChart = echarts.init(lineChartRef.value)
  const trend = dashboard.value.scoreTrend
  lineChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>分数：{c}' },
    grid: { top: 20, right: 20, bottom: 30, left: 40 },
    xAxis: {
      type: 'category',
      data: trend.map(p => p.label),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: '#999d9c', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#999d9c', fontSize: 11 }
    },
    series: [{
      type: 'line',
      data: trend.map(p => p.score),
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#e2b04a', width: 3 },
      itemStyle: { color: '#e2b04a' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(226, 176, 74, 0.3)' },
          { offset: 1, color: 'rgba(226, 176, 74, 0.02)' }
        ])
      }
    }]
  })
}

function initRadarChart() {
  if (!radarChartRef.value || !dashboard.value?.weakPoints?.length) return
  radarChart?.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const points = dashboard.value.weakPoints
  const maxCount = Math.max(...points.map(p => p.count), 1)
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: points.map(p => ({ name: p.name, max: maxCount })),
      shape: 'polygon',
      axisName: { color: '#999d9c', fontSize: 12 },
      splitArea: { areaStyle: { color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.04)'] } },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: points.map(p => p.count),
        name: '薄弱点频次',
        lineStyle: { color: '#6db3b5', width: 2 },
        itemStyle: { color: '#6db3b5' },
        areaStyle: { color: 'rgba(109, 179, 181, 0.25)' }
      }]
    }]
  })
}

function initGaugeChart() {
  if (!gaugeChartRef.value || !dashboard.value?.metrics?.length) return
  gaugeChart?.dispose()
  gaugeChart = echarts.init(gaugeChartRef.value)
  const avgMetric = dashboard.value.metrics.find(m => m.label === '平均面试分')
  const value = avgMetric ? Number(avgMetric.value) || 0 : 0
  gaugeChart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      pointer: { show: false },
      progress: {
        show: true,
        width: 18,
        roundCap: true,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#e2b04a' },
            { offset: 1, color: '#f0d68a' }
          ])
        }
      },
      axisLine: { lineStyle: { width: 18, color: [[1, 'rgba(255,255,255,0.06)']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        fontSize: 36,
        fontWeight: 700,
        color: '#e2b04a',
        offsetCenter: [0, '10%'],
        formatter: '{value} 分'
      },
      data: [{ value }]
    }]
  })
}
</script>
