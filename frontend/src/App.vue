<script setup lang="ts">
import { ref } from 'vue'
import WashbasinManage from './components/WashbasinManage.vue'
import UnitManage from './components/UnitManage.vue'
import MatchingManage from './components/MatchingManage.vue'
import MatchingList from './components/MatchingList.vue'
import RecordsList from './components/RecordsList.vue'

const activeTab = ref('washbasin')

const tabs = [
  { label: '洗漱台管理', value: 'washbasin' },
  { label: '居住单元管理', value: 'unit' },
  { label: '绑定匹配', value: 'matching' },
  { label: '匹配一览', value: 'list' },
  { label: '校验记录', value: 'records' }
]
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>矿山职工宿舍公共洗漱台匹配系统</h1>
    </header>
    <nav class="app-nav">
      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>
    </nav>
    <main class="app-main">
      <WashbasinManage v-if="activeTab === 'washbasin'" />
      <UnitManage v-else-if="activeTab === 'unit'" />
      <MatchingManage v-else-if="activeTab === 'matching'" />
      <MatchingList v-else-if="activeTab === 'list'" />
      <RecordsList v-else-if="activeTab === 'records'" />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.app-header {
  background-color: #1e40af;
  color: white;
  padding: 20px 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.app-nav {
  background-color: white;
  padding: 10px 30px;
  border-bottom: 1px solid #e0e0e0;
}

.app-main {
  padding: 20px 30px;
}
</style>