# 动物课示范音频清单

用途：动物课 `animals-001` 版本 1 的五个单词和五个句子。`storage_key` 是相对 `media-samples/` 的路径，也是以后写入 `media_asset.storage_key` 的值。本文件和同目录的 `metadata.json` 由同一次生成写出，不要手改哈希、字节数或原文。

这些文件是离线合成的示范音，音质不像人声，不是真人录音，也不是已审过的正式教材音。

默认语速 0.80，并在词与词之间留出停顿，是为了让幼儿园大班的孩子听清并跟着说。说明见 `media-samples/README.md`。

停顿已经写进每个 mp3：开头是开口前的静音，句子里的单词之间是词间静音，结尾留给孩子跟读。播放时按原速即可，不要再把语速乘一次 0.80，也不要再叠一段同样的静音。

## 生成参数

| 字段 | 值 |
| --- | --- |
| generated_at | 2026-10-29T15:05:20Z |
| provider | offline |
| source | OFFLINE_TTS |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| voice | en-us+m3 |
| voice_requested | （空） |
| speech_rate | 0.80 |
| style | clear-warm |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| config_file | {"pauseAfterPromptMs": 400, "pauseBetweenSentencesMs": 900, "pauseBetweenWordsMs": 600, "speechRate": 0.8, "style": "clear-warm", "voice": ""} |

## 文件

### audio/animals/cat-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/cat-v1.mp3 |
| type | 音频 |
| kind | word |
| lexeme_id | lexeme-cat |
| sentence_id | （无） |
| source_text | cat |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 21850 |
| sha256 | 695032ccdd3f18889eb362db351a62cdcb705bf020b55f7d240ae90aba324ad5 |
| duration_ms | 1776 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["cat"], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课听一听、认一认，lexeme-cat |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/dog-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/dog-v1.mp3 |
| type | 音频 |
| kind | word |
| lexeme_id | lexeme-dog |
| sentence_id | （无） |
| source_text | dog |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 22163 |
| sha256 | 7bbf0624102a19a549d235d2bd9a53b6d53afbb183fa948aba26e179b1c2c7fb |
| duration_ms | 1802 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["dog"], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课听一听、认一认，lexeme-dog |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/rabbit-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/rabbit-v1.mp3 |
| type | 音频 |
| kind | word |
| lexeme_id | lexeme-rabbit |
| sentence_id | （无） |
| source_text | rabbit |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 23734 |
| sha256 | d4c7fa819868b98c9f7957cf3831579be6cbde890750a84d003410e660556ba3 |
| duration_ms | 1933 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["rabbit"], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课听一听、认一认，lexeme-rabbit |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/bird-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/bird-v1.mp3 |
| type | 音频 |
| kind | word |
| lexeme_id | lexeme-bird |
| sentence_id | （无） |
| source_text | bird |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 22164 |
| sha256 | e55a9f25313d69167e0ed58da9d8d4aaa44e21f0603778647d329198ad94bd81 |
| duration_ms | 1802 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["bird"], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课听一听、认一认，lexeme-bird |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/fish-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/fish-v1.mp3 |
| type | 音频 |
| kind | word |
| lexeme_id | lexeme-fish |
| sentence_id | （无） |
| source_text | fish |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 22164 |
| sha256 | ff5731d063f4106bc5986ab5a34d41d1dd68e21a614648b5eaf1782306553e1b |
| duration_ms | 1802 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["fish"], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课听一听、认一认，lexeme-fish |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/i-see-a-cat-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/i-see-a-cat-v1.mp3 |
| type | 音频 |
| kind | sentence |
| lexeme_id | （无） |
| sentence_id | sent-i-see-a-cat |
| source_text | I see a cat. |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 55400 |
| sha256 | 738e6154eafab886f8148af93f5c9963718c6d0b46094b70d88e9ca1f6fc9cb0 |
| duration_ms | 4571 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["I", "see", "[[a#]]", "cat."], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课跟我说、读一句，sent-i-see-a-cat |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/i-see-a-dog-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/i-see-a-dog-v1.mp3 |
| type | 音频 |
| kind | sentence |
| lexeme_id | （无） |
| sentence_id | sent-i-see-a-dog |
| source_text | I see a dog. |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 55714 |
| sha256 | 8e694823e36d4eacf138885a3255ea1754395e5b91d22a09da7af5b9dfb03766 |
| duration_ms | 4598 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["I", "see", "[[a#]]", "dog."], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课跟我说、读一句，sent-i-see-a-dog |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/i-like-rabbits-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/i-like-rabbits-v1.mp3 |
| type | 音频 |
| kind | sentence |
| lexeme_id | （无） |
| sentence_id | sent-i-like-rabbits |
| source_text | I like rabbits. |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 49134 |
| sha256 | f955de2d731be18ea8236e9b72eac2b904d40d2b0c42b0c2fa21dd425c797a01 |
| duration_ms | 4049 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["I", "like", "rabbits."], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课跟我说、读一句，sent-i-like-rabbits |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/its-a-dog-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/its-a-dog-v1.mp3 |
| type | 音频 |
| kind | sentence |
| lexeme_id | （无） |
| sentence_id | sent-its-a-dog |
| source_text | It's a dog. |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 44428 |
| sha256 | 28bbd9fdcd346a23ac9884f8427e116f8cca1fd51ac09431cd53a193ad91ea8a |
| duration_ms | 3657 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["It's", "[[a#]]", "dog."], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课跟我说、读一句，sent-its-a-dog |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

### audio/animals/whats-this-v1.mp3

| 字段 | 值 |
| --- | --- |
| path | audio/animals/whats-this-v1.mp3 |
| type | 音频 |
| kind | sentence |
| lexeme_id | （无） |
| sentence_id | sent-whats-this |
| source_text | What's this? |
| source | OFFLINE_TTS |
| speech_rate | 0.80 |
| style | clear-warm |
| voice | en-us+m3 |
| pause_between_words_ms | 600 |
| pause_between_sentences_ms | 900 |
| pause_after_prompt_ms | 400 |
| tool | espeak-ng 1.51 + ffmpeg 6.1.1-3ubuntu5 |
| generated_at | 2026-10-29T15:05:20Z |
| mime | audio/mpeg |
| size_bytes | 35338 |
| sha256 | 1cf49505d116239143ed0854625d56aaf9def7ca406e41045b807713c5229b41 |
| duration_ms | 2900 |
| engine_parameters | {"adapter": "espeak-ng", "amplitude": 100, "espeak_tokens": ["What's", "this?"], "pauses_applied_by": "tool", "pitch": 42, "sample_rate_hz": 22050, "speech_rate": 0.8, "speech_rate_applied": true, "speed_basis_wpm": 175, "speed_wpm": 140, "voice": "en-us+m3"} |
| license | 合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。 |
| attribution | 不要把合成音写成真人录音或官方教材音。工具名称见 tool。 |
| credential | 无。生成时不把 API Key 写入清单。 |
| version | 1 |
| purpose | 动物课跟我说、读一句，sent-whats-this |
| warning | 离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。 |

机器可读副本：`audio/animals/metadata.json`。字段用 `speech_rate`、`source_text`、`generated_at` 这些名称，方便以后抄进 `media_asset`，不另起一套。
