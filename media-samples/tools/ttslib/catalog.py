"""动物课十段英文。文本和文件名与共享契约一致，不包含中文释义。"""

from __future__ import annotations

import re
from dataclasses import dataclass

from ttslib.errors import TtsError

_TOKEN = re.compile(r"^([^A-Za-z]*)([A-Za-z']+)([^A-Za-z]*)$")
_CJK = re.compile(r"[\u4e00-\u9fff]")


@dataclass(frozen=True)
class LessonLine:
    kind: str
    item_id: str
    source_text: str
    filename: str

    @property
    def storage_key(self) -> str:
        return f"audio/animals/{self.filename}"

    @property
    def lexeme_id(self) -> str:
        return self.item_id if self.kind == "word" else ""

    @property
    def sentence_id(self) -> str:
        return self.item_id if self.kind == "sentence" else ""

    @property
    def purpose(self) -> str:
        if self.kind == "word":
            return f"动物课听一听、认一认，{self.item_id}"
        return f"动物课跟我说、读一句，{self.item_id}"


LESSON_LINES: tuple[LessonLine, ...] = (
    LessonLine("word", "lexeme-cat", "cat", "cat-v1.mp3"),
    LessonLine("word", "lexeme-dog", "dog", "dog-v1.mp3"),
    LessonLine("word", "lexeme-rabbit", "rabbit", "rabbit-v1.mp3"),
    LessonLine("word", "lexeme-bird", "bird", "bird-v1.mp3"),
    LessonLine("word", "lexeme-fish", "fish", "fish-v1.mp3"),
    LessonLine("sentence", "sent-i-see-a-cat", "I see a cat.", "i-see-a-cat-v1.mp3"),
    LessonLine("sentence", "sent-i-see-a-dog", "I see a dog.", "i-see-a-dog-v1.mp3"),
    LessonLine("sentence", "sent-i-like-rabbits", "I like rabbits.", "i-like-rabbits-v1.mp3"),
    LessonLine("sentence", "sent-its-a-dog", "It's a dog.", "its-a-dog-v1.mp3"),
    LessonLine("sentence", "sent-whats-this", "What's this?", "whats-this-v1.mp3"),
)


def speech_tokens(source_text: str) -> list[str]:
    """把一句英文拆成送给离线引擎的词。冠词 a 单独念会变成字母名，改成弱读音素。"""
    if _CJK.search(source_text):
        raise TtsError("source_text 只能是英文，不朗读中文释义")
    parts = source_text.split()
    if not parts:
        raise TtsError("英文文本是空的")
    tokens: list[str] = []
    for part in parts:
        match = _TOKEN.fullmatch(part)
        if match is None:
            raise TtsError(f"无法切分英文单词：{part}")
        prefix, core, suffix = match.groups()
        spoken = "[[a#]]" if core.lower() == "a" else core
        tokens.append(f"{prefix}{spoken}{suffix}")
    return tokens
