#!/bin/bash
# 启动本地 PostgreSQL 实例（首次自动初始化），端口 5433
set -e
PGBIN=/usr/lib/postgresql/15/bin
PGDATA="$(cd "$(dirname "$0")/.." && pwd)/.pgdata"

if [ ! -d "$PGDATA/base" ]; then
  echo "[db] 初始化 PostgreSQL 数据目录: $PGDATA"
  mkdir -p "$PGDATA"
  $PGBIN/initdb -D "$PGDATA" -U postgres --auth=trust --encoding=UTF8 --locale=C > /dev/null
  sed -i "s/^#port = 5432/port = 5433/" "$PGDATA/postgresql.conf"
  sed -i "s/^#listen_addresses = 'localhost'/listen_addresses = 'localhost'/" "$PGDATA/postgresql.conf"
  echo "unix_socket_directories = '/tmp'" >> "$PGDATA/postgresql.conf"
fi

if $PGBIN/pg_ctl -D "$PGDATA" status > /dev/null 2>&1; then
  echo "[db] PostgreSQL 已在运行 (端口 5433)"
else
  $PGBIN/pg_ctl -D "$PGDATA" -l "$PGDATA/server.log" start
fi

# 建库与账号（幂等）
$PGBIN/psql -h localhost -p 5433 -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='manhole'" | grep -q 1 || \
  $PGBIN/psql -h localhost -p 5433 -U postgres -c "CREATE DATABASE manhole"
$PGBIN/psql -h localhost -p 5433 -U postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='manhole_app'" | grep -q 1 || \
  $PGBIN/psql -h localhost -p 5433 -U postgres -c "CREATE USER manhole_app WITH PASSWORD 'manhole123'"
$PGBIN/psql -h localhost -p 5433 -U postgres -d manhole -c "ALTER DATABASE manhole OWNER TO manhole_app" > /dev/null
$PGBIN/psql -h localhost -p 5433 -U postgres -d manhole -c "GRANT ALL ON SCHEMA public TO manhole_app" > /dev/null
echo "[db] 就绪: jdbc:postgresql://localhost:5433/manhole (manhole_app/manhole123)"
