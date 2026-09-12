<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '../api/request'

const form = ref({ taskType: 'POINT_REVIEW', deviceId: '', priority: 2, remark: '' })
const tasks = ref([])
const msg = ref('')
let timer

async function load() {
  try {
    tasks.value = await request.get('/tasks')
  } catch (e) {
    console.error(e)
  }
}

async function submit() {
  msg.value = ''
  if (!form.value.deviceId) {
    msg.value = '请填写执行设备编号（如 ROBOT-001）'
    return
  }
  try {
    const t = await request.post('/tasks', {
      taskType: form.value.taskType,
      deviceId: form.value.deviceId,
      priority: Number(form.value.priority),
      remark: form.value.remark
    })
    msg.value = `任务已下发：${t.taskId}`
    load()
  } catch (e) {
    msg.value = '下发失败：' + (e.response?.data?.message || e.message)
  }
}

async function cancel(taskId) {
  try {
    await request.post(`/tasks/${taskId}/cancel`)
    load()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 10000)   // 任务列表 10s 刷新（设计报告 5.2.7）
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="card">
    <h3>下发巡检任务（指令经 Kafka → 设备执行回执）</h3>
    <div class="form-row">
      <select v-model="form.taskType">
        <option value="PERIMETER_PATROL">周界巡逻</option>
        <option value="AREA_COVER">区域覆盖</option>
        <option value="POINT_REVIEW">定点复核</option>
        <option value="RETURN_HOME">返航</option>
      </select>
      <input v-model="form.deviceId" placeholder="设备编号，如 ROBOT-001" style="width:220px" />
      <select v-model="form.priority">
        <option :value="1">优先级 1（高）</option>
        <option :value="2">优先级 2（中）</option>
        <option :value="3">优先级 3（低）</option>
      </select>
      <input v-model="form.remark" placeholder="备注" style="width:180px" />
      <button @click="submit">下发任务</button>
    </div>
    <div v-if="msg" class="hint">{{ msg }}</div>
  </div>

  <div class="card">
    <h3>任务列表（每 10 秒刷新）</h3>
    <table class="grid">
      <thead>
        <tr><th>任务编号</th><th>类型</th><th>执行设备</th><th>优先级</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="t in tasks" :key="t.taskId">
          <td>{{ t.taskId }}</td>
          <td>{{ t.taskType }}</td>
          <td>{{ t.deviceId }}</td>
          <td>{{ t.priority }}</td>
          <td><span class="badge pending">{{ t.status }}</span></td>
          <td>{{ (t.createTime || '').replace('T', ' ').slice(0, 19) }}</td>
          <td><button v-if="t.status === 'DISPATCHED'" class="ghost" @click="cancel(t.taskId)">取消</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
