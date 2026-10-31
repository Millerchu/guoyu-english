# server/ — 后端服务

Java 17 + Spring Boot 3.5.16，**模块化单体**。给一个家用 NAS 装微服务是过度设计，Nacos / Redis / MQ / Seata 一律不引入。

S01 已落地可编译的骨架：Maven Wrapper、统一响应、`GET /api/v1/system/health`、Flyway `V1__baseline.sql`。V1 只执行 `SELECT 1`，不建业务表。S06 在此之上增加身份：`auth`、`child` 和 Flyway `V3__family_child_device.sql`。课程、同步包还没有，也没有七天任务。集成测试用 Docker 里的 `postgres:16`。家庭 NAS 尚未验收。

## 技术基线

| 项 | 选型 |
| --- | --- |
| JDK | Java 17，编译目标 17 |
| 框架 | Spring Boot 3.5.16（2026-10-29 时 Maven Central 的 3.x 最新正式版） |
| Maven | Wrapper，发行版 3.9.16 |
| Maven groupId / 根包 | `com.guoyu` / `com.guoyu.english` |
| ArtifactId | `guoyu-english-server` |
| ORM | MyBatis-Plus 3.5.18。不引入 Spring Data JPA |
| 数据库 | PostgreSQL 16+。集成测试镜像 `postgres:16` |
| 迁移 | Flyway，脚本放 `src/main/resources/db/migration`。当前有 `V1__baseline.sql` 和 `V3__family_child_device.sql`。V2 留给课程 |

## 模块划分

每个模块内部统一四层：`controller / application / domain / infrastructure`。MyBatis-Plus 会扫描 `com.guoyu.english` 下带 `@Mapper` 的接口。身份和儿童档案使用注解 SQL，不把数据库行直接当作接口响应。

```
com.guoyu.english
├── auth          # 家长 PIN、设备会话、设备配对与撤销
├── child         # 儿童档案、时区、每日目标步数
├── course        # 主题 / 课节 / 步骤 / 词条
├── learning      # 今日任务、进度聚合、复习池
├── book          # 绘本、页面、阅读进度
├── phonics       # 自然拼读
├── game          # 游戏题目与结果
├── reward        # 星星账本、徽章、连续学习
├── calendar      # 打卡聚合、学习日历
├── report        # 家长周报 / 月报
├── content       # 离线资源包 manifest 与版本
├── sync          # Push / Pull / 幂等 / 变更日志
├── media         # 媒体资源元数据
├── speech        # 录音元数据（发音评分预留）
└── common        # 统一响应、异常、枚举、时间工具
```

## 接口目录

统一前缀 `/api/v1`，JSON UTF-8。响应体固定五个字段：`requestId`、`code`、`message`、`data`、`serverTime`。`serverTime` 为 UTC ISO-8601，以 `Z` 结尾。`requestId` 取请求头 `X-Request-Id`；缺失、空白，或去掉控制字符后为空时，服务端生成 UUID。传入值会去掉回车换行等控制字符，并截断到 128 个字符。响应头也会回传同一个 `X-Request-Id`。

业务码枚举在 `com.guoyu.english.common.api.ApiCode`，名称固定为 `OK`、`INVALID_ARGUMENT`、`DEVICE_REVOKED`、`EVENT_CONFLICT`、`EVENT_ID_REUSED`、`INTERNAL_ERROR`。本阶段成功响应使用 `OK`。HTTP 对照沿用详设第 24.4 节已列出的关系：`OK` 200、`INVALID_ARGUMENT` 400、`EVENT_CONFLICT` 与 `EVENT_ID_REUSED` 409、`INTERNAL_ERROR` 500。`DEVICE_REVOKED` 在详设里写成 401/403，这里的响应使用 401。

健康检查的 `data` 以开发计划共享契约为准，只有 `status=UP` 和构建版本 `version`（取 POM 版本）。详设 24.2 还写了 `ready`，本阶段没有另加字段，也没有改共享契约。

