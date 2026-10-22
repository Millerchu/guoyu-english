# client/ — 客户端（Android App + H5）

uni-app（Vue 3 + TypeScript + Pinia）单套代码，同时输出 **Android APK** 与 **H5**。
儿童端与家长端页面都在这一套工程里，不拆两个仓库。

> **当前状态：空目录。** 没有 `package.json`、没有 `pages.json`、没有 `manifest.json`。
> 下面描述的是依据《详细设计 V1.7》第 9、20、25 章确定的**目标结构**，不是已实现的东西。
> 在脚手架落地前，本文件不作为可执行指南。

## 技术基线

| 项 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | uni-app（Vue 3 语法） | 一套代码双端输出 |
| 语言 | TypeScript | 不允许在业务代码里写 `any` 兜底 |
| 状态 | Pinia | 学习进度、同步状态、儿童档案 |
| UI | 主题化自定义儿童组件 | 不直接套 Vant 默认皮肤，儿童端按钮要重做 |
| Android 最低版本 | Android 8.0（API 26） | 见详设 20.7 |
| 应用 ID | `com.guoyu.english` | 首次打包前固定，之后不要改 |

## 规划目录结构

```
client/
├── src/
│   ├── pages/                 # 页面（tabBar 5 项 + 学习流程页 + 家长页）
│   ├── components/            # 儿童端通用组件：大按钮、圆角卡片、星星、小兔老师
│   ├── stores/                # Pinia：child / learning / sync / reward
│   ├── repositories/          # 数据访问接口层（双端差异都收敛在这里）
│   │   ├── types.ts           # Repository 接口定义
│   │   ├── android/           # SQLite + App 私有文件
│   │   └── h5/                # IndexedDB + Cache Storage
│   ├── sync/                  # outbox、push/pull、资源包校验、重试退避
│   ├── api/                   # /api/v1 请求封装、统一响应解包
│   ├── media/                 # 音频播放、录音（Android 原生 / H5 getUserMedia）
│   ├── styles/                # 品牌色、字号、圆角、间距 token
│   └── utils/
├── pages.json
├── manifest.json
├── package.json
└── tsconfig.json
```

## 建议页面路由

对应详设第 7 章的 13 张高保真原型。tabBar 只放前 5 项，家长入口走长按/PIN 进入。

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

客户端不是权威数据源，只做三件事：

1. **本地先写**：学习动作 → 同事务写本地状态 + outbox（不可拆，拆了就会丢记录）。
2. **Push**：批量上报，单批上限 100 条 / 256KB。每条按 `eventId` 独立返回 `ACCEPTED` / `DUPLICATE` / `REJECTED`。
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

## 开发环境

脚手架落地后预期命令（**当前不可执行**）：

```bash
cd client
npm install
npm run dev:h5        # H5 调试
npm run build:h5      # 产物给 deploy/ 的 Nginx 容器
npm run dev:app       # HBuilderX 或 CLI 跑 Android 真机
```

## 禁止提交

真实儿童录音、真实学习记录、`manifest.json` 里的签名证书路径与密码、NAS 内网地址、任何第三方 App 的课程文本/插画/配音/绘本扫描件。
