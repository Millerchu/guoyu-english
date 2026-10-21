# 果语 · Guoyu English

面向儿童的家庭英语启蒙学习系统。通过主题课程、绘本点读、录音跟读和成长奖励，让英语练习融入日常生活。

**核心原则：NAS 局域网服务端为主，外出离线学习，回家自动同步。**

> 当前阶段：项目目录与文档基线初始化。V1.7 为设计版本，应用版本尚未发布。仓库尚未包含可运行的 Spring Boot / uni-app 脚手架；下列功能均为规划能力。

## 产品规划

| 模块 | 规划能力 |
| --- | --- |
| 今日学习 | 听一听、跟我说、认一认、读一句、小游戏的每日学习闭环 |
| 绘本学习 | 绘本馆、逐页点读、阅读进度及完成记录 |
| 自然拼读 | 基础字母、字母音与拼读练习 |
| 成长奖励 | 星星、徽章、学习成长日历和月度学习统计 |
| 家长中心 | 儿童档案、学习报告、离线内容和同步状态 |
| 离线与同步 | Android 完整离线能力、H5 基础离线能力、学习事件增量同步 |

## 技术方案

| 层次 | 设计基线 |
| --- | --- |
| 客户端 | uni-app、Vue 3、TypeScript，一套代码输出 Android App 和 H5 |
| 服务端 | Java 17、Spring Boot 3.x，模块化单体 |
| 数据库 | PostgreSQL 16+；后续使用 Flyway 管理正式迁移 |
| Android 本地存储 | SQLite、App 私有文件目录 |
| H5 本地存储 | IndexedDB、Cache Storage |
| 家庭部署 | NAS、Docker Compose、Nginx / 可信 HTTPS 入口 |

Android APK 安装在手机或平板上；H5、后端及数据库运行在 NAS。家庭版首版不引入 Nacos、Seata、消息队列等额外基础设施。

学习行为在本地形成待同步事件，服务端按事件标识去重、计算学习进度和奖励，并通过增量接口回传。服务端保存权威数据，客户端保留离线资源及待确认记录。

回家自动同步在应用启动、回到前台、网络恢复或手动触发时执行；应用被系统终止后，不保证即时后台同步。H5 录音和 Service Worker 通常需要设备信任的 HTTPS 安全上下文。

## 仓库结构

采用单仓库管理，前后端仍可独立开发、构建和发布。

| 路径 | 用途 |
| --- | --- |
| [client/](client/README.md) | Android / H5 共用客户端及端侧适配目录 |
| [server/](server/README.md) | Spring Boot 后端及业务模块目录 |
| [deploy/](deploy/README.md) | NAS Docker Compose、环境变量和反向代理配置 |
| [docs/](docs/README.md) | 设计文档、接口、数据库和开发规范 |
| [media-samples/](media-samples/README.md) | 自制或取得授权的演示素材 |

## 设计资料

正式基线为 **《果语（Guoyu English）详细设计 V1.7》**，编制部门：产品支持部。原有 Word、PDF、SQL 和 Compose 文件均按原文件归档。

- [Word 详细设计 V1.7](docs/design/guoyu-english-detailed-design-v1.7.docx)：可编辑版本，保留高保真原型。
- [PDF 详细设计 V1.7](docs/design/guoyu-english-detailed-design-v1.7.pdf)：53 页预览版。
- [PostgreSQL 参考建表脚本](docs/database/guoyu-english-schema-v1.7.sql)。
- [NAS Docker Compose 参考配置](deploy/examples/guoyu-english-docker-compose-v1.7.example.yml)。
- [文档索引与文件校验](docs/README.md)。

SQL 和 Compose 属于设计参考，尚未联调验证。Compose 内的 `YOUR_ORG` / `CHANGE_ME` 为占位值，不能直接作为正式部署配置。SQL 暂不放入自动执行的 Flyway 迁移目录。

## 开始参与开发

```bash
git clone https://github.com/Millerchu/guoyu-english.git
cd guoyu-english
```

1. 先阅读 V1.7 详设、[开发指南](docs/development.md)和[首版里程碑](docs/roadmap.md)。
2. 使用 IDEA 打开仓库，后端代码位于 `server/`；客户端可使用 HBuilderX 或兼容 uni-app 的开发环境维护。
3. 下一步生成服务端 Maven 与客户端 uni-app 工程，确认依赖版本并提交锁定文件，再实现第一个可运行闭环。

当前没有 `pom.xml`、`package.json` 或正式部署镜像，因此暂不提供启动、构建或一键部署命令。

## 开发约定

- Java 基础包名为 `com.guoyu.english`，遵循《阿里巴巴 Java 开发手册》的代码与注释规范。
- 采用短期功能分支，通过 Pull Request 合并到 `main`；提交说明清楚描述目的。
- 客户端页面通过仓储接口访问数据，由平台适配层处理 SQLite / IndexedDB 等差异。
- 不提交真实儿童档案、录音、学习记录、数据库备份、家庭 NAS 地址或密钥；环境配置从示例复制后在本地填写。
- 自制素材与第三方内容分开记录来源。引入第三方代码前核实许可证，文档中的调研不等于已引入相应代码。

## 许可证

项目许可证尚未确定，目前未授予额外开源使用许可。参考项目的许可证不适用于本仓库原创内容。

---

文档维护：产品支持部
