<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/dashboard'
import { useEnergyTypeStore } from '@/store/energyType'

const energyTypeStore = useEnergyTypeStore()
const STATUS_LABEL: Record<string, string> = {
  PENDING_DISPATCH: '待派发',
  RECTIFYING: '整改中',
  PENDING_AUDIT: '待审核',
  COMPLETED: '已完成'
}

const stats = ref<any>({
  todayEmission: 0,
  todayExceeded: 0,
  activeOrders: 0,
  todayDiagnosis: 0,
  emissionTrend: [],
  energyPie: [],
  orderStatus: []
})

const trendRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
const barRef = ref<HTMLDivElement>()

let trendChart: any = null
let pieChart: any = null
let barChart: any = null
let timer: number | undefined

const load = async () => {
  await energyTypeStore.ensureLoaded()
  const data: any = await getDashboardStats()
  stats.value = data
  renderCharts()
}

const renderCharts = () => {
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption(
      {
        color: ['#2f9e75'],
        tooltip: { trigger: 'axis' },
        grid: { left: 56, right: 20, top: 36, bottom: 32 },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: stats.value.emissionTrend.map((x: any) => x.timeLabel),
          axisLabel: { color: '#86909c' }
        },
        yAxis: {
          type: 'value',
          name: 'kgCO2e',
          axisLabel: { color: '#86909c' },
          splitLine: { lineStyle: { color: '#eef1f4' } }
        },
        series: [
          {
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            areaStyle: { opacity: 0.18 },
            data: stats.value.emissionTrend.map((x: any) => Number(x.value))
          }
        ]
      },
      true
    )
  }

  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption(
      {
        color: ['#2f9e75', '#f2a541', '#4c7cf3', '#9b5de5', '#e76f51', '#00a6a6'],
        tooltip: { trigger: 'item', formatter: '{b}: {c} kgCO2e ({d}%)' },
        legend: { bottom: 0 },
        series: [
          {
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '44%'],
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { formatter: '{b}\n{d}%' },
            data: stats.value.energyPie.map((x: any) => ({
              name: energyTypeStore.labelOf(x.name) || x.name,
              value: Number(x.value)
            }))
          }
        ]
      },
      true
    )
  }

  if (barRef.value) {
    barChart = barChart || echarts.init(barRef.value)
    const orderData = stats.value.orderStatus.map((x: any) => ({
      name: STATUS_LABEL[x.name] || x.name,
      value: Number(x.value)
    }))
    barChart.setOption(
      {
        color: ['#2f9e75'],
        tooltip: { trigger: 'axis' },
        grid: { left: 48, right: 20, top: 24, bottom: 32 },
        xAxis: { type: 'category', data: orderData.map((x: any) => x.name), axisLabel: { color: '#86909c' } },
        yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#86909c' }, splitLine: { lineStyle: { color: '#eef1f4' } } },
        series: [{ type: 'bar', barWidth: 42, itemStyle: { borderRadius: [6, 6, 0, 0] }, data: orderData.map((x: any) => x.value) }]
      },
      true
    )
  }
}

const onResize = () => {
  trendChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  load()
  timer = window.setInterval(load, 30000)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h2>碳排总览</h2>
        <div class="sub">实时监测 · AI 智能诊断 · 整改闭环，每 30 秒自动刷新</div>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="label">今日碳排放总量</div>
        <div class="value">
          {{ Number(stats.todayEmission).toFixed(1) }}<span class="unit">kgCO2e</span>
        </div>
        <el-icon class="icon" color="#2f9e75"><TrendCharts /></el-icon>
      </div>
      <div class="stat-card">
        <div class="label">今日超标次数</div>
        <div class="value">
          {{ stats.todayExceeded }}<span class="unit">次</span>
        </div>
        <el-icon class="icon" color="#f2a541"><Warning /></el-icon>
      </div>
      <div class="stat-card">
        <div class="label">进行中整改工单</div>
        <div class="value">
          {{ stats.activeOrders }}<span class="unit">张</span>
        </div>
        <el-icon class="icon" color="#4c7cf3"><Tickets /></el-icon>
      </div>
      <div class="stat-card">
        <div class="label">今日 AI 诊断次数</div>
        <div class="value">
          {{ stats.todayDiagnosis }}<span class="unit">次</span>
        </div>
        <el-icon class="icon" color="#9b5de5"><MagicStick /></el-icon>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-card">
        <h3>近 24 小时碳排放趋势</h3>
        <div ref="trendRef" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <h3>能源类型碳排占比</h3>
        <div ref="pieRef" class="chart-box small"></div>
      </div>
    </div>

    <div class="chart-card">
      <h3>整改工单状态分布</h3>
      <div ref="barRef" class="chart-box" style="height: 240px"></div>
    </div>
  </div>
</template>
