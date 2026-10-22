# server/ — 后端服务

Java 17 + Spring Boot 3.x，**模块化单体**。给一个家用 NAS 装微服务是过度设计，Nacos / Redis / MQ / Seata 一律不引入。

> **当前状态：空目录。** 没有 `pom.xml`、没有 `src/`、没有一行 Java。
> 以下为依据《详细设计 V1.7》第 9.2、23、24、26 章确定的**目标结构**，不是已实现内容。
> 当前不存在可运行的启动命令。

## 技术基线

| 项 | 选型 |
| --- | --- |
| JDK | Java 17 |
| 框架 | Spring Boot 3.x |
| Maven groupId / 根包 | `com.guoyu` / `com.guoyu.english` |
| ArtifactId | `guoyu-english-server` |
| ORM | MyBatis-Plus（MVP 首选）或 Spring Data JPA，二选一，别混 |
| 数据库 | PostgreSQL 16+ |
| 迁移 | Flyway，脚本放 `src/main/resources/db/migration` |

## 模块划分

每个模块内部统一四层：`controller / application / domain / infrastructure`。

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

统一前缀 `/api/v1`，JSON UTF-8。响应体固定 `requestId / code / message / data / serverTime`。

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
| POST | `/devices/pair` | 家长 PIN + 设备信息 → 设备令牌 |
| GET | `/content/packs` | 可用资源包与版本 |
| GET | `/content/packs/{id}/manifest` | 文件列表 + sha256 + size |
| POST | `/sync/push` | 批量上报，逐条 `eventId` 幂等 |
| GET | `/sync/pull?cursor=N&limit=100` | 增量快照 + nextCursor |
| GET | `/sync/status` | 上次同步时间、待同步数 |
| GET | `/system/health` | NAS 可达性与版本（仅非敏感信息） |

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

## 安全红线

- 禁止客户端直连数据库。
- 家长密码加盐强哈希；设备令牌可撤销、定期更换。
- 日志不记录录音内容、家庭 PIN、Bearer Token。
- 上传走 MIME 白名单 + 大小限制 + 路径规范化，防目录穿越；媒体目录默认只读。
- Flyway 只向前迁移。升级前备份 PG + 媒体目录。

## 启动（脚手架落地后）

```bash
cd server
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**现在跑不了**，`pom.xml` 还没生成。
