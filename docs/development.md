# 本机开发：Compose 拉起数据库、服务和网页

用途：在一台已经安装 Docker 的电脑上，用一条编排同时看到 PostgreSQL、Spring Boot 和 H5 入口。

- 适用版本：开发计划 S03。服务端仍是 S01 的健康检查和 Flyway V1，页面仍是 S02 的首页壳。
- 范围：本机回环地址上的 HTTP。不是家庭 NAS 验收，不是可信 HTTPS，也不是录音可用。
- 结论：网页入口是 `http://127.0.0.1:8080`。`/api/` 原样转到服务端 `/api/`。数据库不对宿主机开放 5432。课程素材只读挂载。

部署文件的说明在 [deploy/README.md](../deploy/README.md)。下面的命令都在仓库根目录准备好之后，进入 `deploy/` 执行。

## 你需要先有什么

- Docker Engine 和 Docker Compose v2。命令是 `docker compose`，不是旧的 `docker-compose`。
- 能访问 Docker Hub、Maven Central 和 npm，因为第一次会拉取基础镜像并在镜像里构建。
- 不要把真实口令、家庭地址或 NAS 路径写进 Git。

本指南不要求改 Java 或页面。镜像起不来时只修 Dockerfile、Nginx 和 Compose。

## 第一次启动

1. 克隆仓库并进入目录。试用某个 Pull Request 时，先切到那个分支。
2. 复制环境变量模板。这一步生成的 `.env` 已经被 `deploy/.gitignore` 忽略。

```bash
cd deploy
cp .env.example .env
```

3. 本机只想看健康检查和首页时，可以暂时留下 `.env` 里的 `CHANGE_ME`。入口默认是 `127.0.0.1:8080`，数据库端口不公布。
4. 准备把入口开到局域网之前，先把 `POSTGRES_BOOTSTRAP_PASSWORD` 和 `DB_PASSWORD` 改成两组不同的口令，并把 `HTTP_BIND` 改成这台机器的局域网地址。不要写成 `0.0.0.0`。不要在路由器上做端口转发，也不要打开 UPnP。
5. 先检查编排。这条命令会把 `.env` 里的值展开，所以**只对还是占位符的文件做，或者自己看完就删掉输出**。不要把含真实口令的结果贴到聊天、Issue 或 Pull Request。

```bash
docker compose --env-file .env.example config >/tmp/guoyu-compose-config.txt
```

检查这份输出时确认三件事：口令位置只有 `CHANGE_ME` 这种占位符；`db` 服务没有 `ports` 或 `published`；`web` 只把 `127.0.0.1:8080` 转到容器的 80。

6. 构建并启动。第一次会下载基础镜像，并在镜像里执行 Maven 和 `npm run build:h5`。

```bash
docker compose --env-file .env.example up -d --build
```

自己的 `.env` 已经改过时，去掉 `--env-file .env.example`。Compose 会自动读取同目录的 `.env`。

7. 看状态。`db` 和 `server` 的端口列应该是空的，只有 `web` 显示 `127.0.0.1:8080->80/tcp`。

```bash
docker compose ps
docker compose logs server
```

## 健康检查

```bash
curl -sS http://127.0.0.1:8080/api/v1/system/health
```

成功时 HTTP 200，JSON 恰好五个字段，和 S01 相同：

```json
{
  "requestId": "由服务端生成的 UUID",
  "code": "OK",
  "message": "OK",
  "data": {
    "status": "UP",
    "version": "0.0.1-SNAPSHOT"
  },
  "serverTime": "2026-10-31T00:00:00.000Z"
}
```

`serverTime` 是 UTC，以 `Z` 结尾。`data` 只有 `status` 和 `version`，没有口令、地址、令牌或媒体路径。容器时区 `Asia/Shanghai` 只影响日志时钟，不改变这个字段。

这条路径经过 Nginx：浏览器的 `/api/v1/system/health` 到达服务端时仍然是 `/api/v1/system/health`。若少了 `v1` 或变成 `/api/api/v1`，就是反代写错了。

首页：

```bash
curl -sS -D - http://127.0.0.1:8080/ -o /tmp/guoyu-index.html
```

响应要是 S02 构建出来的页面，标题里有「果语」。看到 Nginx 的 “Welcome to nginx” 说明网页镜像没有拷进 H5 产物。

示例音频（只读媒体）：

```bash
curl -sS -D - -o /dev/null http://127.0.0.1:8080/media/audio/animals/cat-v1.mp3
```

期望 `200` 和 `Content-Type: audio/mpeg`。这是仓库里的演示素材，不是孩子的录音。

## 确认数据库没有暴露

```bash
docker port guoyu-english-db
```

这条没有输出。`docker ps` 里数据库那一行可能出现 `5432/tcp`，那是镜像声明的容器内端口，前面没有 `127.0.0.1:` 或 `0.0.0.0:`。真正映射到宿主机的只有网页，形状是 `127.0.0.1:8080->80/tcp`。