已实现：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/system/health` | 无需令牌。`data` 只有 `status` 与 `version`。不返回口令、地址、令牌或 `MEDIA_ROOT` |
| POST | `/api/v1/parent/bootstrap` | 正文是引导码和新 PIN，请求头带 `Idempotency-Key`。成功后创建唯一家庭并返回 15 分钟 `parentToken` |
| POST | `/api/v1/parent/children` | 家长会话。昵称、`ageBand`（`AGE_5` / `AGE_6` / `AGE_7`）、每日分钟数、IANA 时区。相同幂等键和相同载荷返回原儿童 |
| GET | `/api/v1/parent/children` | 家长会话。空库时 `data` 为 `[]` |
| POST | `/api/v1/parent/pair-codes` | 家长会话换 5 分钟一次性配对码，请求里带 `childIds` |
| POST | `/api/v1/devices/pair` | `pairCode`、`deviceId`、`platform`（`ANDROID` 或 `H5`）换设备令牌。正文出现 `pin` 或 `newPin` 时拒绝，并且不消费配对码 |
| POST | `/api/v1/parent/sessions` | 设备令牌加 PIN 换 15 分钟家长会话。不返回设备令牌 |
| GET | `/api/v1/parent/devices` | 家长会话。设备摘要，不含令牌 |
| DELETE | `/api/v1/parent/devices/{deviceId}` | 家长会话撤销设备。重复调用仍然是已撤销。撤销后该设备令牌访问受保护接口返回 `DEVICE_REVOKED` |

示例：

```json
{
  "requestId": "11111111-2222-4333-8444-555555555555",
  "code": "OK",
  "message": "OK",
  "data": {
    "status": "UP",
    "version": "0.0.1-SNAPSHOT"
  },
  "serverTime": "2026-10-29T14:47:00.123Z"
}
```

身份接口的成功响应也是上面的五字段外壳，HTTP 200，`code=OK`。详设里建档写了 201，共享契约的 `OK` 固定为 200，本阶段不新增业务码。同幂等键换了载荷时返回已有的 `EVENT_ID_REUSED`（409），对应详设里的 `IDEMPOTENCY_KEY_REUSED`，不重复建档。

建档响应包含 `childId`、`profileVersion=1`、`timezoneVersion=1` 和 `issuedThrough`。`issuedThrough` 目前为 null。详设要求建档时同时生成 7 日任务；共享契约把任务表放在 V5，并写明 `issued_through` 只在 V3 建列、由 S14 写入。本阶段不建课程表，也不因内容未就绪而拒绝建档。

引导码只放环境变量，仓库里的默认值是空字符串，空值表示不能建家庭。相同幂等键、相同 PIN 和仍然正确的引导码，在成功后 10 分钟内重试会返回同一个 `familyId`，并另签一张家长会话；明文令牌不入库。超过窗口或换一把幂等键都不再创建家庭。全凭证丢失后的恢复码属于 S22，这里没有实现。

家长 PIN 为 4–6 位数字，使用 Argon2id，随机盐，参数是 memory 64 MiB（`m=65536`）、iterations 3、parallelism 1。连续失败 5 次后按家庭和设备写入 `pin_attempt.locked_until`：30 秒、1 分钟、5 分钟、15 分钟。未满 5 次不锁定。锁定期间不再校验 PIN。配对码和令牌只存 SHA-256，PIN 和引导请求指纹走 Argon2id。日志不记录 PIN、引导码、配对码和令牌。

下面其余路径仍是目标目录，还没有控制器。

### 业务接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/home/today` | 首页今日课程、进度、奖励概览 |
| GET | `/lessons/{id}` | 课程与步骤 JSON |
| GET | `/books/{id}/pages` | 页码、分词、音频映射 |
| POST | `/books/{bookId}/progress` | 提交阅读进度 |
| GET | `/phonics/today` | 今日拼读任务 |
| POST | `/games/{gameId}/submit` | 提交小游戏结果 |
| GET | `/rewards/summary` | 星星余额、徽章 |
| GET | `/learning/calendar?month=YYYY-MM` | 按月打卡状态 |
| GET | `/learning/calendar/{date}` | 指定日期明细 |
| GET | `/parent/dashboard` | 家长中心概览 |
| GET | `/parent/reports/weekly` | 周报 |
| GET | `/parent/reports/monthly?month=YYYY-MM` | 月报 |

