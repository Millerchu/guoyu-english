# client/ — 客户端（Android App + H5）

uni-app（Vue 3 + TypeScript + Pinia）单套代码，同时输出 **Android APK** 与 **H5**。
儿童端与家长端页面都在这一套工程里，不拆两个仓库。

当前已接上 S09 的本地仓储。H5 用 IndexedDB，Android 用同一套接口上的 SQLite 适配。学习动作和 outbox、预估星星在同一个事务里写入；失败时不会显示已保存。`eventId` 由调用方创建，重试不更换。Push 体带 `schemaVersion=2` 和原来的 `eventId`，走 `/api/v1/sync/push`。学习页还没有。Android 适配没有真机或模拟器结果。

## 技术基线

| 项 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | uni-app（Vue 3 语法） | 官方 `dcloudio/uni-preset-vue#vite-ts`。npm 包 `3.0.0-5020620260917001`，构建日志为 Compiler version 5.26（vue3） |
| 语言 | TypeScript | 不允许在业务代码里写 `any` 兜底 |
| 状态 | Pinia `2.1.7` | 已在 `src/main.ts` 接入。学习进度、同步状态、儿童档案的 store 还没有 |
| UI | 主题化自定义儿童组件 | 不直接套 Vant 默认皮肤。本阶段只有首页按钮和 `src/styles/tokens.css` |
| Node | `^18.0.0 \|\| >=20.0.0` | 与脚手架锁定的 Vite `5.2.8` 的 `engines` 相同 |
| 包管理 | npm | 锁文件是 `package-lock.json` |
| Android 最低版本 | Android 8.0（API 26） | `src/manifest.json` 已写 `minSdkVersion` 26。还没有 APK |
| 应用 ID | `com.guoyu.english` | 已写入 `src/manifest.json` 的 Android `packagename` |

## 当前目录

官方 CLI 把页面和配置放在 `src/`。不要再把 `pages.json`、`manifest.json` 挪到 `client/` 根目录。

```
client/
├── src/
│   ├── pages/index/index.vue  # 首页壳，本阶段唯一页面
│   ├── repositories/          # 仓储接口、IndexedDB、SQLite
│   ├── sync/                  # outbox 拆批、退避、逐条 ACK
│   ├── api/                   # /api/v1/sync 的类型和 HTTP 传输
│   ├── styles/tokens.css      # 字号、间距、圆角、主按钮高度
│   ├── pages.json             # 只注册首页，没有 tabBar
│   ├── manifest.json          # 应用名「果语」，Android 应用 ID
│   ├── App.vue
│   └── main.ts                # Pinia
├── package.json
├── package-lock.json
└── tsconfig.json
```

这些目录还没有，留给后续阶段：`src/pages/learn/`、`src/components/`、`src/stores/`、`src/media/`。存储实现放在 `src/repositories/`，没有再拆 `h5/` 和 `android/` 子目录。

## 页面路由

共享契约已经点名 `pages/index/index`、`pages/learn/listen`、`pages/learn/speak`、`pages/learn/recognize`、`pages/learn/complete`。读一句 `pages/learn/read` 到 S12 再做。本阶段只注册首页。

tabBar、游戏入口仍有评审分歧，共享契约没有登记 tabBar。本阶段不放 tabBar，也不把下表其余行当成已经实现的页面。

下表仍是详设第 7 章的页面清单，供后续阶段对照。

| 路由 | 页面 | 对应原型 |
| --- | --- | --- |
| `pages/index/index` | 首页 / 今日 | 7.2 |
| `pages/today/today` | 今日学习任务分解 | 7.2 |
| `pages/learn/listen` | 听一听 | 5.1 ① |
| `pages/learn/speak` | 跟我说（录音+回放） | 7.3 |
| `pages/learn/recognize` | 认一认（听音选图） | 7.3 |
| `pages/learn/read` | 读一句 | 7.4 |
| `pages/learn/game` | 小游戏 | 7.4 |
| `pages/book/index` | 绘本馆 | 7.5 |
| `pages/book/detail` | 绘本点读 | 7.5 |
| `pages/phonics/index` | 自然拼读 | 7.6 |
| `pages/reward/index` | 奖励 | 7.6 |
| `pages/calendar/index` | 学习日历 / 月度打卡 | 7.8 |
| `pages/parent/index` | 家长中心 | 7.7 |
| `pages/parent/report` | 学习报告 | 7.7 |
| `pages/parent/settings` | 学习设置 / 设备管理 | 8.1、22.5 |

## 双端差异（唯一允许分叉的地方）