不要用 `docker compose port db 5432` 判断。Compose 2.40 在端口没有映射时仍可能打印 `:0` 并以退出码 0 结束，这不表示宿主机开了 5432。

在宿主机上连接 `127.0.0.1:5432` 应该被拒绝。服务端在 Compose 网络里用主机名 `db`、端口 `5432` 连接，这个端口只存在于容器之间。

## 录音和 HTTPS

`http://127.0.0.1:8080` 只说明网页和健康检查能在本机打开。H5 录音和 Service Worker 需要设备信任的 HTTPS。用局域网 IP 的普通 HTTP 打开页面，不能当成录音已经可用，也不能写成平板或 NAS 验收。

## 停掉

```bash
docker compose stop
```

`docker compose down` 会删掉容器和网络，命名卷 `guoyu-english_guoyu-postgres-data` 还在，下次启动仍能看到 Flyway 已经应用到 V1。

不要对已经有学习数据的环境执行 `docker compose down -v`。`-v` 会删掉数据库卷。这份指南的验证环境如果是刚用占位口令拉起的，删掉它的卷只影响这次试验；家庭 NAS 上没有演练过的删除都不做。

## 镜像

| 镜像 | 用途 |
| --- | --- |
| `postgres:16.15-alpine` | 数据库。固定补丁标签，不用 `latest` |
| `maven:3.9.16-eclipse-temurin-17` | 只在构建服务端时使用 |
| `eclipse-temurin:17.0.20.1_1-jre-alpine` | 服务端运行时，Java 17 |
| `node:20.20.2-bookworm` | 只在构建 H5 时使用 |
| `nginx:1.30.6-alpine` | 网页入口 |
| `guoyu-english-server:0.0.1` | 本机构建的服务端，不推送到公共仓库 |
| `guoyu-english-web:0.0.1` | 本机构建的网页，里面是 `npm run build:h5` 的产物 |

这些官方标签带多架构清单。本文的运行记录是 linux/amd64。ARM 家庭 NAS 还没有构建过。

## 升级时注意

先备份数据库和媒体清单，再换镜像。Flyway 只向前，不能靠换回旧镜像撤销已经执行的迁移。备份目录要和数据库卷分开。备份脚本、恢复演练都还没有，恢复未验证，那是 S21、S22 的事。

## 验证记录

2026-10-31，在 linux/amd64 的云端代理环境执行。Docker Engine 29.1.3，Compose 2.40.3。使用 `deploy/.env.example`，没有另写 `.env`。

| 检查 | 结果 |
| --- | --- |
| `docker compose --env-file .env.example config` | 通过。口令字段只有占位符 `CHANGE_ME`。`web` 只发布 `127.0.0.1:8080`。媒体挂载为只读 |
| `docker compose --env-file .env.example up -d --build` | 通过。三个镜像都构建出来 |
| 数据库健康后重启服务端 | 通过。日志 `Started GuoyuEnglishApplication in 1.774 seconds`。Flyway 报告 schema 已在版本 1，没有再执行一次迁移 |
| `GET http://127.0.0.1:8080/api/v1/system/health` | HTTP 200。五个字段齐全，`code=OK`，`data.status=UP`，`data.version=0.0.1-SNAPSHOT`，`serverTime` 为 UTC 且以 `Z` 结尾。响应里没有口令、地址或令牌。请求头 `X-Request-Id: verify-s03-restart` 原样回到 `requestId` |
| 首页 `GET /` | HTTP 200，标题是「果语」，不是 Nginx 默认页。构建产物里的首页脚本含有「听一听」 |
| `GET /media/audio/animals/cat-v1.mp3` | HTTP 200，`Content-Type: audio/mpeg`，文件头是 `ID3`，21850 字节 |
| `GET /media/` | HTTP 403，没有目录列表 |
| 容器内写入 `/data/media` | `server` 和 `web` 都返回 `Read-only file system` |
| `docker port guoyu-english-db` | 无输出。连接 `127.0.0.1:5432` 被拒绝 |
| 数据库角色 | `guoyu` 不是超级用户。用这个账号能读到 `flyway_schema_history` 的 V1，`success` 为真 |
| 三个容器的日志 | 没有出现占位口令 |

这次验证没有做：家庭 NAS、ARM 架构、可信 HTTPS、录音、平板或 Android、学习闭环、备份和恢复。本机 HTTP 不能当成录音已经可用。

云端环境第一次启动时，容器之间的网络被宿主机旧的防火墙默认策略拦住，服务端连不上数据库。放行转发之后，同一套 Compose 在数据库健康检查通过后约 2 秒启动完成。这是那台机器的防火墙问题，不是要写进家庭 NAS 步骤的操作。Docker Desktop 或正常的 NAS Docker 不需要额外改防火墙才能让 Compose 网络里的服务互相访问。
