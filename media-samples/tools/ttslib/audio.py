"""WAV 拼接、静音和 mp3 编码。时长由样本数决定，不靠引擎自己的词间空隙。"""

from __future__ import annotations

import array
import hashlib
import json
import math
import shutil
import struct
import subprocess
import sys
import wave
from pathlib import Path

from ttslib.errors import TtsError

SAMPLE_RATE = 22050
# 约 -3 dBFS。清楚，但留一点余量，避免再编码时削顶。
SPEECH_PEAK = 0.71
PLACEHOLDER_PEAK = 0.22


def require_binary(name: str) -> str:
    located = shutil.which(name)
    if not located:
        raise TtsError(f"找不到 {name}。生成 mp3 需要本机的 ffmpeg 和 ffprobe，这不是云端密钥。")
    return located


def tool_version(binary: str, flag: str) -> str:
    try:
        completed = subprocess.run(
            [binary, flag],
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
    except OSError:
        return "unknown"
    first = (completed.stdout or "").splitlines()
    if not first:
        return "unknown"
    line = first[0].strip()
    if binary.endswith("espeak-ng") or line.startswith("eSpeak"):
        for token in line.split():
            if token[:1].isdigit():
                return token
        return "unknown"
    if line.startswith("ffmpeg version "):
        return line.split(" Copyright", 1)[0].removeprefix("ffmpeg version ").strip()
    return line


def _pcm_bytes(samples: array.array) -> bytes:
    payload = array.array("h", samples)
    if sys.byteorder != "little":
        payload.byteswap()
    return payload.tobytes()


def _samples_from_bytes(raw: bytes) -> array.array:
    count = len(raw) // 2
    values = struct.unpack("<" + "h" * count, raw[: count * 2])
    return array.array("h", values)


def write_wav(path: Path, samples: array.array, sample_rate: int = SAMPLE_RATE) -> None:
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(sample_rate)
        handle.writeframes(_pcm_bytes(samples))


def read_wav(path: Path) -> tuple[array.array, int]:
    with wave.open(str(path), "rb") as handle:
        if handle.getnchannels() != 1 or handle.getsampwidth() != 2:
            raise TtsError(f"{path.name} 不是单声道 16 位 WAV")
        rate = handle.getframerate()
        raw = handle.readframes(handle.getnframes())
    return _samples_from_bytes(raw), rate


def silence(sample_rate: int, duration_ms: int) -> array.array:
    count = int(round(sample_rate * duration_ms / 1000))
    return array.array("h", bytes(count * 2))


def trim_edges(samples: array.array, sample_rate: int, threshold: int = 200, pad_ms: int = 20) -> array.array:
    """只去掉首尾的数字静音，保留单词内部的塞音空隙。"""
    if len(samples) == 0:
        raise TtsError("合成结果是空的")
    frame = max(1, sample_rate // 100)
    first: int | None = None
    last = 0
    frame_count = (len(samples) + frame - 1) // frame
    for index in range(frame_count):
        chunk = samples[index * frame : (index + 1) * frame]
        peak = max((abs(sample) for sample in chunk), default=0)
        if peak >= threshold:
            if first is None:
                first = index
            last = index
    if first is None:
        raise TtsError("合成结果听不到声音")
    pad = int(sample_rate * pad_ms / 1000)
    start = max(0, first * frame - pad)
    end = min(len(samples), (last + 1) * frame + pad)
    return array.array("h", samples[start:end])


def fade_edges(samples: array.array, sample_rate: int, fade_ms: int = 4) -> array.array:
    count = min(int(sample_rate * fade_ms / 1000), max(0, len(samples) // 4))
    if count <= 0:
        return samples
    faded = array.array("h", samples)
    for index in range(count):
        gain = index / count
        faded[index] = int(faded[index] * gain)
        faded[-1 - index] = int(faded[-1 - index] * gain)
    return faded


def normalize_peak(samples: array.array, target: float) -> array.array:
    peak = max((abs(sample) for sample in samples), default=0)
    if peak <= 0:
        raise TtsError("音频只有静音")
    gain = min(3.0, (target * 32767) / peak)
    normalized = array.array("h")
    for sample in samples:
        value = int(sample * gain)
        if value > 32767:
            value = 32767
        elif value < -32768:
            value = -32768
        normalized.append(value)
    return normalized


def pace(
    segments: list[array.array],
    sample_rate: int,
    lead_ms: int,
    gap_ms: int,
    trail_ms: int,
) -> array.array:
    """开头留开口前静音，词与词之间留词间静音，结尾留跟读用的句尾静音。"""
    pieces: list[array.array] = [silence(sample_rate, lead_ms)]
    for index, segment in enumerate(segments):
        if index:
            pieces.append(silence(sample_rate, gap_ms))
        pieces.append(segment)
    pieces.append(silence(sample_rate, trail_ms))
    paced = array.array("h")
    for piece in pieces:
        paced.extend(piece)
    return paced


def tone(sample_rate: int, duration_ms: int, frequency: float, amplitude: float) -> array.array:
    count = int(round(sample_rate * duration_ms / 1000))
    level = int(32767 * amplitude)
    samples = array.array("h")
    for index in range(count):
        samples.append(int(level * math.sin(2 * math.pi * frequency * index / sample_rate)))
    return fade_edges(samples, sample_rate, 8)


def placeholder_chime(sample_rate: int = SAMPLE_RATE) -> array.array:
    """两声短促的音，故意不像任何一个单词。"""
    chime = tone(sample_rate, 160, 392, 0.45)
    chime.extend(silence(sample_rate, 40))
    chime.extend(tone(sample_rate, 180, 494, 0.45))
    return chime


def silence_runs(samples: array.array, sample_rate: int, threshold: int = 300) -> list[tuple[bool, int]]:
    frame = max(1, sample_rate // 100)
    runs: list[tuple[bool, int]] = []
    current: bool | None = None
    length = 0
    for offset in range(0, len(samples), frame):
        chunk = samples[offset : offset + frame]
        silent = max((abs(sample) for sample in chunk), default=0) < threshold
        if current is None:
            current = silent
            length = 1
        elif silent == current:
            length += 1
        else:
            runs.append((current, length * 10))
            current = silent
            length = 1
    if current is not None:
        runs.append((current, length * 10))
    return runs


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(65536), b""):
            digest.update(block)
    return digest.hexdigest()


def _run_ffmpeg(arguments: list[str], secrets: list[str]) -> None:
    completed = subprocess.run(
        arguments,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if completed.returncode != 0:
        detail = completed.stderr.decode("utf-8", "replace")[-400:]
        raise TtsError(f"ffmpeg 失败：{detail}", secrets)


def transcode_to_wav(source: Path, dest: Path, secrets: list[str]) -> None:
    _run_ffmpeg(
        [
            "ffmpeg",
            "-nostdin",
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(source),
            "-ac",
            "1",
            "-ar",
            str(SAMPLE_RATE),
            "-c:a",
            "pcm_s16le",
            str(dest),
        ],
        secrets,
    )


def encode_mp3(wav_path: Path, mp3_path: Path, *, title: str, comment: str, secrets: list[str]) -> None:
    _run_ffmpeg(
        [
            "ffmpeg",
            "-nostdin",
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(wav_path),
            "-ac",
            "1",
            "-ar",
            str(SAMPLE_RATE),
            "-c:a",
            "libmp3lame",
            "-b:a",
            "96k",
            "-metadata",
            f"title={title}",
            "-metadata",
            f"comment={comment}",
            "-metadata",
            "album=Guoyu English animals-001 demonstration",
            str(mp3_path),
        ],
        secrets,
    )


def probe_mp3(path: Path) -> dict[str, object]:
    completed = subprocess.run(
        [
            "ffprobe",
            "-v",
            "error",
            "-print_format",
            "json",
            "-show_format",
            "-show_streams",
            str(path),
        ],
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if completed.returncode != 0:
        detail = (completed.stderr or "").strip().splitlines()
        tail = detail[-1] if detail else "未知错误"
        raise TtsError(f"ffprobe 无法读取 {path.name}：{tail}")
    try:
        payload = json.loads(completed.stdout)
    except json.JSONDecodeError as exc:
        raise TtsError(f"ffprobe 没有返回 {path.name} 的音频信息") from exc
    streams = [item for item in payload.get("streams", []) if item.get("codec_type") == "audio"]
    if not streams:
        raise TtsError(f"{path.name} 不是音频")
    codec = streams[0].get("codec_name")
    if codec != "mp3":
        raise TtsError(f"{path.name} 的编码是 {codec}，不是 mp3")
    try:
        duration_ms = int(round(float(payload["format"]["duration"]) * 1000))
        size_bytes = int(payload["format"]["size"])
    except (KeyError, TypeError, ValueError) as exc:
        raise TtsError(f"{path.name} 缺少时长或大小") from exc
    if duration_ms <= 0 or size_bytes <= 0:
        raise TtsError(f"{path.name} 是空音频")
    return {"duration_ms": duration_ms, "size_bytes": size_bytes, "codec": codec}


def file_declares_audio(path: Path) -> bool:
    """文件头是 ID3 或 MP3 帧同步，而不是空文件或文本。"""
    header = path.read_bytes()[:4]
    if len(header) < 2:
        return False
    if header.startswith(b"ID3"):
        return True
    return header[0] == 0xFF and (header[1] & 0xE0) == 0xE0
