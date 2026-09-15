<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { matchingApi, type MatchingCheckRecord } from '@/api'

const records = ref<MatchingCheckRecord[]>([])
const loading = ref(false)

const columns = [
  { prop: 'unitId', label: '单元ID' },
  { prop: 'washbasinId', label: '洗漱台ID' },
  { prop: 'checkType', label: '操作类型' },
  { prop: 'unitResidentCount', label: '单元居住人数' },
  { prop: 'totalCapacity', label: '总容纳容量' },
  { prop: 'checkResult', label: '校验结果' },
  { prop: 'checkMessage', label: '校验消息' },
  { prop: 'operator', label: '操作人' },
  { prop: 'checkTime', label: '校验时间' }
]

const getCheckTypeText = (type: string) => {
  switch (type) {
    case 'BIND': return '绑定'
    case 'UNBIND': return '解绑'
    case 'UPDATE': return '更新'
    case 'QUOTA_OPEN': return '定额开单'
    case 'QUOTA_APPEND': return '定额追加'
    case 'QUOTA_CLOSE': return '定额结案'
    default: return type
  }
}

const getCheckTypeColor = (type: string) => {
  switch (type) {
    case 'BIND': return 'success'
    case 'UNBIND': return 'warning'
    case 'QUOTA_OPEN': return 'primary'
    case 'QUOTA_APPEND': return 'danger'
    case 'QUOTA_CLOSE': return 'info'
    default: return 'info'
  }
}

const getResultColor = (result: string) => {
  switch (result) {
    case 'PASS': return 'success'
    case 'WARN': return 'warning'
    case 'FAIL': return 'danger'
    default: return 'info'
  }
}

const getResultText = (result: string) => {
  switch (result) {
    case 'PASS': return '通过'
    case 'WARN': return '预警'
    case 'FAIL': return '失败'
    case 'INFO': return '记录'
    default: return result
  }
}

const loadData = async () => {
  loading.value = true
  records.value = await matchingApi.getRecords()
  loading.value = false
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="loadData">刷新记录</el-button>
    </div>

    <el-table :data="records" border style="width: 100%" :loading="loading">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
        <template #default="scope">
          <template v-if="col.prop === 'checkType'">
            <el-tag size="small" :type="getCheckTypeColor(scope.row.checkType)">
              {{ getCheckTypeText(scope.row.checkType) }}
            </el-tag>
          </template>
          <template v-else-if="col.prop === 'checkResult'">
            <el-tag :type="getResultColor(scope.row.checkResult)">
              {{ getResultText(scope.row.checkResult) }}
            </el-tag>
          </template>
          <template v-else-if="col.prop === 'unitId'">
            {{ scope.row.unitId === 0 ? '楼栋级' : scope.row.unitId }}
          </template>
          <template v-else-if="col.prop === 'checkTime'">
            {{ new Date(scope.row.checkTime).toLocaleString() }}
          </template>
          <template v-else>
            {{ scope.row[col.prop] }}
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="records.length === 0" class="empty-state">
      <el-empty description="暂无校验记录" />
    </div>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  justify-content: flex-end;
}

.empty-state {
  padding: 40px;
}
</style>
