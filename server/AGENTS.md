# server/ 后端协作指南

本目录继承 [根指南](../AGENTS.md)，尤其是 Git 限制与 Java 独立类规范；先阅读 [服务端说明](README.md)。

## 现状与架构

- 指南建立时只有 README，没有 `pom.xml`、Maven Wrapper、Java 源码或迁移脚本。不要宣称已能启动或测试。
- 设计基线为 Java 17、Spring Boot 3.x、PostgreSQL 16+、Flyway，模块化单体；根包 `com.guoyu.english`，Maven groupId 规划为 `com.guoyu`。
- ORM 尚未落地，当前倾向 MyBatis-Plus，也可按已确认方案选择 Spring Data JPA；初始化时明确一种主方案，避免混用。
- 按业务能力组织 `auth / child / course / learning / book / phonics / game / reward / calendar / report / content / sync / media / speech / common`。这是逻辑包规划，不是要求创建 15 个 Maven 子工程。
- 每个模块按实际复杂度划分 `controller / application / domain / infrastructure`；不为凑层数新增空类、无意义接口或重复转发。

## Java 与职责边界

- 遵循根指南中完整的 Java 类规则：DTO、VO、BO、Query、Command、Context、Result 与业务 Enum 均独立成文件，不用内部类或嵌套 record 压缩文件数。
- Controller 负责协议、入参校验及鉴权入口；Application 编排用例和事务；Domain 表达业务规则；Infrastructure 处理数据库、文件等实现。
- 对外 DTO 与持久化对象区分，避免直接暴露数据库实体；使用明确字段，不用无约束 Map 代替稳定契约。
- 使用当前 Spring Boot 3 依赖支持的 `jakarta.*` API，不能照搬旧工程的 `javax.*` 配置；依赖版本通过 Maven 实际核对。
- 枚举按归属放在业务模块；真正跨模块共享的类型再进入 `common`，避免把所有领域类型集中成公共大包。
- 异常与日志提供可定位的业务上下文及 requestId，保留原因链；不吞异常、不用“全部成功”掩盖部分失败，不输出 PIN、令牌或录音内容。

## 接口与鉴权

- 规划 API 前缀 `/api/v1`，JSON UTF-8，统一响应字段 `requestId / code / message / data / serverTime`；落地时同步确定时间格式、空值、HTTP 状态与业务码。
- 学习动作涉及在线接口和 `/sync/push` 时，复用同一业务处理与幂等边界；不得为不同入口建立互相独立的奖励路径。
- 客户端传来的 `childId`、deviceId 等不能作为授权依据；服务端验证设备状态、绑定关系和数据归属，读取与写入都检查越权。
- 设备令牌支持撤销；家长 PIN 需要安全哈希、尝试次数限制与延迟等措施，不能因“已哈希”就声称短 PIN 是强密码。
- Push 返回每个事件的处理结果，区分 `ACCEPTED / DUPLICATE / REJECTED`。明确单条事务与批次部分成功语义，并与客户端重试处理一致。
- 接口目录在现有设计中存在分歧，实施具体功能前更新统一契约，不机械照抄多个章节的清单。

## 幂等、事务与同步

- 事件写入、进度/日历投影、奖励账本及 `sync_change_log` 必须在同一事件事务内保持一致。异常时整体回滚，不能留下已消费事件却缺少奖励或 Pull 变更的状态。
- `learning_event.event_id` 按设计使用 UUID 全局去重；依靠数据库唯一约束和插入结果处理并发，禁止只用“先查再插”保证幂等。
- 对重复事件核验归属与有效身份后返回幂等结果，不重复投影或发奖。相同 ID 携带冲突内容的处理也要定义，不能仅凭主键已存在就认可任意负载。
- 奖励需要独立的业务唯一键，`eventId` 去重不能替代跨设备的奖励去重。签到与按词条/步骤奖励粒度尚有评审分歧；先明确规则，再定义 `grant_key` 和唯一索引。
- 打卡从有效学习事件与业务条件派生，不增加“点击即签到”的旁路。录音跳过是否完成、是否发奖，必须由统一规则决定。
- 学习日按已统一的儿童档案时区规则计算，不能直接取服务器系统日期；客户端时间作输入校验，异常时间偏差与跨日边界要有明确策略。
- Pull 游标只能越过可见且不会遗漏的已提交变更；并发提交、分页、中断重试与同游标重放必须安全，不直接假定自增 ID 的分配顺序就是事务提交顺序。
- 不在数据库事务中执行长时间媒体传输或不必要的外部调用；现有事务交由明确的应用边界控制，不自行另取连接提交。

## 数据库、配置与媒体

- 正式迁移目标目录为 `src/main/resources/db/migration`，实际版本以工程配置为准。设计参考 SQL 不能未经核验直接放入自动执行目录。
- Flyway 迁移发布后不直接改旧脚本，使用新增版本修正；涉及破坏性变更需明确备份、数据兼容和恢复方案，不擅自重建库。
- 常规业务主键按设计使用 BIGINT / Identity；事件 UUID 等例外按对应契约，不能把 README 的泛化主键描述套用到全部表。
- 媒体元数据存相对路径或 storage key；统一校验路径、MIME 与大小，禁止客户端控制服务端绝对文件路径。
- 播放素材只读目录与录音上传写入目录应区分。录音上传端点、存储与清理策略未完整定义前，不擅自把全媒体目录改为可写。
- 数据源和 profile 通过外部配置注入；与 `deploy/` 对齐 `DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD / MEDIA_ROOT / SPRING_PROFILES_ACTIVE`，不写死 NAS 地址或真实口令。
- 数据库使用受限账号，不暴露给客户端或公网；修改查询时评估必要索引、分页与跨儿童数据隔离。

## 验证与交付

- 有 `pom.xml` 后核对 `java -version`、Maven 和实际编译目标；有 Wrapper 时优先使用 Windows 的 `mvnw.cmd` 或对应平台脚本，否则检查已安装 Maven。
- 选择实际工程支持的编译、测试与打包命令；没有构建文件就只检查文档与方案，不报告“测试通过”。
- 同步与奖励变更至少验证重复事件、并发重复、跨设备奖励去重、事务异常回滚、部分失败、Pull 分页/重试/并发提交、越权和设备撤销。
- 涉及 PostgreSQL 语法、约束、事务或迁移时，用匹配版本的 PostgreSQL 验证；内存替身测试不能代替数据库行为验收。
- 报告区分单元测试、数据库集成、前后端联调与 NAS 运行结果；未具备环境的部分明确列出。默认不提交 Git。
