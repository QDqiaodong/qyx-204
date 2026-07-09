<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buildingApi, washbasinApi, type Building, type Washbasin } from '@/api'

const buildings = ref<Building[]>([])
const washbasins = ref<Washbasin[]>([])
const form = ref<Partial<Washbasin>>({})
const visible = ref(false)
const isEdit = ref(false)
const searchBuildingId = ref<number | null>(null)

const columns = [
  { prop: 'washbasinCode', label: '洗漱台编号' },
  { prop: 'capacity', label: '可容纳人数' },
  { prop: 'buildingName', label: '所属楼栋' },
  { prop: 'location', label: '安装位置' },
  { prop: 'status', label: '状态', formatter: (row: Washbasin & { buildingName?: string }) => row.status === 1 ? '正常' : '停用' },
  { prop: 'createdAt', label: '创建时间', formatter: (row: Washbasin & { buildingName?: string }) => row.createdAt ? new Date(row.createdAt).toLocaleString() : '' }
]

const loadBuildings = async () => {
  buildings.value = await buildingApi.getAll()
}

const loadWashbasins = async () => {
  const data = await washbasinApi.getAll()
  washbasins.value = data.map(w => ({
    ...w,
    buildingName: buildings.value.find(b => b.id === w.buildingId)?.buildingName || ''
  }))
}

const handleAdd = () => {
  form.value = { status: 1 }
  isEdit.value = false
  visible.value = true
}

const handleEdit = (row: Washbasin) => {
  form.value = { ...row }
  isEdit.value = true
  visible.value = true
}

const handleDelete = async (row: Washbasin) => {
  await ElMessageBox.confirm('确定要删除该洗漱台吗？', '提示', { type: 'warning' })
  await washbasinApi.delete(row.id)
  ElMessage.success('删除成功')
  loadWashbasins()
}

const handleSubmit = async () => {
  if (!form.value.washbasinCode || !form.value.capacity || !form.value.buildingId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (isEdit.value) {
    await washbasinApi.update(form.value.id!, form.value)
    ElMessage.success('更新成功')
  } else {
    await washbasinApi.create(form.value as Omit<Washbasin, 'id'>)
    ElMessage.success('创建成功')
  }
  visible.value = false
  loadWashbasins()
}

const handleSearch = async () => {
  if (searchBuildingId.value) {
    const data = await washbasinApi.getByBuildingId(searchBuildingId.value)
    washbasins.value = data.map(w => ({
      ...w,
      buildingName: buildings.value.find(b => b.id === w.buildingId)?.buildingName || ''
    }))
  } else {
    loadWashbasins()
  }
}

onMounted(() => {
  loadBuildings()
  loadWashbasins()
})
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="searchBuildingId" placeholder="按楼栋筛选" clearable @change="handleSearch">
        <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
      </el-select>
      <el-button type="primary" @click="handleAdd">新增洗漱台</el-button>
    </div>
    <el-table :data="washbasins" border style="width: 100%">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
        <template #default="scope">
          {{ col.formatter ? col.formatter(scope.row) : scope.row[col.prop] }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="isEdit ? '编辑洗漱台' : '新增洗漱台'" v-model="visible">
      <el-form :model="form" label-width="100px">
        <el-form-item label="洗漱台编号" required>
          <el-input v-model="form.washbasinCode" placeholder="请输入洗漱台编号" />
        </el-form-item>
        <el-form-item label="可容纳人数" required>
          <el-input-number v-model="form.capacity" :min="1" placeholder="请输入可容纳人数" />
        </el-form-item>
        <el-form-item label="所属楼栋" required>
          <el-select v-model="form.buildingId" placeholder="请选择楼栋">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="安装位置">
          <el-input v-model="form.location" placeholder="请输入安装位置" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :label="1" :value="1">正常</el-option>
            <el-option :label="0" :value="0">停用</el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  justify-content: flex-end;
}
</style>
