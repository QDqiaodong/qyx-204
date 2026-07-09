# 矿山职工宿舍公共洗漱台楼栋居住单元关联匹配系统

## 项目简介

本系统是矿区后勤管理公共洗漱台的核心管理系统，主要功能为居住单元与洗漱台容量匹配校验。系统支持洗漱台绑定楼栋、居住单元，录入可容纳人数；调整所属单元时自动校验单元居住人数与洗漱台容纳上限；支持按单元检索匹配洗漱设施，查看容量适配情况；生成单元设施适配情况一览。

## 核心功能

1. **公共洗漱台基础建档**：编号、可容纳人数、安装楼栋、位置信息管理
2. **居住单元初始绑定**：分配洗漱台对应职工居住单元
3. **单元归属调整**：自动校验人数容量匹配度并记录
4. **按单元查询配套洗漱设施**：查看容量适配情况
5. **导出单元容量适配一览**：生成匹配情况报表

## 技术栈

- **前端**：Vue 3 + Vite + Element Plus + TypeScript
- **后端**：Spring Boot 3.3 + JDK 17 + MyBatis-Plus + Redis
- **数据库**：MySQL 8.0
- **缓存**：Redis 7
- **容器编排**：Docker + Docker Compose

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+

### 启动方式

```bash
# 进入项目目录
cd qyx-204

# 启动所有服务
docker compose up -d --build

# 停止服务
docker compose down

# 查看日志
docker compose logs -f
```

### 访问地址

- **前端页面**：http://localhost:8124
- **后端API**：http://localhost:8134/api

### 端口配置

所有端口配置在 `.env` 文件中：

| 服务 | 端口 |
|------|------|
| 前端 | 8124 |
| 后端 | 8134 |
| MySQL | 3350 |
| Redis | 6423 |

## 目录结构

```
qyx-204/
├── backend/           # 后端代码
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/          # 前端代码
│   ├── src/
│   ├── Dockerfile
│   ├── package.json
│   └── vite.config.ts
├── sql/               # 数据库初始化脚本
│   └── init.sql
├── docker-compose.yml # Docker Compose配置
├── .env               # 环境变量配置
├── .gitignore
└── .dockerignore
```

## 容量匹配校验逻辑

当调整单元归属时，系统会自动执行以下校验：

1. 汇总目标单元已绑定的所有洗漱台的可容纳人数之和
2. 与单元居住人数进行比较
3. 根据使用率给出校验结果：
   - **PASS**：使用率低于85%，容量充足
   - **WARN**：使用率在85%-100%之间，接近上限
   - **FAIL**：使用率超过100%，容量不足

## API接口

### 楼栋管理

- `GET /api/buildings` - 获取所有楼栋列表

### 洗漱台管理

- `GET /api/washbasins` - 获取所有洗漱台列表
- `GET /api/washbasins/building/{buildingId}` - 获取指定楼栋的洗漱台
- `POST /api/washbasins` - 创建洗漱台
- `PUT /api/washbasins/{id}` - 更新洗漱台信息
- `DELETE /api/washbasins/{id}` - 删除洗漱台

### 居住单元管理

- `GET /api/living-units` - 获取所有居住单元列表
- `GET /api/living-units/building/{buildingId}` - 获取指定楼栋的居住单元
- `POST /api/living-units` - 创建居住单元
- `PUT /api/living-units/{id}` - 更新居住单元信息
- `DELETE /api/living-units/{id}` - 删除居住单元

### 匹配管理

- `POST /api/matching/bind` - 绑定洗漱台到单元
- `POST /api/matching/unbind` - 解绑洗漱台
- `GET /api/matching/check/{unitId}` - 校验单元容量
- `GET /api/matching/unit/{unitId}` - 获取单元匹配详情
- `GET /api/matching/units` - 获取所有单元匹配一览
- `GET /api/matching/records` - 获取校验记录

## 开发说明

### 后端开发

```bash
cd backend
mvn compile -q
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend
npm ci
npm run dev
```

## License

MIT License
