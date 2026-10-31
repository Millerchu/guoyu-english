#!/bin/sh
# 首次初始化时创建服务端使用的数据库角色。
# 引导账号只留在数据库容器里。服务端连接的是 DB_USER，不是引导账号。
# 口令只从环境变量读入，不写进仓库，成功时也不打印。

set -eu

case "${DB_USER:-}" in
  ""|*[!a-z0-9_]*)
    echo "DB_USER must match [a-z0-9_]+" >&2
    exit 1
    ;;
esac

case "${DB_NAME:-}" in
  ""|*[!a-z0-9_]*)
    echo "DB_NAME must match [a-z0-9_]+" >&2
    exit 1
    ;;
esac

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "DB_PASSWORD is empty" >&2
  exit 1
fi

if [ "$DB_USER" = "${POSTGRES_USER}" ]; then
  echo "DB_USER must differ from POSTGRES_BOOTSTRAP_USER" >&2
  exit 1
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres >/dev/null <<EOF
\getenv app_password DB_PASSWORD
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', '${DB_USER}', :'app_password') \gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', '${DB_NAME}', '${DB_USER}') \gexec
EOF

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB_NAME" >/dev/null <<EOF
SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', '${DB_USER}') \gexec
EOF
