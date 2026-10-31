# 果语开发计划

用途：把《详细设计 V1.9》和动物示例课拆成可以交给不同助手、单独开 Pull Request、单独验收的任务卡。

- 编写日期：2026-10-29
- 设计依据：[V1.9 详细设计](设计文档/guoyu-english-detailed-design-v1.9.docx)、[V1.9 修订对照](20261023-详细设计V1.9修订对照.md)。第 23 章是唯一持久化模型，第 24 章是唯一接口契约，第 27.3 节是详设里的里程碑。本文件是实施切片，不替代这三处。
- 应用状态：S01、S02、S05、S06 已合并。服务端可以编译并回答健康检查，也可以从空库完成引导、建档和配对。H5 首页可以构建，动物课示范音频已经生成。还没有 Compose，也没有学习闭环。以进度表为准。

阶段大小按一次实现运行能完成来切，不按日历周数估计。详设里的 M0–M4 周数仍只是未验证的粗估。

## 怎么把一张任务卡交给另一个助手

1. 先看「进度表」。只领状态为「可开始」的阶段。状态是「等待依赖」时不要开工。
2. 每个助手只领一个阶段。开工前只读「共享契约」和自己的任务卡。领任务时先把进度表里自己那一行改为「进行中」，并写上负责助手；这个改动与该阶段的代码放在同一个分支里。
3. 从最新 `main` 拉新分支。不要复用已经合并过的旧分支。基线没合并就不要开始。
4. 只改任务卡「产出」里的路径。契约里的 ID、路径、JSON 字段和表名不够用时，先单独提文档 PR 改「共享契约」，合并后再改代码。功能 PR 里不要顺手改契约。
5. 迁移版本号以契约里的预分配为准，不要占用别人的版本号。
6. PR 说明写实际跑过的命令和结果。没跑的验收项写未执行。
7. 每个阶段的 PR 在合并前，于同一个 PR 里把进度表该行改为「已合并」，填上 PR 链接和合并日期。依赖因此已经满足的其他行改为「可开始」。不要在依赖尚未合并时把后面的阶段写成可开始。

## 进度表

更新规则：

- 助手开工时，先把自己这一行改为「进行中」，并填写负责助手。负责助手可以留空，但一旦领取就写上，避免两个人做同一阶段。
- 该阶段的 PR 在合并前，必须在同一个 PR 里把这一行改为「已合并」，并填上 PR 链接和合并日期。
- 某一行改为「已合并」后，按「波次」表检查其他行。依赖都已合并的阶段改为「可开始」，其余保持「等待依赖」。
- 状态只有这四种：已合并、进行中、可开始、等待依赖。

本表核对日期：2026-10-31。S01、S02、S05、S06 已合并。因此现在可以开始的是：S03（等待 S01 和 S02）、S04（等待 S01；S05 已合并，音频哈希可以直接用）、S09（等待 S02）。S07 和 S08 还要等 S04，所以仍是等待依赖。S10 还要等 S09。

