# deploy/ — NAS 部署配置

家庭 NAS 上的 Docker Compose 编排、Nginx 反向代理、环境变量模板与备份脚本。

> **当前状态：空目录。** 没有 `docker-compose.yml`，没有 `.env.example`，没有 `nginx/`。
> 根 README 不再链接不存在的 `deploy/examples/` 示例文件。正式 Compose 按 [开发计划](../docs/roadmap.md) 的 S03 再添加。
> 下面为依据《详细设计 V1.9》第 13、26.4 章确定的**目标拓扑**，不是已可运行的配置。

## 容器拓扑

| 容器 | 镜像 | 端口策略 | 持久化 |
| --- | --- | --- | --- |
| `guoyu-english-db` | `postgres:16-alpine` | **仅内部网络，不映射 5432** | `./postgres/data` |
| `guoyu-english-server` | 自构建 | 内部 8080 | `./media` 只读、`./app/logs` 可写 |
| `guoyu-english-web` | Nginx | 局域网 8080 | `./media` 只读挂载 |
| `guoyu-english-backup`（可选） | 定时任务 | 无 | `./backup` |

TLS 反代（NAS 自带反代或 Caddy/Nginx）是**推荐项而非可选项**——H5 录音和 Service Worker 必须走可信 HTTPS。

## NAS 目录结构

```
/volume/docker/guoyu-english/
├── docker-compose.yml
├── .env                    # 不入仓库
├── nginx/
│   └── conf.d/
├── postgres/
│   └── data/
├── media/
│   ├── image/
│   ├── audio/
│   ├── book/
│   └── speech/
├── app/
│   └── logs/
└── backup/
```

实际挂载点按 NAS 厂商存储路径确定，`/volume/docker` 只是 Synology 风格示例。

## 环境变量

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `DB_PASSWORD` | 是 | PostgreSQL 业务账号密码，**不要用示例值** |
| `DB_NAME` | 是 | 默认 `guoyu_english` |
| `DB_USER` | 是 | 默认 `guoyu`，限制权限，别用 `postgres` |
| `MEDIA_ROOT` | 是 | 容器内路径，默认 `/data/media` |
| `SPRING_PROFILES_ACTIVE` | 是 | `prod` |
| `TZ` | 建议 | `Asia/Shanghai`，影响日志与聚合任务 |

以后的 `.env.example` 只放 `CHANGE_ME` 这类可识别占位符。占位符原样出现在正式配置里，说明没改，不能上线。

## 首次部署步骤（拓扑确定后）

1. 在 NAS 上建好上面的目录树。
2. 复制 `.env.example` 为 `.env`，填写 `DB_PASSWORD` 等真实值，`chmod 600 .env`。
3. 构建并推送 `guoyu-english-server`、`guoyu-english-web` 镜像，或在本机构建后导入。
4. `docker compose up -d`。
5. 验证 `GET /api/v1/system/health` 返回正常。
6. 配内网域名 + 可信证书，再用平板浏览器验证 H5 录音可用。
7. 用 Android 端完成设备配对，下载一个离线包，然后**断网验证**。

## 安全红线

- **不向公网映射任何端口。** NAS 默认不暴露，关闭 UPnP 自动端口映射。
- 5432 只在 Docker 内部网络，宿主机上也别开。
- `.env` 不进仓库，不进备份包的公开部分。
- 媒体目录容器内只读挂载。
- 升级流程：备份 PG dump + 媒体清单快照 → 拉新镜像 → 启动 → 验证。失败回滚旧镜像 + 恢复备份。Flyway 只向前，别指望自动回滚。

## 备份与恢复

| 项 | 频率 | 位置 |
| --- | --- | --- |
| PostgreSQL dump | 每天 | 与数据库容器**不同的卷**或外部存储 |
| 媒体目录 | 每周增量校验 | 同上 |

恢复必须实际演练过。至少验证两个场景：**重复 Push 不重复加星**、**冲突合并不丢记录**。没演练过的备份等于没有备份。

## 资源建议

2 核 CPU / 2–4 GB 内存 / 10 GB 起步存储。家庭单用户压力极低，瓶颈永远是绘本和音频的体积。

## 待补文件

- `docker-compose.yml`（3 容器拓扑）
- `.env.example`
- `nginx/conf.d/guoyu-english.conf`（H5 静态资源 + `/api` 反代 + `/media` 静态）
- `backup/` 定时备份脚本

这些文件按 [开发计划](../docs/roadmap.md) 的 S03、S21 添加。根 README 已不再链接不存在的 V1.7 Compose 示例。