### 设备 / 同步 / 内容

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/devices/pair` | 已实现。配对码换设备令牌，不接受 PIN |
| GET | `/content/packs` | 可用资源包与版本 |
| GET | `/content/packs/{id}/manifest` | 文件列表 + sha256 + size |
| POST | `/sync/push` | 批量上报，逐条 `eventId` 幂等 |
| GET | `/sync/pull?cursor=N&limit=100` | 增量快照 + nextCursor |
| GET | `/sync/status` | 上次同步时间、待同步数 |
| GET | `/system/health` | 已实现，见上一节。进程存活与构建版本，不探测 NAS |

## 幂等与事务——这块做错整个项目就废了

**事件是「发生过什么」，投影表是「当前显示什么」。** 两者必须在同一个事务里。

单次 Push 对每条事件的处理顺序：

1. 校验设备与儿童绑定关系，禁止跨儿童写入。
2. `INSERT INTO learning_event ... ON CONFLICT (event_id) DO NOTHING`
3. `inserted == 0` → 返回 `DUPLICATE`，**不执行二次奖励**，直接结束。
4. `inserted == 1` → 更新进度 / 日历 / 奖励 → 写 `sync_change_log` → 提交。

两个不可动摇的约束：

- `learning_event.event_id` 是 UUID 主键，全局去重。客户端超时重试必须能安全拿到重复 ACK。
- `star_ledger(child_id, grant_key)` 唯一索引。`grantKey` 由服务端按 `childId:learningDay:rewardRule` 生成，保证同一天重复完成课程不会重复发签到星。

打卡**没有独立签到接口**。`daily_checkin` 由服务端按 `child_id + learning_day` 唯一聚合派生，儿童端点一下不该算签到。

## 关键表清单

`child_profile`、`client_device`、`course_theme`、`lesson`、`lesson_step`、`lexeme`、`media_asset`、`picture_book`、`picture_book_page`、`learning_event`、`daily_checkin`、`star_ledger`、`word_mastery`、`reading_progress`、`content_pack`、`content_pack_asset`、`sync_change_log`、`device_sync_cursor`、`speech_record`。

约定：

- 主键 `BIGINT`，家庭单实例用 Identity 就够，不必上雪花。
- 媒体字段存**相对路径或 storage_key**，绝不存 NAS 绝对路径。
- 学习记录追加写，不覆盖历史。
- 时间统一 `timestamptz`，儿童日归属按 `child_profile.timezone_id`（默认 `Asia/Shanghai`）计算。
- 枚举字段统一在 `common` 里定义，禁止魔法字符串。

## 配置项

与 `deploy/` 的 Compose 对齐，通过环境变量注入：

| 变量 | 示例 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | |
| `DB_HOST` | `db` | Compose 服务名 |
| `DB_PORT` | `5432` | 不对宿主机暴露 |
| `DB_NAME` | `guoyu_english` | |
| `DB_USER` | `guoyu` | 用受限业务账号，别用 postgres 超级用户 |
| `DB_PASSWORD` | `${DB_PASSWORD}` | 从 `.env` 读，不进仓库 |
| `MEDIA_ROOT` | `/data/media` | 媒体只读挂载 |
| `GUOYU_BOOTSTRAP_CODE` | `CHANGE_ME` | 一次性引导码。不要把真实值写进仓库或日志 |

## 安全红线

- 禁止客户端直连数据库。
- 家长 PIN 使用 Argon2id，参数见上文。4–6 位 PIN 只防误触，不能当成强密码。设备令牌可撤销。
- 日志不记录录音内容、家庭 PIN、引导码、配对码或 Bearer Token。
- 不要增加用 PIN 直接换设备令牌的接口。`/parent/sessions` 只换家长会话。
- 上传走 MIME 白名单 + 大小限制 + 路径规范化，防目录穿越；媒体目录默认只读。
- Flyway 只向前迁移。升级前备份 PG + 媒体目录。

## 启动与测试

在 `server/` 下执行。需要 JDK 17。`./mvnw test` 还需要本机 Docker，由 Testcontainers 启动 `postgres:16`。

```bash
cd server
./mvnw test
```

交互启动需要一套可连接的 PostgreSQL。口令和地址只放在环境变量里，不写入仓库：

```bash
cd server
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=guoyu_english
export DB_USER=guoyu
export DB_PASSWORD=CHANGE_ME
export MEDIA_ROOT=/data/media
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

目前没有单独的 `application-local.yml`。未设置 `DB_PASSWORD` 时口令为空，数据库拒绝连接则进程起不来。健康检查不回传这些配置。

```bash
curl -sS http://127.0.0.1:8080/api/v1/system/health
```

Compose、镜像和家庭 NAS 不在本阶段。
