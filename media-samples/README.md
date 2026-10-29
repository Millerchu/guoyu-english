# media-samples/ — 演示素材

存放**自制或已取得授权**的演示素材：示例单词卡、示例音频、示例绘本页。

> **当前状态：** 动物课十段慢速英文示范音在 [`audio/animals/`](audio/animals/MANIFEST.md)。图片仍由后续阶段制作，这里还没有。
> 这个目录永远不该出现真实用户内容——不放真实儿童录音、不放家庭照片、不放从商业 App 里扒下来的任何东西，也不提交 TTS API Key。

## 用途边界

| 放这里 | 不放这里 |
| --- | --- |
| 自己录的示范音频 | 真实孩子的跟读录音 |
| 自制或可商用图库的插画 | 第三方 App / 商业绘本的扫描件 |
| 自编超短篇绘本页（6–8 页） | 受版权保护的出版物内容 |
| 授权素材 + 授权说明 | 来源不明的网络图片 |
| 图标、奖章、贴纸设计稿 | 品牌 Logo 的未核验商标版本 |

运行时素材不走这里。生产环境的媒体文件挂在 NAS 的 `/data/media/` 下，由 Nginx 直接暴露，例如 `/media/audio/animals/cat.mp3`。本目录只放**可进 Git 的小体积示例**。

## 建议目录结构

图片清单和音频清单分开，避免两个阶段改同一个文件。

```
media-samples/
├── image/animals/          # 后续阶段：自制图和 MANIFEST.md
├── audio/animals/          # 十段 mp3、MANIFEST.md、metadata.json
├── tts/tts.config.yaml     # 语速、音色、风格、三项停顿
├── tools/                  # 可替换的生成工具，不绑定厂商 SDK
├── book/                   # 绘本页，尚未制作
└── speech/                 # 录音格式示例，尚未制作；只用合成音
```

## 命名规范

小写 + 连字符，主题作子目录，文件名用英文单词本身：

```
audio/animals/cat.mp3
audio/animals/cat-v1.mp3        # 带版本号，避免缓存混淆
image/animals/cat.webp
book/i-see-animals/page-01.webp
```

资源包里引用的是 `path` + `size` + `sha256` + `mime`，所以**改了内容就必须改版本号**，否则客户端 SHA-256 校验会失败、包切换不生效。

## 授权登记

每个素材在对应主题的 `MANIFEST.md` 里登记，缺这行就不该被提交。动物课音频的登记在 [`audio/animals/MANIFEST.md`](audio/animals/MANIFEST.md)，机器可读副本是同目录的 `metadata.json`。

| 字段 | 说明 |
| --- | --- |
| 文件路径 | 相对本目录 |
| 类型 | 图片 / 音频 / 绘本页 / 图标 |
| 来源 | 自制 / 图库名 / 录音者 |
| 许可证 | CC0 / CC BY 4.0 / 商用授权编号 / 自制 |
| 授权凭证 | 购买记录或授权书存放位置 |
| 备注 | 是否需要署名、可否商用、到期日 |

## 体积控制

Git 不适合放大文件。单个素材建议 ≤ 500 KB；音频统一转 mp3 或 m4a（AAC），图片优先 webp。整目录超过 20 MB 就该考虑 Git LFS 或干脆只留清单。

## 可替换的慢速 TTS

动物课的十段英文要能播放，但 TTS 厂商还没定。生成工具因此有三条路，仓库里不放任何一家 SDK：

| 提供方 | 何时使用 | 清单里的来源 |
| --- | --- | --- |
| `offline` | 本机 `espeak-ng`，不需要密钥 | `OFFLINE_TTS` |
| `http` | 只读 `TTS_API_BASE`、`TTS_API_KEY`、`TTS_VOICE`，POST 一段通用 JSON，等待音频字节 | `AI_GENERATED` |
| `placeholder` | 没有语音引擎时的占位音，听不出单词 | `PLACEHOLDER` |

不设置 `TTS_PROVIDER` 时：有密钥就走 HTTP；否则有 `espeak-ng` 就走离线；再没有就用占位音。已经配了密钥但请求失败时，工具直接报错，不会把离线音或占位音标成外部合成。占位音也不能覆盖已经生成好的语音，除非明确指定 `TTS_PROVIDER=placeholder` 或加上 `--allow-placeholder`。

默认参数在 [`tts/tts.config.yaml`](tts/tts.config.yaml)，与共享契约一致：语速 `0.80`，风格 `clear-warm`，词间 600 毫秒，句间 900 毫秒，开口前 400 毫秒。`voice` 留空。离线引擎在没有指定音色时用 `en-us+m3`，音高 42（引擎默认是 50），取一个偏低、不尖的声音。

这组值是给幼儿园大班、大约 5 到 6 岁的孩子跟读的。`espeak-ng` 的默认语速大约是每分钟 175 词，0.80 倍大约是每分钟 140 词，发音本身已经放慢。真正拉开和日常说话距离的是停顿：一个句子里的单词之间空 600 毫秒，整句说完再留 900 毫秒，孩子才来得及听清并开口。开头再留 400 毫秒，避免一播放就出声。这十段文件都只有一个单词或一个句子，所以 900 毫秒落在文件末尾，而不是两句中间。冠词 a 若拆开朗读会被读成字母 A，工具改用弱读。停顿已经做进 mp3，播放端按原速播放，不要再乘一次 0.80，也不要再叠同样的静音。

当前提交的十段是离线合成音。音质不像人声，不是真人录音，也不是审过的正式教材音。选定厂商以后，用同一工具换提供方，把文件版本从 `v1` 往上加，并同时更新哈希。HTTP 适配器不识别厂商私有 JSON；真要接某一家，另加一个适配器，不要把那家 SDK 写成唯一实现。

密钥只放环境变量，示例值用 `CHANGE_ME`。不要写进配置、清单、命令历史或提交。

在仓库根目录执行。需要本机的 `ffmpeg`；离线语音还需要 `espeak-ng`。

```bash
python3 media-samples/tools/generate_animal_audio.py
python3 media-samples/tools/generate_animal_audio.py --check
python3 media-samples/tools/test_tts.py
```

换成本地 HTTP 服务时：

```bash
TTS_PROVIDER=http \
TTS_API_BASE=https://tts.example.invalid/v1/speech \
TTS_API_KEY=CHANGE_ME \
TTS_VOICE=clear-warm \
python3 media-samples/tools/generate_animal_audio.py
```

`metadata.json` 里同时有契约语速 `speech_rate` 和引擎自己的参数（离线引擎是 `speed_wpm`、`pitch`、`voice`）。`source_text` 只有英文。哈希以这份文件为准，供后续课程种子读取。本阶段没有服务端迁移。

## 红线

- 不复制其他项目的绘本、图片、音频、字典内容。详设 21.3 明确写了这条。
- 首版素材只能是：自制词汇卡、可商用图库素材、自己录制或合法授权的音频。
- 商用图库素材要留购买凭证，未来若商业化，这是审计依据。
