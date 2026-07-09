#!/bin/bash

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "项目目录: $PROJECT_DIR"
echo ""

echo "=== 检查端口占用情况 ==="
for port in 8124 8134 3350 6423; do
    if lsof -i ":$port" > /dev/null 2>&1; then
        echo "端口 $port 被占用，查找占用进程:"
        lsof -i ":$port" | grep LISTEN
        exit 1
    else
        echo "端口 $port 可用"
    fi
done
echo ""

echo "=== 后端单独编译 ==="
cd "$PROJECT_DIR/backend"
echo "执行: mvn compile -q"
mvn compile -q
if [ $? -eq 0 ]; then
    echo "后端编译成功"
else
    echo "后端编译失败"
    exit 1
fi
echo ""

echo "=== 前端单独构建 ==="
cd "$PROJECT_DIR/frontend"
echo "执行: npm ci"
npm ci
echo ""
echo "执行: npm run build"
npm run build
if [ $? -eq 0 ]; then
    echo "前端构建成功"
else
    echo "前端构建失败"
    exit 1
fi
echo ""

echo "=== Docker整体构建启动 ==="
cd "$PROJECT_DIR"
echo "执行: docker compose up -d --build"
docker compose up -d --build
echo ""

echo "=== 等待服务启动 ==="
sleep 30

echo "=== 检查容器状态 ==="
docker compose ps
echo ""

echo "=== curl验证 ==="
echo "验证 localhost:8134/api/buildings"
curl -s http://localhost:8134/api/buildings | head -50
echo ""
echo ""
echo "验证 127.0.0.1:8134/api/buildings"
curl -s http://127.0.0.1:8134/api/buildings | head -50
echo ""
echo ""

echo "=== 前端访问验证 ==="
echo "验证 localhost:8124"
curl -s http://localhost:8124 | head -20
echo ""
echo ""
echo "验证 127.0.0.1:8124"
curl -s http://127.0.0.1:8124 | head -20
echo ""

echo "=== 启动完成 ==="
echo "前端地址: http://localhost:8124"
echo "后端API: http://localhost:8134/api"
