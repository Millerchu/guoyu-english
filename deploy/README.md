# deploy/ — 本机 Compose 与家庭局域网入口

S03 提供一套可检查的 Docker Compose：PostgreSQL 16、Spring Boot 服务端、Nginx 上的 H5 入口。家庭 NAS、可信 HTTPS、录音、备份和恢复都还没有验收。

操作步骤和健康检查示例见 [开发指南](../docs/development.md)。本文件说明这些文件实际做什么、哪些端口开着、哪些事情不能做。

## 当前文件

| 文件 | 作用 |
| --- | --- |
| [docker-compose.yml](docker-compose.yml) | 三个服务：`db`、`server`、`web` |
| [.env.example](.env.example) | 变量名和 `CHANGE_ME` 占位符。复制成 `.env` 后本地修改，`.env` 不入库 |
| [nginx/nginx.conf](nginx/nginx.conf) | H5、`/api/` 反代、`/media/` 只读素材 |
| [postgres/init/01-app-role.sh](postgres/init/01-app-role.sh) | 首次初始化时创建服务端使用的数据库角色 |
| [../server/Dockerfile](../server/Dockerfile) | 服务端镜像 |
| [../client/Dockerfile](../client/Dockerfile) | H5 镜像，构建时执行 `npm run build:h5` |

`backup/` 还没有。每日备份是 S21，不在本目录提前加脚本。

## 容器

| 服务 | 镜像 | 宿主机端口 | 持久化 |
| --- | --- | --- | --- |
| `db` | `postgres:16.15-alpine` | 不映射。容器内 5432 只在 Compose 网络 | 命名卷 `guoyu-postgres-data` |
| `server` | 本机构建 `guoyu-english-server:0.0.1` | 不映射。容器内 8080 | 媒体目录只读挂载 |
| `web` | 本机构建 `guoyu-english-web:0.0.1` | 默认 `127.0.0.1:8080` 转到容器 80 | 同一媒体目录只读挂载 |

`/volume/docker/guoyu-english` 只是说明「NAS 上会有一个数据目录」时的形状示例，不是当前用户的真实路径，Compose 里也没有写死它。数据库用 Docker 命名卷。媒体目录由 `MEDIA_HOST_DIR` 指向，本地默认是仓库的 `media-samples/`。

服务端通过环境变量 `DB_HOST=db`、`DB_PORT=5432`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`MEDIA_ROOT=/data/media` 连接。这些名字和 `server/src/main/resources/application.yml` 一致。引导账号 `POSTGRES_BOOTSTRAP_USER` 只在数据库容器第一次初始化时使用，服务端不拿它登录。

`/api/` 由 Nginx 原样转到 `server:8080`，请求 `/api/v1/system/health` 到达服务端时仍然是 `/api/v1/system/health`。

## 环境变量

| 变量 | 说明 |
| --- | --- |
| `HTTP_BIND` | 默认 `127.0.0.1`。局域网试用时改成 NAS 的局域网地址，不要用 `0.0.0.0` |
| `HTTP_PORT` | 默认 `8080`，只作为 HTTP 入口 |
| `POSTGRES_BOOTSTRAP_USER` | 容器内引导账号，默认 `postgres`，不要和 `DB_USER` 相同 |
| `POSTGRES_BOOTSTRAP_PASSWORD` | 引导口令。示例是 `CHANGE_ME` |
| `DB_NAME` | 默认 `guoyu_english` |
| `DB_USER` | 默认 `guoyu`。服务端只用这个账号 |
| `DB_PASSWORD` | 服务端口令。示例是 `CHANGE_ME` |
| `MEDIA_HOST_DIR` | 宿主机媒体目录，容器内只读挂到 `/data/media` |
| `TZ` | 默认 `Asia/Shanghai`，影响容器日志时钟。接口里的 `serverTime` 仍是 UTC |
| `SPRING_PROFILES_ACTIVE` | 默认 `prod`。仓库里没有单独的 `application-prod.yml` |

占位符原样出现在准备对局域网开放的配置里，说明还没改，不能那样启动。

## 启动

在 `deploy/` 目录：

```bash
cp .env.example .env
docker compose --env-file .env.example config >/tmp/guoyu-compose-config.txt
docker compose --env-file .env.example up -d --build
curl -sS http://127.0.0.1:8080/api/v1/system/health
```

`docker compose config` 会打印展开后的口令。用示例文件时只能看到 `CHANGE_ME`。已经写成真实口令之后，不要把这条命令的输出贴出去。

更完整的检查步骤、停机方式和升级注意在 [开发指南](../docs/development.md)。

## 安全边界

- 不对公网映射端口，不改路由器、UPnP、防火墙或 NAS 安全设置。
- 宿主机不监听 5432。客户端也不能直连数据库。
- 媒体目录以只读方式挂进 `server` 和 `web`。录音上传要单独的可写目录，那是以后的阶段，Nginx 现在没有公开可写的录音路径。
- `.env`、真实口令、证书私钥、家庭地址不入库。
- 本机 HTTP 打开首页，不等于录音可用。录音和 Service Worker 仍要设备信任的 HTTPS。
- 不执行 `docker compose down -v`，除非明确知道这会删掉数据库卷，并且这只是一次还没有学习数据的本地试验。

## 备份与恢复

还没有备份脚本。数据库卷和媒体目录需要分开备份，保留设想仍是以后的 S21。恢复未验证。Flyway 不能自动倒回旧结构。

## 资源

家庭单用户用 2 核 CPU、2–4 GB 内存可以起步。本阶段的服务端镜像把 Java 堆限制在容器内存的 75% 以内。这不是 NAS 上的实测容量。
