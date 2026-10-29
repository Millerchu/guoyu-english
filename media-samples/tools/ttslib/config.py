"""读取扁平的 tts.config.yaml。只接受契约里的六个字段。"""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

from ttslib.errors import TtsError

REQUIRED_KEYS = (
    "speechRate",
    "voice",
    "style",
    "pauseBetweenWordsMs",
    "pauseBetweenSentencesMs",
    "pauseAfterPromptMs",
)


@dataclass(frozen=True)
class TtsConfig:
    speech_rate: float
    voice: str
    style: str
    pause_between_words_ms: int
    pause_between_sentences_ms: int
    pause_after_prompt_ms: int
    source_path: str

    def as_file_dict(self) -> dict[str, object]:
        return {
            "speechRate": self.speech_rate,
            "voice": self.voice,
            "style": self.style,
            "pauseBetweenWordsMs": self.pause_between_words_ms,
            "pauseBetweenSentencesMs": self.pause_between_sentences_ms,
            "pauseAfterPromptMs": self.pause_after_prompt_ms,
        }


def _parse_scalar(raw: str) -> object:
    text = raw.strip()
    if len(text) >= 2 and text[0] == text[-1] and text[0] in "\"'":
        return text[1:-1]
    if " #" in text:
        text = text.split(" #", 1)[0].strip()
    if text in {"", "null", "~", "Null", "NULL"}:
        return ""
    if re.fullmatch(r"-?\d+", text):
        return int(text)
    if re.fullmatch(r"-?\d+\.\d+", text):
        return float(text)
    return text


def load_config(path: Path) -> TtsConfig:
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as exc:
        raise TtsError(f"无法读取 TTS 配置：{path.name}") from exc

    values: dict[str, object] = {}
    for line_number, line in enumerate(lines, start=1):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if ":" not in stripped:
            raise TtsError(f"{path.name}:{line_number} 不是「键: 值」")
        key, raw_value = stripped.split(":", 1)
        key = key.strip()
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", key):
            raise TtsError(f"{path.name}:{line_number} 的键无法识别")
        if key in values:
            raise TtsError(f"{path.name} 里 {key} 重复")
        values[key] = _parse_scalar(raw_value)

    unknown = sorted(set(values) - set(REQUIRED_KEYS))
    missing = [key for key in REQUIRED_KEYS if key not in values]
    if unknown:
        raise TtsError(f"{path.name} 有契约之外的字段：{', '.join(unknown)}")
    if missing:
        raise TtsError(f"{path.name} 缺少字段：{', '.join(missing)}")

    speech_rate = values["speechRate"]
    pauses = (
        values["pauseBetweenWordsMs"],
        values["pauseBetweenSentencesMs"],
        values["pauseAfterPromptMs"],
    )
    voice = values["voice"]
    style = values["style"]
    if isinstance(speech_rate, bool) or not isinstance(speech_rate, (int, float)):
        raise TtsError("speechRate 必须是数字")
    if not 0 < float(speech_rate) <= 2:
        raise TtsError("speechRate 必须在 0 到 2 之间")
    if not isinstance(voice, str) or any(ord(char) < 32 for char in voice):
        raise TtsError("voice 必须是一行文本，可以留空")
    if not isinstance(style, str) or not style.strip() or any(ord(char) < 32 for char in style):
        raise TtsError("style 必须是一行非空文本")
    for pause in pauses:
        if isinstance(pause, bool) or not isinstance(pause, int) or not 0 <= pause <= 5000:
            raise TtsError("停顿毫秒必须是 0 到 5000 的整数")

    return TtsConfig(
        speech_rate=float(speech_rate),
        voice=voice.strip(),
        style=style.strip(),
        pause_between_words_ms=int(pauses[0]),
        pause_between_sentences_ms=int(pauses[1]),
        pause_after_prompt_ms=int(pauses[2]),
        source_path=path.name,
    )
