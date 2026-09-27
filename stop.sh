#!/bin/bash
# 停止后端与前端（PostgreSQL 保留运行，可用 scripts/start-db.sh 管理）
pkill -f "[s]pring-boot:run" 2>/dev/null
pkill -f "[m]anhole-platform" 2>/dev/null
pkill -f "[v]ite --port 5173" 2>/dev/null
echo "已停止后端与前端"
