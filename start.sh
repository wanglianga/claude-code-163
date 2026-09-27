#!/bin/bash
# 一键启动：PostgreSQL + 后端(8080) + 前端(5173)
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"

bash "$ROOT/scripts/start-db.sh"

echo "[backend] 启动 Spring Boot (端口 8080) ..."
cd "$ROOT/backend"
nohup mvn spring-boot:run > /tmp/manhole-backend.log 2>&1 &

echo "[frontend] 启动 Vite (端口 5173) ..."
cd "$ROOT/frontend"
if [ ! -d node_modules ]; then
  npm install --cache "$ROOT/.npm-cache"
fi
nohup npx vite --port 5173 > /tmp/manhole-frontend.log 2>&1 &

echo ""
echo "等待后端就绪 ..."
for i in $(seq 1 90); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login -X POST \
    -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>/dev/null)
  [ "$code" = "200" ] && break
  sleep 2
done
echo "后端: http://localhost:8080  前端: http://localhost:5173"
echo "日志: /tmp/manhole-backend.log  /tmp/manhole-frontend.log"
