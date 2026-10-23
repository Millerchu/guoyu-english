# docs/ — 设计文档与工程规范

本目录存放产品设计、数据库设计、接口说明、开发规范、第三方许可证登记和测试用例。

## 现有文件

2026-10-23 补充：已依据 V1.8 评审和本次用户确认生成 **V1.9 详细设计**。D01–D12 业务选择已确认；设计细节、建议参数及验收场景仍须实施验证，不表示应用已发布。原 V1.7/V1.8 保留归档。

- [V1.9 详细设计](设计文档/guoyu-english-detailed-design-v1.9.docx)
- [V1.9 修订对照与决策记录](20261023-详细设计V1.9修订对照.md)：R01–R11、恢复与录音、固定离线计划、作答/绘本统计及 T01–T37 计划矩阵。

2026-10-22 补充：已依据评审生成 **V1.8 修订稿（待确认）**，保留 V1.7 原件。业务选择和建议参数尚待确认，不表示应用已发布或功能已实现。V1.8 采用第 23 章作为唯一持久化模型、第 24 章作为唯一 API 目录、第 27.3 节作为唯一里程碑。

- [V1.8 详细设计修订稿](设计文档/guoyu-english-detailed-design-v1.8.docx)
- [V1.8 修订对照与待确认决策](20261022-详细设计V1.8修订对照.md)
- [V1.8 评审意见](design-review-v1.8.md)：2026-10-23，含异常恢复、任务契约和统计规则的补充评审；处理结果和确认状态见 V1.9 修订对照。
- [V1.7 评审意见](design-review-v1.7.md)

V1.8 另已补充 **21.5 ChinaTextbookStudyFree 专项参考与适配**：按固定提交核查可参考的内容工具链、逐句听读、复习、资源校验与测试方法，区分 MIT 代码和教材素材权利；新增计划用例 T23–T26。具体来源与采用边界见修订对照，不表示已集成该项目或已通过应用验收。

下表及后续元信息描述原 V1.7 归档资料；后续实现参考 V1.9 及本次已确认的决策，原版仅用于历史对照。

| 文件 | 大小 | 说明 |
| --- | --- | --- |
| [设计文档/guoyu-english-detailed-design-v1.7.docx](设计文档/guoyu-english-detailed-design-v1.7.docx) | 约 20 MB | **正式设计基线**。28 章 + 2 附录，含 13 张高保真原型图 |

设计文档元信息：

| 项 | 内容 |
| --- | --- |
| 文档名称 | 果语（Guoyu English）家庭英语启蒙学习系统产品设计与技术方案（方案 A） |
| 版本 | V1.7 |
| 修订日期 | 2026-10-20 |
| 编制部门 | 产品支持部 |
| 用途 | 产品评审、原型确认、第一阶段开发实施 |

因为含高保真图，文件体积 20 MB，Git 里改这个文件会让仓库膨胀。后续正文变更建议改为 Markdown 维护，docx 只在定版时归档。

## 路径不一致——需要处理

根 `README.md` 的「设计资料」一节引用了 6 个路径，**其中 5 个当前不存在，1 个路径写错**：

| 根 README 引用 | 实际状态 |
| --- | --- |
| `docs/design/guoyu-english-detailed-design-v1.7.docx` | 路径错误，实际在 `docs/设计文档/` 下 |
| `docs/design/guoyu-english-detailed-design-v1.7.pdf` | 不存在（README 称 53 页预览版） |
| `docs/database/guoyu-english-schema-v1.7.sql` | 不存在（详设 23.3 称附有 18+ 张表的参考脚本） |
| `docs/development.md` | 不存在 |
| `docs/roadmap.md` | 不存在 |
| `deploy/examples/guoyu-english-docker-compose-v1.7.example.yml` | 不存在 |

两条路选一条：把文件补齐到 README 描述的位置，或者把 README 改成反映真实结构。**别放着不管**——新加入的人点进去全是 404。

## 规划文档结构

依据详设第 21.4、26.1、27 章，建议补成下面这样：

```
docs/
├── README.md                      # 本文件
├── design/                        # 设计文档
│   ├── guoyu-english-detailed-design-v1.7.docx
│   └── guoyu-english-detailed-design-v1.7.pdf
├── database/
│   └── guoyu-english-schema-v1.7.sql   # 参考建表脚本，未联调
├── api/
│   └── api-v1.md                  # /api/v1 接口说明 + 错误码
├── development.md                 # 本地开发环境、分支、提交规范
├── roadmap.md                     # M0–M4 里程碑与验收门槛
├── testing/
│   └── test-matrix.md             # T01–T12 用例
└── THIRD_PARTY_NOTICES.md         # 第三方依赖与素材授权登记
```

## 各文档要点（避免重复造轮子）

**database/** — 参考 SQL 只是设计稿，**不要直接放进 Flyway 迁移目录**。正式迁移脚本在 `server/src/main/resources/db/migration`，由 Flyway 按版本号顺序执行。两者对不上时以 Java 实体和 Flyway 脚本为准。

**api/** — 记录统一响应格式（`requestId / code / message / data / serverTime`）、分页约定（cursor，不用 offset）、业务错误码：

| 业务码 | HTTP | 客户端处理 |
| --- | --- | --- |
| `OK` | 200 | 正常流程 |
| `INVALID_ARGUMENT` | 400 | 局部提示，不无限重试 |
| `DEVICE_REVOKED` | 401/403 | 停止同步，进家长重新绑定 |
| `EVENT_CONFLICT` | 409 | 拉服务端快照后重算 |
| `PACK_VERSION_EXPIRED` | 409 | 刷新清单，不破坏现有可用包 |
| `RATE_LIMITED` | 429 | 按 `Retry-After` 退避 |
| `INTERNAL_ERROR` | 500 | 保留队列，指数退避 |

**testing/** — V1.9 第 27.1 节已维护 T01–T37 计划用例。实施时按第 27.3 节分阶段执行并记录真实结果，不把设计矩阵当作测试通过。

**THIRD_PARTY_NOTICES.md** — 每引入一个依赖或素材，登记：仓库 URL、commit SHA、许可证、用途、是否含媒体素材。MIT 可谨慎集成；GPL/AGPL 只参考思路，不移植代码。这是工程风险提示，不替代法律意见。

## 文档维护

- 设计变更同步核对文档和实现；存在冲突时记录双方依据及决策，不将未经确认的建议自动当作批准规范。
- V1.9 是本次详设修订版本，不代表应用发布版本。业务选择与实测参数分别记录，旧文档保留归档。
- 不把真实儿童姓名、录音、家庭 NAS 地址、密钥写进任何文档。