| 能力 | Android | H5 |
| --- | --- | --- |
| 本地存储 | SQLite（App 专用） | IndexedDB |
| 课程资源 | App 私有文件目录 + manifest | Cache Storage + IndexedDB 索引 |
| 录音 | 原生录音能力，`RECORD_AUDIO` 权限 | `getUserMedia` + `MediaRecorder`，**必须 HTTPS** |
| 离线启动 | 装完 APK 即可看已下载课程 | 必须先缓存 PWA shell，受浏览器策略限制 |
| 同步触发 | 启动 / 返回前台 / 网络恢复 / 手动 | 页面存活或下次打开，**不承诺后台自动** |
| 升级 | 需更新 APK | 刷新即生效 |

**别踩的坑**：用 `http://192.168.x.x` 打开的 H5 不属于安全上下文，`getUserMedia` 直接不可用，Service Worker 也起不来。这不是兼容性问题，是规范限制。要录音就上内网 HTTPS 域名。

## 端侧同步职责

客户端不是权威数据源。页面通过 `src/repositories/` 的五个方法访问，不直接打开 IndexedDB，也不直接发 HTTP。数据库打不开或事务失败时，方法会拒绝；读失败不会返回空列表冒充成功。失败反馈的 `saved` 固定为 `false`，文案不是「已保存」。

`saveLearningAction` 在同一事务里写进度、outbox 和跟我说的预估星星。听一听、认一认、读一句不写预估星星。跟我说同一句子只留一条 `PENDING` 预估，ACK 不删除它。`listOutbox` 只返回尚未确认的事件。`ACCEPTED` 和 `DUPLICATE` 在同一事务写入确认记录后再移出 outbox；`REJECTED` 留在本地并退出待推送队列，`eventId` 不变。`savePullPage` 先写变更，最后写游标；中途失败则整页回滚，旧游标还在。

Push 请求字段是 `schemaVersion`、`streamEpoch`、`events[].eventId/eventType/stepId/targetId`。学习动作签名里没有 `childId`，这里不编造儿童编号。单批最多 100 条、256 KiB。网络失败按 5 秒、15 秒、45 秒、最长 5 分钟退避，抖动不超过该档；`429` 优先使用 `Retry-After`。`DEVICE_REVOKED` 停止同步，不把队列改记到别的身份。

H5 数据库名是 `guoyu-english`。Android 请求的库路径是应用私有的 `_doc/guoyu-english.db`。`ANDROID_SQLITE_DEVICE_VERIFIED` 固定为 `false`：自动化测试用注入的 SQLite 执行同一条 SQL，没有 Android 真机、模拟器或 HBuilderX 打包结果。

客户端只做三件事：

1. **本地先写**：学习动作 → 同事务写本地状态 + outbox（不可拆，拆了就会丢记录）。
2. **Push**：批量上报，单批上限 100 条 / 256 KiB。每条按 `eventId` 独立返回 `ACCEPTED` / `DUPLICATE` / `REJECTED`。
3. **Pull**：按 `serverCursor` 增量拉取，**必须先把所有变更持久化成功，才能保存 nextCursor**。

重试退避：5s → 15s → 45s → 上限 5min，带随机抖动。收到 `429` 按 `Retry-After` 走；收到 `DEVICE_REVOKED` 立即停止同步，进家长重新绑定流程，不要拿别的儿童身份重试。

## 交互硬性约束

- 主按钮高度 ≥ 48dp，儿童端优先 56–64dp。
- 一屏一件事，主按钮唯一。
- 答错不出现红色「失败」文案，只用「再听一次」「再试试」。
- 进入页面不自动大声播放音频。
- 录音必须显式显示状态，并限制单次时长。
- 学习任务中返回要保留进度，不清空已完成步骤。
- 儿童端不展示服务器 IP、同步日志、管理按钮。

## 本地运行

在 `client/` 目录执行。Node 需满足 `package.json` 的 `engines.node`，即 `^18.0.0 || >=20.0.0`。

```bash
cd client
npm install
npm run type-check
npm run check:repository
npm run check:storage
npm run dev:h5
npm run build:h5
```

`check:storage` 覆盖事务回滚、重试不换 `eventId`、Pull 中途失败不推进游标。它使用 Node 内置的 `node:sqlite` 和 `fake-indexeddb`，不能代替浏览器或 Android 真机。

`dev:h5` 启动开发服务器。`build:h5` 的页面在 `dist/build/h5/`。

没有 `dev:app` 脚本。Android 打包仍要 HBuilderX，本阶段没有 APK，也没有真机结果。

## 禁止提交

真实儿童录音、真实学习记录、`manifest.json` 里的签名证书路径与密码、NAS 内网地址、任何第三方 App 的课程文本/插画/配音/绘本扫描件。
