"""清单和 metadata.json 从同一份结构生成，避免手改哈希。"""

from __future__ import annotations

import json
from typing import Any


def _cell(value: object) -> str:
    if isinstance(value, float):
        text = f"{value:.2f}"
    elif isinstance(value, (dict, list)):
        text = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(", ", ": "))
    else:
        text = str(value)
    return text.replace("|", "/").replace("\n", " ")


def _table(rows: list[tuple[str, object]]) -> list[str]:
    lines = ["| 字段 | 值 |", "| --- | --- |"]
    for key, value in rows:
        lines.append(f"| {key} | {_cell(value)} |")
    return lines


def render_manifest(document: dict[str, Any]) -> str:
    source = document["source"]
    if source == "PLACEHOLDER":
        banner = "这些文件是占位音，听不到单词或句子，不能当作教材音频。"
    elif source == "OFFLINE_TTS":
        banner = "这些文件是离线合成的示范音，音质不像人声，不是真人录音，也不是已审过的正式教材音。"
    else:
        banner = "这些文件来自外部 HTTP TTS。能否发布取决于该服务自己的授权，本工具没有确认过。"

    lines = [
        "# 动物课示范音频清单",
        "",
        "用途：动物课 `animals-001` 版本 1 的五个单词和五个句子。`storage_key` 是相对 `media-samples/` 的路径，也是以后写入 `media_asset.storage_key` 的值。本文件和同目录的 `metadata.json` 由同一次生成写出，不要手改哈希、字节数或原文。",
        "",
        banner,
        "",
        "默认语速 0.80，并在词与词之间留出停顿，是为了让幼儿园大班的孩子听清并跟着说。说明见 `media-samples/README.md`。",
        "",
        "停顿已经写进每个 mp3：开头是开口前的静音，句子里的单词之间是词间静音，结尾留给孩子跟读。播放时按原速即可，不要再把语速乘一次 0.80，也不要再叠一段同样的静音。",
        "",
        "## 生成参数",
        "",
    ]
    parameter_rows: list[tuple[str, object]] = [
        ("generated_at", document["generated_at"]),
        ("provider", document["provider"]),
        ("source", document["source"]),
        ("tool", document["tool"]),
        ("voice", document["voice"] or "（空）"),
        ("voice_requested", document["voice_requested"] or "（空）"),
        ("speech_rate", document["speech_rate"]),
        ("style", document["style"]),
        ("pause_between_words_ms", document["pause_between_words_ms"]),
        ("pause_between_sentences_ms", document["pause_between_sentences_ms"]),
        ("pause_after_prompt_ms", document["pause_after_prompt_ms"]),
        ("engine_parameters", document["engine_parameters"]),
        ("config_file", document["config"]),
    ]
    lines.extend(_table(parameter_rows))
    lines.extend(["", "## 文件", ""])
    for asset in document["assets"]:
        lines.append(f"### {asset['storage_key']}")
        lines.append("")
        lines.extend(
            _table(
                [
                    ("path", asset["storage_key"]),
                    ("type", "音频"),
                    ("kind", asset["kind"]),
                    ("lexeme_id", asset["lexeme_id"] or "（无）"),
                    ("sentence_id", asset["sentence_id"] or "（无）"),
                    ("source_text", asset["source_text"]),
                    ("source", asset["source"]),
                    ("speech_rate", asset["speech_rate"]),
                    ("style", asset["style"]),
                    ("voice", asset["voice"] or "（空）"),
                    ("pause_between_words_ms", asset["pause_between_words_ms"]),
                    ("pause_between_sentences_ms", asset["pause_between_sentences_ms"]),
                    ("pause_after_prompt_ms", asset["pause_after_prompt_ms"]),
                    ("tool", asset["tool"]),
                    ("generated_at", asset["generated_at"]),
                    ("mime", asset["mime"]),
                    ("size_bytes", asset["size_bytes"]),
                    ("sha256", asset["sha256"]),
                    ("duration_ms", asset["duration_ms"]),
                    ("engine_parameters", asset["engine_parameters"]),
                    ("license", asset["license"]),
                    ("attribution", asset["attribution"]),
                    ("credential", asset["credential"]),
                    ("version", asset["version"]),
                    ("purpose", asset["purpose"]),
                    ("warning", asset["warning"]),
                ]
            )
        )
        lines.append("")
    lines.append("机器可读副本：`audio/animals/metadata.json`。字段用 `speech_rate`、`source_text`、`generated_at` 这些名称，方便以后抄进 `media_asset`，不另起一套。")
    lines.append("")
    return "\n".join(lines)