| 阶段 | 一句话 | 状态 | PR | 负责助手 | 合并日期 |
| --- | --- | --- | --- | --- | --- |
| S01 | 后端空壳、健康检查、Flyway 基线 | 已合并 | [#3](https://github.com/Millerchu/guoyu-english/pull/3) | | 2026-10-29 |
| S02 | 客户端空壳，H5 能构建 | 已合并 | [#2](https://github.com/Millerchu/guoyu-english/pull/2) | | 2026-10-29 |
| S03 | Compose 拉起数据库、服务和网页入口 | 可开始 | | | |
| S04 | 动物课表、五个自制 SVG、课程种子 | 可开始 | | | |
| S05 | 可替换慢速 TTS 和十段示例音频 | 已合并 | [#4](https://github.com/Millerchu/guoyu-english/pull/4) | | 2026-10-29 |
| S06 | 空库引导、建档、配对码 | 已合并 | [#8](https://github.com/Millerchu/guoyu-english/pull/8) | Cursor Agent | 2026-10-31 |
| S07 | 幂等同步、句子跟读星、每日贴纸 | 等待依赖 | | | |
| S08 | 当天三步任务和首页接口 | 等待依赖 | | | |
| S09 | 本地状态与 outbox | 可开始 | | | |
| S10 | 听、说、认、完成页 | 等待依赖 | | | |
| S11 | 端云联调 | 等待依赖 | | | |
| S12 | 读一句页面和一局课程小游戏 | 等待依赖 | | | |
| S13 | 麦克风失败时跳过 | 等待依赖 | | | |
| S14 | 冻结未来七天任务 | 等待依赖 | | | |
| S15 | 资源包校验 | 等待依赖 | | | |
| S16 | 家长今日概览 | 等待依赖 | | | |
| S17 | 最小学习日历 | 等待依赖 | | | |
| S18 | 服务端判题 | 等待依赖 | | | |
| S19 | 不计星的自由复习 | 等待依赖 | | | |
| S20 | 默认关闭的录音上传 | 等待依赖 | | | |
| S21 | 每日备份 | 等待依赖 | | | |
| S22 | 新代次恢复 | 等待依赖 | | | |
| S23 | 一本自制短绘本 | 等待依赖 | | | |
| S24 | 一组自然拼读 | 等待依赖 | | | |
| S25 | 奖励页和连续徽章 | 等待依赖 | | | |
| S26 | 周报和月报 | 等待依赖 | | | |
| S27 | 按主题扩充，一主题一个 PR | 等待依赖 | | | |
| S28 | Android 本地库 | 等待依赖 | | | |
| S29 | 家庭局域网验收 | 等待依赖 | | | |

## 已确认的产品要求

- 服务端：Java 17、Spring Boot 3.x 模块化单体、Maven Wrapper、`groupId` `com.guoyu`、根包 `com.guoyu.english`、PostgreSQL 16+、Flyway。统一响应 `requestId / code / message / data / serverTime`。
- 客户端：uni-app、Vue 3、TypeScript、Pinia。先保证 H5。仓储接口隔离 H5 IndexedDB 与 Android SQLite。
- 部署：最小 Compose、`.env.example`、Nginx。
- 第一个学习闭环：动物课，与详设高保真原型同一主题。首页 → 听一听 → 跟我说 → 认一认 → 完成。本地先写状态和 outbox，再经 `/api/v1/sync/push` 与 `/api/v1/sync/pull` 同步。首页显示进度和星星。
- 跟读音频可替换 TTS，默认适合幼儿园大班跟读。文本和图片都是自制的。

## 实施取舍

| 编号 | 选择 | 原因 |
| --- | --- | --- |
| P01 | 实现以 V1.9 为准 | 第 24 章是唯一 API，第 23 章是唯一表模型。S01 把根协作指南里仍写 V1.7 的现状句改到 V1.9 |
| P02 | 主题「动物 / Animals」，课 `animals-001`。听一听和认一认用 cat、dog、rabbit、bird、fish；跟我说和读一句用五句短句 | 与 V1.9 原型和 6.3 节一致。原型里已有 cat、dog、rabbit；bird、fish 是 6.3 节同一主题里的两个常见词，用来凑满五词。不再使用水果课 |
| P03 | 种子一次写齐四步内容。第一张每日任务只把听、说、认列为必做 | 读一句的句子已经定死，避免 S12 再发明文案。已发行快照不追加步骤。小游戏不进这版种子 |
| P04 | 今日完成不另发签到星。每日贴纸 `CHECKIN_STICKER` 记在 `badge_award`，同一儿童同一学习日只有一张 | V1.9 第 23.4.1 节。重复完成不新增签到用的 `star_ledger` 行 |
| P05 | 听一听、认一认不发星。跟我说按句子发星，键为 `{childId}:{learningDay}:SPEAK_STAR:{sentenceId}` | 主人把跟我说定在句子上，所以这一课不用词条 ID 做跟读键。详设原文的 `SPEAK_STAR:{lexemeId}` 只保留给以后「按词跟读」的课。读一句用 `READ_STAR:{sentenceId}`，与跟读键不同，同一句可以各得 1 星，但读一句要到 S12 才发放 |
| P06 | 配对用一次性配对码，PIN 不进入 `/devices/pair` | V1.9 的要求。引导码只放环境变量 |
| P07 | 恢复码和代次切换放到 S22 | 不挡第一个闭环。试用期没有演练过的恢复 |
| P08 | 同步使用 `schemaVersion=2` 和单个 `streamEpoch`，先不做签名和重放 | 字段形状先定下来，恢复阶段再轮换代次 |
| P09 | ORM 只用 MyBatis-Plus | S01 锁定当时的 Spring Boot 3 补丁，不升到 4 |
| P10 | 集成测试用 PostgreSQL 16，不用 H2 代替 `ON CONFLICT` | S01 接上，后面复用 |
| P11 | TTS 不绑定厂商。参数写入配置和 `media_asset` | 见共享契约。运行时不调用 TTS |
| P12 | 跟我说的录音默认只在浏览器里回放 | 上传要到 S20，且默认关闭 |
| P13 | 种子只放课程和媒体，不插入儿童、PIN 或设备 | 建档走 API |
| P14 | 显示星星 = 已覆盖的服务端确认数 + 尚未被该快照覆盖的本机预估 | 两边不能直接相加 |

## 波次：哪些并行，哪些必须串行

共享契约随本文件合并后生效。它不是 S01 或其他实现阶段的副产品。实现阶段都从已合并的契约往下做。

| 波次 | 可以同时做 | 必须先合并 |
| --- | --- | --- |
| A | S01、S02、S05 | 本计划 |
| B | S04、S06；S03 也可与它们同时做 | S04、S06 等待 S01。S03 等待 S01 和 S02。S05 未合并时，S04 仍可插入音频的路径，哈希由 S05 补 |
| C | S07 与 S08 可同时做；S09 可与 B、C 里其他阶段同时做 | S07、S08 等待 S04 和 S06。S09 只等待 S02 |
| D | S10 可与 S07、S08 同时做 | S10 等待 S05 和 S09 |
| E | S11 单独做联调 | S03、S06、S07、S08、S10 |
| F | S12、S13、S14、S15、S17、S21 可按下面的依赖同时做 | 见各任务卡。S13 不改读一句页面，S12 不改跳过逻辑 |
| G | S16、S18、S19、S20、S22、S23、S24、S25、S26 | 见各任务卡 |
| H | S27 按主题拆开后可并行，但不要同时改生成工具 | S12、S15、S05 |
| I | S28、S29 | S28 等待 S10 和 S11。S29 最后做，不和功能 PR 并行改同一部署文件 |

必须串行的主链：

- 后端骨架：本计划 → S01 → S04 → S07 → S11
- 身份：本计划 → S01 → S06 → S07
- 客户端：本计划 → S02 → S09 → S10 → S11
- 音频：本计划 → S05 → S10
- 部署：S01 和 S02 → S03 → S11；S03 → S21 → S22

两个阶段若都改 `server/`，只有在契约已经分好包名和 Flyway 版本号时才并行，并且各自从同一基线拉分支。合并时不要重排别人的迁移文件。

## 共享契约

这一节是阶段之间的唯一共享契约。定稿时机就是本计划合并。S01 不负责发明第二套字段，S04 不负责改句子文本，S10 不负责改题目选项。

### 目录所有权

| 路径 | 谁先创建 | 别人不要改 |
| --- | --- | --- |
| `server/` 工程骨架、`common`、健康检查、`V1__baseline.sql` | S01 | 后续阶段只加自己的包和新迁移 |
| `client/` 工程骨架、首页壳、设计 token | S02 | S09、S10 只加自己的子目录 |
| `deploy/`、`server/Dockerfile`、`client` 的 H5 镜像构建文件 | S03 | S21 只加 `deploy/backup/` |
| `media-samples/image/animals/` | S04 | 不改音频工具 |
| `media-samples/tools/`、`media-samples/tts/`、`media-samples/audio/animals/` | S05 | 不改 SVG，不改 Java |
| `server` 的 `course`、`media` 包和 `V2` | S04 | |
| `server` 的 `auth`、`child` 包和 `V3` | S06 | |
| `server` 的 `sync`、`reward` 包和 `V4` | S07 | |
| `server` 的 `learning` 包和 `V5` | S08 | |
| `client/src/repositories/`、`client/src/sync/`、`client/src/api/` | S09 | S10 通过这些接口访问，不直接写 IndexedDB |
| `client/src/pages/learn/`、`client/src/components/`、`client/src/media/` | S10 | |
| `client/src/pages/learn/read/`、`client/src/pages/learn/game/` | S12 | 不改认一认题目 |

### Flyway 版本预分配

| 版本 | 阶段 | 内容 |
| --- | --- | --- |
| V1 | S01 | 基线，不建业务表 |
| V2 | S04 | 课程、词条、媒体、题目 |
| V3 | S06 | 家庭、儿童、设备、PIN |
| V4 | S07 | 事件、星星、贴纸、打卡、同步日志 |
| V5 | S08 | `daily_assignment` |
| V6 | S15 | `content_pack` 与文件清单 |
| V7 | S20 | `speech_record` 与墓碑所需列 |
| V8 | S23 | 绘本 |
| V9 起 | S24、S27 | 每个内容 PR 只用一个新版本，先在 PR 里声明版本号 |

`child_profile.timezone_id` 默认 `Asia/Shanghai`，`issued_through` 在 V3 建列，S14 才写入七天逻辑。`daily_assignment` 在 V5 含 `snapshot_json`、`snapshot_hash`、`required_step_ids`。

### 统一响应

所有 `/api/v1` JSON 都是：

```json
{
  "requestId": "由请求头 X-Request-Id 传入，否则服务端生成 UUID",
  "code": "OK",
  "message": "OK",
  "data": {},
  "serverTime": "UTC ISO-8601"
}
```

S01 就建好业务码枚举，后续阶段只引用，不改枚举名：`OK`、`INVALID_ARGUMENT`、`DEVICE_REVOKED`、`EVENT_CONFLICT`、`EVENT_ID_REUSED`、`INTERNAL_ERROR`。其余详设错误码在用到的阶段再追加，追加前先改本节。

健康检查 `GET /api/v1/system/health` 无需令牌。`data` 只有 `status=UP` 和构建版本，没有口令、地址、令牌。

### 动物课内容

主题 `theme-animals`，中文名「动物」，英文名 `Animals`。课 `animals-001`，`version=1`。这与详设 Push 示例里的课程 ID 相同，但版本用第一版，不沿用示例里的 `lessonVersion=3`。文本是自制的。TTS 只朗读英文，不朗读中文释义。

单词（听一听、认一认）：

| lexemeId | 英文 | 中文 | 图片 | 单词音频 | 依据 |
| --- | --- | --- | --- | --- | --- |
| `lexeme-cat` | cat | 猫 | `image/animals/cat.svg` | `audio/animals/cat-v1.mp3` | 原型 |
| `lexeme-dog` | dog | 狗 | `image/animals/dog.svg` | `audio/animals/dog-v1.mp3` | 原型 |
| `lexeme-rabbit` | rabbit | 兔子 | `image/animals/rabbit.svg` | `audio/animals/rabbit-v1.mp3` | 原型 |
| `lexeme-bird` | bird | 鸟 | `image/animals/bird.svg` | `audio/animals/bird-v1.mp3` | 详设 6.3，补充 |
| `lexeme-fish` | fish | 鱼 | `image/animals/fish.svg` | `audio/animals/fish-v1.mp3` | 详设 6.3，补充 |

`lexeme-cat` 与详设 Push 示例里的 `targetId` 一致。图片是简单自制 SVG 占位插图，来源 `HOMEMADE`。不要引入第三方受版权保护的图片，也不要下载图库动物图。

句子（跟我说、读一句）。前三句来自原型或 6.3 节，后两句保持同样的短句长度：

| sentenceId | 英文 | 中文 | 音频 | 依据 |
| --- | --- | --- | --- | --- |
| `sent-i-see-a-cat` | I see a cat. | 我看见一只猫。 | `audio/animals/i-see-a-cat-v1.mp3` | 原型、6.3 |
| `sent-i-see-a-dog` | I see a dog. | 我看见一只狗。 | `audio/animals/i-see-a-dog-v1.mp3` | 原型 |
| `sent-i-like-rabbits` | I like rabbits. | 我喜欢兔子。 | `audio/animals/i-like-rabbits-v1.mp3` | 6.3 |
| `sent-its-a-dog` | It's a dog. | 这是一只狗。 | `audio/animals/its-a-dog-v1.mp3` | 短句补充 |
| `sent-whats-this` | What's this? | 这是什么？ | `audio/animals/whats-this-v1.mp3` | 短句补充 |

步骤 ID：`step-listen`、`step-speak`、`step-recognize`、`step-read`。第一张任务的 `requiredStepIds` 只有前三个。

认一认每题听该词音频，选项只有图片，2 到 3 张，不要在选项上写英文或中文。正确项与干扰项如下，干扰项都来自这五个动物：

| questionId | 正确 lexemeId | 选项顺序 |
| --- | --- | --- |
| `q-cat` | `lexeme-cat` | cat、dog |
| `q-dog` | `lexeme-dog` | dog、rabbit、bird |
| `q-rabbit` | `lexeme-rabbit` | rabbit、cat |
| `q-bird` | `lexeme-bird` | bird、fish、dog |
| `q-fish` | `lexeme-fish` | fish、rabbit |

选项 ID 形如 `opt-cat`，与 lexeme 一一对应，全课复用，不按题另起一套图片。

`GET /api/v1/lessons/animals-001?version=1` 的 `data` 使用上述 ID，并带上 `steps[]`。听一步列出五个 `lexemeId`。说一步和读一步列出五个 `sentenceId`。认一步列出上表题目。未知版本返回 `INVALID_ARGUMENT`。

### 媒体元数据

`media_asset.storage_key` 使用上表相对路径，不存绝对路径。来源枚举：`HOMEMADE`、`LICENSED_THIRD_PARTY`、`AI_GENERATED`、`OFFLINE_TTS`、`PLACEHOLDER`。另有生成工具、音色、`speech_rate`、三项停顿毫秒、`source_text`、`generated_at`、MIME、字节数、SHA-256。

TTS 配置文件固定为 `media-samples/tts/tts.config.yaml`：

| 参数 | 默认 |
| --- | --- |
| `speechRate` | `0.80`（建议区间 0.75–0.85） |
| `voice` | 空，由提供方选清晰、不尖的音色 |
| `style` | `clear-warm` |
| `pauseBetweenWordsMs` | `600` |
| `pauseBetweenSentencesMs` | `900` |
| `pauseAfterPromptMs` | `400` |

环境变量：`TTS_PROVIDER`、`TTS_API_BASE`、`TTS_API_KEY`、`TTS_VOICE`。无密钥时用离线 TTS，再不行用 `PLACEHOLDER`。元数据同时保存 0.80 倍速和引擎自己的原始参数。每个文件只含一个单词或一个句子。

清单分成两份，避免并行改同一个文件：`media-samples/image/animals/MANIFEST.md` 由 S04 写，`media-samples/audio/animals/MANIFEST.md` 由 S05 写。

### 身份与同步

- `POST /api/v1/parent/bootstrap`：环境变量中的一次性引导码 + 新 PIN + `Idempotency-Key`。
- `POST /api/v1/parent/children`：昵称、年龄段、每日分钟数、时区。相同幂等键不重复建档。
- `POST /api/v1/parent/pair-codes`：家长会话换 5 分钟配对码。
- `POST /api/v1/devices/pair`：配对码、`deviceId`、平台，换设备令牌。请求体没有 PIN。
- `POST /api/v1/sync/push`：`schemaVersion=2`、`streamEpoch`、最多 100 条且不超过 256 KiB。每条结果为 `ACCEPTED`、`DUPLICATE` 或 `REJECTED`。
- `GET /api/v1/sync/pull?childId=&streamEpoch=&cursor=&limit=`：返回 `changes`、`nextCursor`、`hasMore`。
- `GET /api/v1/home/today?childId=`：返回 `taskId`、`lessonId=animals-001`、`lessonVersion=1`、`requiredStepIds`、三步进度、`confirmedStars`、`stickerGranted`。

跟我说完成事件：`STEP_FINISHED`，`stepId=step-speak`，`targetId` 为 `sentenceId`。同一 `eventId` 再推返回 `DUPLICATE`，星星行数不变。同一句子用新 `eventId` 再完成，仍然只有一颗 `SPEAK_STAR`。听一听和认一认的完成事件不写 `star_ledger`。当天三个必做步骤都完成后写一张 `CHECKIN_STICKER`，再完成一次不增加第二张，也不写签到星。同一 `eventId` 换载荷返回 `EVENT_ID_REUSED`。

学习日按 `Asia/Shanghai`。客户端传来的星星数不入账。

### 客户端接口

页面通过仓储，不直接打开 IndexedDB 或发 HTTP。S02 建出这些方法签名，S09 实现，方法在未实现时明确失败，不要假装保存成功：

- `saveLearningAction(action)`：同一事务写进度、outbox、预估星星
- `listOutbox(limit)`
- `ackOutbox(eventId, result)`
- `savePullPage(changes, nextCursor)`：变更写成功后才推进游标
- `getLesson(lessonId, version)`

路由：`pages/index/index`、`pages/learn/listen`、`pages/learn/speak`、`pages/learn/recognize`、`pages/learn/complete`。读一句用 `pages/learn/read`，到 S12 才做。

## 阶段总览

| 阶段 | 一句话 | 波次 |
| --- | --- | --- |
| S01 | 后端空壳、健康检查、Flyway 基线 | A |
| S02 | 客户端空壳，H5 能构建 | A |
| S03 | Compose 拉起数据库、服务和网页入口 | B |
| S04 | 动物课表、五个自制 SVG、课程种子 | B |
| S05 | 可替换慢速 TTS 和十段示例音频 | A |
| S06 | 空库引导、建档、配对码 | B |
| S07 | 幂等同步、句子跟读星、每日贴纸 | C |
| S08 | 当天三步任务和首页接口 | C |
| S09 | 本地状态与 outbox | C |
| S10 | 听、说、认、完成页 | D |
| S11 | 端云联调 | E |
| S12 | 读一句页面和一局课程小游戏 | F |
| S13 | 麦克风失败时跳过 | F |
| S14 | 冻结未来七天任务 | F |
| S15 | 资源包校验 | F |
| S16 | 家长今日概览 | G |
| S17 | 最小学习日历 | F |
| S18 | 服务端判题 | G |
| S19 | 不计星的自由复习 | G |
| S20 | 默认关闭的录音上传 | G |
| S21 | 每日备份 | F |
| S22 | 新代次恢复 | G |
| S23 | 一本自制短绘本 | G |
| S24 | 一组自然拼读 | G |
| S25 | 奖励页和连续徽章 | G |
| S26 | 周报和月报 | G |
| S27 | 按主题扩充，一主题一个 PR | H |
| S28 | Android 本地库 | I |
| S29 | 家庭局域网验收 | I |

每个阶段都更新自己改到的 README，删掉已经不真实的「空目录、现在跑不了」。不要提交 `.env`、真实 PIN、设备令牌、儿童档案、录音或家庭地址。

## S01 后端脚手架与健康检查

- 背景：仓库还没有 Java 工程。后面的表和接口都要接到这个进程上。
- 输入：已合并的共享契约。无代码基线。
- 产出：`server/` 的 Maven Wrapper、POM、`common`、健康检查、`V1__baseline.sql`、`server/README.md`、`server/AGENTS.md`，以及根 `AGENTS.md` 里过时的 V1.7 现状句。
- 接口契约：落实统一响应和 `GET /api/v1/system/health`。建好契约中的业务码枚举。引入 MyBatis-Plus，但不建业务表。Spring Boot 3 的补丁版本写进 POM。
- 验收标准：`server/` 下 `./mvnw test` 通过。Testcontainers `postgres:16` 上 Flyway 应用到 V1。健康检查 HTTP 200，五个响应字段齐全，`serverTime` 为 UTC ISO-8601，`data` 没有口令、地址和令牌。
- 不要碰：`client/`、`deploy/`、`media-samples/`、课程种子、认证、同步。不要把 Dockerfile 放进本阶段。
- 风险：没有 Docker 时测试失败。不要改用 H2。不要改 Java 级别。

## S02 客户端脚手架

- 背景：还没有 uni-app 工程。页面和仓储后面要加进这套工程。
- 输入：已合并的共享契约。可与 S01 并行。
- 产出：`client/` 工程、锁文件、首页壳、空的仓储接口、`client/README.md`、`client/AGENTS.md`。
- 接口契约：按契约创建仓储方法签名。应用 ID 预留 `com.guoyu.english`。首页一屏一件事，主按钮按 56–64dp，进入页面不放音频。`engines` 写成脚手架实际要求的 Node 版本。
- 验收标准：安装依赖、类型检查、`build:h5` 成功。产物里能看到首页壳。未实现的仓储方法失败时不会显示已保存。
- 不要碰：`server/`、`media-samples/`、学习页、IndexedDB 表、真实 HTTP。
- 风险：CLI 与 Node 版本不匹配时，以官方脚手架为准，不从别的 Vite 项目抄版本。

## S03 本地 Compose

- 背景：需要一条命令同时看到数据库、后端和 H5 入口。
- 输入：S01 和 S02 已合并。
- 产出：`deploy/docker-compose.yml`、`deploy/.env.example`、Nginx 配置、`server/Dockerfile`、H5 镜像构建文件、`docs/development.md`、`deploy/README.md`。
- 接口契约：网页入口的 `/api/` 原样转到后端 `/api/`，不重复也不截掉 `v1`。数据库不映射 5432。媒体目录只读。`.env.example` 只用 `CHANGE_ME` 一类占位符。8080 只用于本机 HTTP，文档写明录音仍要可信 HTTPS。
- 验收标准：Compose 配置检查不打印真实密钥。拉起后 `GET http://127.0.0.1:8080/api/v1/system/health` 与 S01 的成功结构一致。宿主机没有 5432。把真实命令写进开发指南。
- 不要碰：业务 Java 包、课程数据、客户端页面逻辑。镜像起不来时只修构建文件。
- 风险：web 镜像若没拷进 S02 产物，默认页不算通过。

## S04 动物课数据与自制图

- 背景：听、说、认、读都要引用同一套词、句、图和题目，不能由页面临时编写。
- 输入：S01 已合并。共享契约中的动物课表。S05 若已合并，读取它的音频哈希；未合并则音频行先记下路径和 `PLACEHOLDER`，留待 S05 的迁移更新哈希。
- 产出：Flyway `V2`，`course` 与 `media` 包，五个 SVG，`media-samples/image/animals/MANIFEST.md`，课程查询接口。
- 接口契约：ID、中英文、路径、认一认选项和 `GET /api/v1/lessons/animals-001?version=1` 与契约一致。SVG 为自制简单图形。种子不含儿童。
- 验收标准：`./mvnw test` 覆盖迁移和课程查询。五个词、五个句子、五道题的选项数量为 2 或 3。图片哈希与清单一致。响应仍是统一外壳。
- 不要碰：`media-samples/audio/`、`media-samples/tools/`、认证、同步、客户端。不要改句子或选项来「优化课文」。
- 风险：H5 若不能显示 SVG，留到 S10 把同一图形转成自制栅格图，并同时改哈希和契约，不在本阶段换图库。

## S05 慢速跟读音频

- 背景：五个单词和五个句子需要可播放的慢速英文音频，厂商以后还能换。
- 输入：已合并的共享契约。可与 S01、S02 并行。
- 产出：`media-samples/tools/` 生成工具、`media-samples/tts/tts.config.yaml`、十个音频、`media-samples/audio/animals/MANIFEST.md`、`media-samples/README.md` 里的提供方说明。若 S04 已合并，再用一个新的预分配迁移补上真实哈希；版本号不要占用 V2–V5，使用当时尚未使用的下一个号，并在 PR 里声明。
- 接口契约：默认语速 0.80，词间 600 毫秒，句间 900 毫秒，`style=clear-warm`。`source_text` 只存英文。来源按实际引擎写成 `AI_GENERATED`、`OFFLINE_TTS` 或 `PLACEHOLDER`。工具提供离线引擎、占位音频和只读环境变量的 HTTP 适配，不提交某一家 SDK 作为唯一实现。
- 验收标准：不设置密钥能生成十个文件。文件头或探测工具确认是音频而不是空文件。元数据含语速、停顿、原文本、工具和生成时间。改环境变量后元数据跟着变。README 说明为什么这组默认值适合 5–6 岁跟读。
- 不要碰：SVG、Java 业务、客户端页面、句子文本。
- 风险：离线音质不像人声。占位音必须在清单里标明，不能写成正式教材音。

## S06 引导、建档与配对

- 背景：空库要能创建家庭和儿童，设备令牌不能靠 PIN 直接换。
- 输入：S01 已合并。可与 S04 并行。
- 产出：Flyway `V3`，`auth` 与 `child` 包，引导、建档、配对码、配对和撤销。
- 接口契约：按共享契约的四个身份接口。PIN 4–6 位，Argon2id。连续失败 5 次后的延迟存在数据库里。日志不打印 PIN、引导码和令牌。
- 验收标准：从空库走完引导、建档、配对。重复建档返回原儿童。撤销后的令牌不能访问受保护接口。测试输出里没有可用 PIN。
- 不要碰：课程表、同步表、客户端。不要增加「PIN 换令牌」的第二入口。
- 风险：Argon2 内存参数在小容器里失败时，在 PR 里记录实测值，不要改成弱哈希。

## S07 同步与幂等账本

- 背景：重复上报不能重复发星，同一天完成不能重复发贴纸。
- 输入：S04 和 S06 已合并。可与 S08 并行。
- 产出：Flyway `V4`，`sync` 与 `reward` 包，Push 和 Pull。
- 接口契约：按共享契约的事件、`SPEAK_STAR` 句子键、贴纸和 `DUPLICATE` / `EVENT_ID_REUSED`。一个事务里写事件、进度、星星、贴纸和 `sync_change_log`。家庭一个 `streamEpoch`。学习日用上海时区。
- 验收标准：同一 `eventId` 推两次，第二次为 `DUPLICATE`，星星行数不变。同一句子新事件再完成仍是一颗星。同一天再次完成三个必做步骤，贴纸仍为一张，且没有签到星。哈希冲突不加星。事务失败时事件、星星和日志一起回滚。并发插入同一 `eventId` 只有一行。听完和认对不产生星星行。
- 不要碰：`daily_assignment`、首页控制器、客户端、音频文件。
- 风险：不要用「先查后插」代替唯一约束。Pull 只前进到已提交行。

## S08 今日任务

- 背景：客户端不能自己决定今天学什么。
- 输入：S04 和 S06 已合并。可与 S07 并行。
- 产出：Flyway `V5`，`learning` 包，`GET /api/v1/home/today`。
- 接口契约：同一天重复领取得到同一 `taskId` 和哈希。`requiredStepIds` 只有听、说、认。快照冻结 `animals-001` 版本 1 和契约里的题目。设备时区不改变学习日。进度字段还没有时返回 0，不自己计算星星。
- 验收标准：重复领取不改哈希。改请求里的设备时区后，学习日仍是上海日期。未授权儿童读不到任务。
- 不要碰：`star_ledger` 的发放、Push 控制器、页面。
- 风险：不要把 `step-read` 放进第一张快照的必做列表。

## S09 本地仓储与 outbox

- 背景：学习动作必须先落在浏览器里，失败时不能显示已保存。
- 输入：S02 已合并。可与 S04、S06、S07、S08 并行。
- 产出：`client/src/repositories/`、`client/src/sync/`、`client/src/api/`。
- 接口契约：实现 S02 已声明的方法。H5 使用 IndexedDB。`eventId` 创建后不变。Android 实现同一接口，可以用最小占位，但必须标明没有真机验证。类型按契约手写，不用 `any`。
- 验收标准：自动化测试覆盖事务失败不留半条 outbox、重试不换 `eventId`、Pull 写到一半不推进游标。H5 构建通过。
- 不要碰：学习页视觉、服务端、`media-samples/`。
- 风险：不要把 Pinia 内存当成持久化通过的证据。

## S10 听一听、跟我说、认一认

- 背景：孩子要在不连服务器时学完这节课的三步。
- 输入：S05 和 S09 已合并。可与 S07、S08 并行。
- 产出：`pages/learn/listen`、`speak`、`recognize`、`complete`，以及共用的儿童组件、播放和录音适配。
- 接口契约：听五个词，点按后播放，不自动大声播放。跟我说五个句子，显式录音，显示状态，单次最长 30 秒，可回放，离页释放设备。认一认使用契约里的五题，每题 2–3 张图，答错只显示「再听一次」或「再试试」。动作只走仓储。非安全上下文要提示需要 HTTPS。
- 验收标准：H5 构建通过。浏览器里走完三步和完成页，刷新后进度还在。五道题的干扰项与契约一致。能播放 S05 的音频。PR 附截图。录音权限被拒绝时的提示要单独看到。Android 写明未做真机。
- 不要碰：服务端发星、配对页面、SVG 的版权来源、读一句页面。
- 风险：没有麦克风时不能把截图写成录音已通过。

## S11 第一个闭环联调

- 背景：本地学完后，首页要显示服务端进度和星星，重复上报不加星。
- 输入：S03、S06、S07、S08、S10 已合并。本阶段不与这些阶段并行。
- 产出：同步接线、首页数据绑定、`docs/api/api-v1.md` 里已经实现的接口。
- 接口契约：先 Push 再 Pull。逐条处理三种 ACK。星星按 P14 合并。儿童页不显示服务器 IP。配对使用配对码。
- 验收标准：本地或 Compose 后端加 H5，走完配对、五句跟读和同步。该句只有一颗跟读星，当天只有一张贴纸。同一 outbox 再推一次，星星数量不变。首页能看到进度和星星。PR 写命令、结果和截图。
- 不要碰：七天发行、日历、录音上传、题目文案。
- 风险：HTTP 页面可能拒绝录音。结果里分开写页面联调和录音限制。

## S12 读一句与课程小游戏

- 背景：五个句子已经在种子里，读一句页面还没有。课程小游戏也还没有。
- 输入：S11 已合并。可与 S13、S14、S15、S17、S21 并行，但不改这些阶段的文件。
- 产出：`pages/learn/read`、`pages/learn/game`，以及新任务快照的必做步骤。
- 接口契约：读一句使用已有五个 `sentenceId`，键为 `READ_STAR`。小游戏 `context=COURSE`，只用这五个动物，键为 `{childId}:{learningDay}:COURSE_GAME_STAR:animals-001`，每天 1 星。新任务才要求读和玩。旧的三步快照不改。
- 验收标准：新任务五步才发贴纸。同一句的读星不重复。旧三步任务仍按三步完成。
- 不要碰：认一认选项、TTS 默认参数、已发行快照的哈希。
- 风险：共用完成判断会把旧课重新变成未完成。测试要覆盖两个快照版本。

## S13 麦克风跳过

- 背景：没有麦克风时，孩子仍应能结束跟我说，但不能因此得到该句的星。
- 输入：S10 已合并。可与 S12 并行。
- 产出：跟我说页面上的跳过分支，以及 `STEP_SKIPPED` 的服务端处理。若 S07 已合并，只在 `sync` 包内加这一类事件。
- 接口契约：跳过计入必做完成，不发该句 `SPEAK_STAR`。之后补录只发一次。跳过按钮只在录音不可用时出现。
- 验收标准：重复跳过不加星。补做成功后该句只有一颗星。完成状态成立。
- 不要碰：读一句页面、题目、配对。
- 风险：不要把跳过画成常驻的第二主按钮。

## S14 七天任务

- 背景：今天以后的六天也要事先冻住，避免两台设备各编一套计划。
- 输入：S08 已合并。
- 产出：发行七天的逻辑。优先不新增迁移；列已经在 V3、V5 里。
- 接口契约：`child_id + learning_day` 唯一。重复领取不改哈希。不改写已发行行。时区修改按钮在生效规则没写完前不要出现。
- 验收标准：连续领取七天，行数不增加、哈希不变。更新已发行快照被拒绝。
- 不要碰：Push 的发星公式、客户端学习页。
- 风险：只做一半的时区切换会让两端算出不同的生效日。

## S15 资源包

- 背景：图和音频要能校验后切换，失败时旧文件还在。
- 输入：S05 和 S11 已合并。
- 产出：Flyway `V6`，`content` 包，客户端临时下载和切换。
- 接口契约：清单含相对路径、大小、SHA-256、MIME。全部通过才切换。
- 验收标准：改坏一个字节后不切换。中断后原包仍能播放。哈希由工具计算。
- 不要碰：奖励规则、绘本。
- 风险：浏览器配额很小。记录实际占用，不要宣称已经完成七天预下载。

## S16 家长今日概览

- 背景：家长需要看到今天是否完成、星星是否已确认、有没有跳过录音。
- 输入：S06、S11、S13 已合并。
- 产出：`client/src/pages/parent/` 和 `GET /api/v1/parent/dashboard`。
- 接口契约：长按只发现入口，进入仍要 PIN。离线不新建家长会话。撤销复用 S06。
- 验收标准：错误 PIN 进不去。概览与服务端一致。撤销后 Push 被拒绝。
- 不要碰：周月报、儿童页上的管理按钮。
- 风险：PIN 不要出现在日志或查询字符串里。

## S17 最小学习日历

- 背景：完成日要按儿童时区显示，同一天只算一次。
- 输入：S07 已合并。可与 S12 并行。
- 产出：`GET /api/v1/learning/calendar` 和日历页。
- 接口契约：数据来自 `daily_checkin`，没有单独签到按钮。整月一次返回。
- 验收标准：同一天两批事件只有一个完成日。上海时区 23:30 和次日 00:30 落在不同学习日。
- 不要碰：连续徽章展示、时长相加。
- 风险：不要用服务器本地日期切日。

## S18 服务端判题

- 背景：认一认的对错现在还停在客户端，熟悉度需要服务端按第一次作答计算。
- 输入：S07、S10、S14 已合并。
- 产出：`QUESTION_ANSWERED` 的处理和题目判分。
- 接口契约：使用契约里的 `questionId` 和 `opt-*`。最终答对可以完成步骤，重试不加熟悉度。同一尝试序号换答案返回冲突。只影响未发行任务。
- 验收标准：先错后对，完成但熟悉度不加。客户端声称答对、选项错误时，服务端记错。
- 不要碰：已发行的七天快照、跟读键。
- 风险：不要接收屏幕坐标代替选项 ID。

## S19 自由复习

- 背景：计划用完后，已缓存的动物课还应能再玩，但不发奖励。
- 输入：S12 和 S15 已合并。
- 产出：一个最小复习入口。
- 接口契约：`context=FREE`。不发星，不写打卡。
- 验收标准：复习前后星星和 `daily_checkin` 不变。
- 不要碰：课程模式的必做步骤。
- 风险：不要把 FREE 事件写成 COURSE。

## S20 录音上传

- 背景：家长以后可以选择把录音放到 NAS，默认仍不上传。
- 输入：S06 和 S11 已合并。
- 产出：Flyway `V7`，`speech` 包，独立 recordings 卷。
- 接口契约：默认关闭。同一标识和同一哈希不存两份。删除后再次上传失败。Nginx 不公开该目录。学习完成不依赖文件。
- 验收标准：默认拒绝上传，本地完成仍有效。删除后不能重新播放同一标识。
- 不要碰：第三方评分、Git 里的真实录音。
- 风险：先写墓碑再删文件。跨进程锁没做完就不要宣称删除竞态已通过。

## S21 每日备份

- 背景：闭环试用前需要一条可审查的备份命令。
- 输入：S03 已合并。可与功能阶段并行，但只改备份目录。
- 产出：`deploy/backup/` 和部署说明。
- 接口契约：转储数据库，并记录媒体相对路径、大小、SHA-256。备份目录与数据目录分开。说明保留设想是 30 天。
- 验收标准：对本地 Compose 跑通一次。PR 写明恢复未验证。
- 不要碰：业务代码、真实 NAS。
- 风险：输出里不要带 `.env` 的值。

## S22 恢复代次

- 背景：旧备份恢复后，旧游标和旧令牌不能继续使用。
- 输入：S07 和 S21 已合并。
- 产出：新 `streamEpoch`、凭证代次和受控重放。做不完时拆成两个 PR：先失效令牌，再重放。
- 接口契约：重放沿用原 `eventId` 和 `grantKey`，不第二次发星。超过普通窗口且从未确认的事件不补进账本。
- 验收标准：在拷贝出的测试库上，恢复后仍能收到备份点之后的新变更。旧令牌被拒绝。
- 不要碰：同步主路径的正常 Push 语义、生产库。
- 风险：没有第二份环境时，不要写成 NAS 已通过。

## S23 短绘本

- 背景：闭环之后才做阅读。完成条件必须是必读句读完。
- 输入：S15 和 S07 已合并。
- 产出：Flyway `V8`，一本 6 到 8 页自制绘本和点读页。
- 接口契约：自然播放结束才记已读。同一本书首次读完计 1。音频沿用 S05 的元数据格式。
- 验收标准：跳到末页不算读完。再读一遍总本数仍是 1。
- 不要碰：动物课必做步骤、商业绘本扫描件。
- 风险：改句子就要新版本，不要覆盖旧音频路径。

## S24 自然拼读

- 背景：需要一组不引入生词的字母音练习。
- 输入：S15 已合并。
- 产出：一个新 Flyway 版本、六个与动物课有关的字母音、一个练习页。优先包含详设 6.3 的 C /k/ 和 D /d/。
- 接口契约：不把拼读加入 `animals-001` 的必做步骤。音频来源写入清单。
- 验收标准：可播放、可作答。没有事件就没有星星。
- 不要碰：动物课快照。
- 风险：离线字母音可能不清楚，清单里标明是否已经听审。

## S25 奖励页与徽章

- 背景：儿童要看到确认星星和预估星星，连续学习徽章各得一次。
- 输入：S07 和 S17 已合并。
- 产出：奖励页，以及 3、7、14、30 天徽章。
- 接口契约：是否获得不随机。断学习不收回已发徽章。学习日复用 S17。
- 验收标准：同一里程碑只有一枚。Pull 覆盖后星星不翻倍。
- 不要碰：商店、扣星动画。
- 风险：不要用设备本地日历计算连续天数。

## S26 周报与月报

- 背景：家长需要按周、按月看已有投影，而不是另一套账。
- 输入：S16 和 S17 已合并。
- 产出：周报和月报接口与页面。
- 接口契约：周从周一开始。绘本本数在 S23 之前显示为还没有数据。时长不把两台设备相加。
- 验收标准：跳过次数与 S13 的事件一致。
- 不要碰：新的统计表。
- 风险：不要在报告里重算一套与日历不同的完成日。

## S27 扩充主题

- 背景：动物课已经是第一个闭环。机制稳定后，再按附录 B 增加 Colors 和 Numbers。
- 输入：S12、S15、S05 已合并。每个主题单独一个 PR，可并行，但都不要改生成工具的默认参数。
- 产出：该主题的迁移、自制图、音频和清单。
- 接口契约：课结构、TTS 配置和清单格式与动物课相同。不改奖励公式。
- 验收标准：新课可查询。动物课仍能学完。
- 不要碰：`animals-001` 的 ID、词条和题目。不要一次提交多个主题，也不要另做一节重复的 Animals。
- 风险：改共用组件时要再走一遍动物课最短路径。

## S28 Android 本地实现

- 背景：H5 仓储还不能代表平板已经可用。
- 输入：S10 和 S11 已合并。
- 产出：SQLite 仓储、原生录音接口、Android 8.0 / API 26 的打包配置。
- 接口契约：方法签名与 H5 相同。没有设备时只报告工程和单元测试。
- 验收标准：按实际脚本打包。本阶段仍要通过一次 H5 构建。
- 不要碰：已通过的 H5 交互规则。
- 风险：条件编译把 H5 构建弄坏就不算完成。

## S29 家庭环境验收

- 背景：本机 Compose 不能代替家庭 NAS 和可信 HTTPS。
- 输入：S11、S15、S21 已合并。恢复结论还要 S22，录音上传结论还要 S20。
- 产出：一份日期开头的验收记录。
- 接口契约：不改协议。只记录健康检查、数据库未暴露、媒体只读、HTTPS 录音、NAS 不可达时学完、恢复后星星不增加。只断公网不算离线通过。
- 验收标准：记录含环境、命令和失败项。没有 NAS 时本阶段不开始。
- 不要碰：路由器、公网端口、生产库上的恢复实验。
- 风险：不要把 S11 的本机结果抄成家庭验收。

## 整个计划都不做的事

- 不拆微服务，不引入 Nacos、Seata、Redis、消息队列。
- 不做开放式 AI 对话、发音评分，也不把儿童录音发到外站。TTS 只给维护者生成教材音频。
- 不做商城、支付、排行榜和公开账号。
- 不把商业 App、教材页或其他仓库的图、音频、课文复制进 `media-samples/`。
- 不把 H5 桌面结果写成平板、APK 或 NAS 已验收。
