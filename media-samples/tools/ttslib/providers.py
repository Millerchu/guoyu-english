"""三种提供方。新增厂商时加一个渲染函数，不要把某一家 SDK 写进仓库。"""

from __future__ import annotations

import array
import hashlib
import json
import shutil
import subprocess
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

from ttslib.audio import (
    PLACEHOLDER_PEAK,
    SAMPLE_RATE,
    SPEECH_PEAK,
    fade_edges,
    normalize_peak,
    pace,
    placeholder_chime,
    read_wav,
    tool_version,
    transcode_to_wav,
    trim_edges,
    write_wav,
)
from ttslib.catalog import LessonLine, speech_tokens
from ttslib.config import TtsConfig
from ttslib.errors import TtsError

# espeak-ng 的 -s 默认是 175 wpm。0.80 倍速对应大约 140。
ESPEAK_SPEED_BASIS_WPM = 175
# 默认男声再把音高放到 42（默认 50）。clear-warm：清楚，但不尖。
ESPEAK_PITCH = 42
ESPEAK_AMPLITUDE = 100
DEFAULT_OFFLINE_VOICE = "en-us+m3"
SOURCE_BY_PROVIDER = {
    "offline": "OFFLINE_TTS",
    "http": "AI_GENERATED",
    "placeholder": "PLACEHOLDER",
}
WARNINGS = {
    "OFFLINE_TTS": "离线合成音，音质不像人声，不是真人录音，也不是正式教材录音。",
    "AI_GENERATED": "外部 HTTP TTS 生成的音频。本工具没有核验该服务的授权。",
    "PLACEHOLDER": "占位音，不是语音，不能当作教材音频。",
}
MAX_HTTP_BYTES = 2_000_000


@dataclass
class SynthResult:
    source: str
    tool: str
    voice: str
    voice_applied: bool
    engine_parameters: dict[str, object]
    warning: str
    pauses_applied_by: str
    pcm: array.array | None = None
    external_audio: Path | None = None


def secret_values(env: dict[str, str]) -> list[str]:
    values: list[str] = []
    raw = (env.get("TTS_API_KEY") or "").strip()
    if len(raw) >= 4:
        values.append(raw)
    return values


def select_provider(env: dict[str, str], *, offline_available: bool) -> str:
    provider = (env.get("TTS_PROVIDER") or "auto").strip().lower()
    key = (env.get("TTS_API_KEY") or "").strip()
    base = (env.get("TTS_API_BASE") or "").strip()
    known = {"auto", "offline", "http", "placeholder"}
    if provider not in known:
        raise TtsError(
            f"未知的 TTS_PROVIDER={provider!r}。可选 auto、offline、http、placeholder。"
            "本工具不内置厂商 SDK。"
        )
    if provider == "auto":
        if key:
            if not base:
                raise TtsError("已设置 TTS_API_KEY，但没有 TTS_API_BASE。不会猜测厂商地址。")
            return "http"
        if offline_available:
            return "offline"
        return "placeholder"
    if provider == "http":
        if not key or not base:
            raise TtsError("TTS_PROVIDER=http 时必须同时设置 TTS_API_BASE 和 TTS_API_KEY。")
        return "http"
    if provider == "offline" and not offline_available:
        raise TtsError("TTS_PROVIDER=offline，但找不到 espeak-ng。")
    return provider


def resolve_voice(config_voice: str, env: dict[str, str]) -> str:
    from_env = (env.get("TTS_VOICE") or "").strip()
    if from_env:
        return from_env
    return config_voice.strip()


def _assert_voice(voice: str) -> None:
    if any(ord(char) < 32 for char in voice):
        raise TtsError("音色名称不能换行")


def _speed_wpm(speech_rate: float) -> int:
    return int(round(ESPEAK_SPEED_BASIS_WPM * speech_rate))


def _synthesize_token(token: str, voice: str, speed: int, dest: Path) -> array.array:
    # 单个字母 I 若不带换行，espeak-ng 会写出一段空音频。
    completed = subprocess.run(
        [
            "espeak-ng",
            "-v",
            voice,
            "-s",
            str(speed),
            "-p",
            str(ESPEAK_PITCH),
            "-a",
            str(ESPEAK_AMPLITUDE),
            "-z",
            "-w",
            str(dest),
            "--stdin",
        ],
        input=f"{token}\n".encode("utf-8"),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if completed.returncode != 0 or not dest.exists():
        detail = completed.stderr.decode("utf-8", "replace")[:300]
        raise TtsError(f"espeak-ng 无法朗读：{detail or token}")
    samples, rate = read_wav(dest)
    if rate != SAMPLE_RATE:
        raise TtsError(f"espeak-ng 采样率是 {rate}，期望 {SAMPLE_RATE}")
    trimmed = trim_edges(samples, rate)
    minimum = int(rate * 0.18)
    if len(trimmed) < minimum:
        raise TtsError(f"合成结果过短，可能被截断：{token}")
    return fade_edges(trimmed, rate, 4)


def render_offline(line: LessonLine, config: TtsConfig, voice: str, work: Path) -> SynthResult:
    _assert_voice(voice)
    chosen = voice or DEFAULT_OFFLINE_VOICE
    speed = _speed_wpm(config.speech_rate)
    tokens = speech_tokens(line.source_text)
    segments: list[array.array] = []
    for index, token in enumerate(tokens):
        segments.append(_synthesize_token(token, chosen, speed, work / f"{index}.wav"))
    paced = pace(
        segments,
        SAMPLE_RATE,
        config.pause_after_prompt_ms,
        config.pause_between_words_ms,
        config.pause_between_sentences_ms,
    )
    version = tool_version("espeak-ng", "--version")
    ffmpeg_version = tool_version("ffmpeg", "-version")
    return SynthResult(
        source="OFFLINE_TTS",
        tool=f"espeak-ng {version} + ffmpeg {ffmpeg_version}",
        voice=chosen,
        voice_applied=True,
        warning=WARNINGS["OFFLINE_TTS"],
        pauses_applied_by="tool",
        pcm=normalize_peak(paced, SPEECH_PEAK),
        engine_parameters={
            "adapter": "espeak-ng",
            "speech_rate": config.speech_rate,
            "speed_basis_wpm": ESPEAK_SPEED_BASIS_WPM,
            "speed_wpm": speed,
            "pitch": ESPEAK_PITCH,
            "amplitude": ESPEAK_AMPLITUDE,
            "voice": chosen,
            "sample_rate_hz": SAMPLE_RATE,
            "espeak_tokens": tokens,
            "pauses_applied_by": "tool",
            "speech_rate_applied": True,
        },
    )


def render_placeholder(line: LessonLine, config: TtsConfig, voice: str, work: Path) -> SynthResult:
    del work
    _assert_voice(voice)
    tokens = speech_tokens(line.source_text)
    segments = [placeholder_chime() for _ in tokens]
    paced = pace(
        segments,
        SAMPLE_RATE,
        config.pause_after_prompt_ms,
        config.pause_between_words_ms,
        config.pause_between_sentences_ms,
    )
    ffmpeg_version = tool_version("ffmpeg", "-version")
    return SynthResult(
        source="PLACEHOLDER",
        tool=f"placeholder-tone + ffmpeg {ffmpeg_version}",
        voice=voice,
        voice_applied=False,
        warning=WARNINGS["PLACEHOLDER"],
        pauses_applied_by="tool",
        pcm=normalize_peak(paced, PLACEHOLDER_PEAK),
        engine_parameters={
            "adapter": "placeholder-tone",
            "speech_rate": config.speech_rate,
            "speech_rate_applied": False,
            "tone_hz": [392, 494],
            "voice_applied": False,
            "pauses_applied_by": "tool",
            "note": "没有调用语音引擎。音调不表示单词。",
        },
    )


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):  # type: ignore[no-untyped-def]
        return None


def _validated_base(base: str) -> urllib.parse.SplitResult:
    parsed = urllib.parse.urlsplit(base.strip())
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        raise TtsError("TTS_API_BASE 必须是 http 或 https 地址")
    if parsed.username or parsed.password:
        raise TtsError("不要把密钥放进 TTS_API_BASE，请使用 TTS_API_KEY")
    return parsed


def api_base_fingerprint(base: str) -> str:
    """主机和路径变了，元数据就要变。不记录查询串，避免把密钥写进去。"""
    parsed = _validated_base(base)
    identity = urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", ""))
    return hashlib.sha256(identity.encode("utf-8")).hexdigest()[:16]


def render_http(
    line: LessonLine,
    config: TtsConfig,
    voice: str,
    work: Path,
    env: dict[str, str],
    secrets: list[str],
) -> SynthResult:
    _assert_voice(voice)
    parsed = _validated_base(env.get("TTS_API_BASE") or "")
    key = (env.get("TTS_API_KEY") or "").strip()
    if not key:
        raise TtsError("TTS HTTP 适配器缺少 TTS_API_KEY")
    payload = {
        "text": line.source_text,
        "speechRate": config.speech_rate,
        "voice": voice,
        "style": config.style,
        "pauseBetweenWordsMs": config.pause_between_words_ms,
        "pauseBetweenSentencesMs": config.pause_between_sentences_ms,
        "pauseAfterPromptMs": config.pause_after_prompt_ms,
    }
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        urllib.parse.urlunsplit(parsed),
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
            "Accept": "audio/mpeg, audio/wav",
            "User-Agent": "guoyu-english-tts/1",
        },
    )
    opener = urllib.request.build_opener(_NoRedirect)
    try:
        with opener.open(request, timeout=30) as response:
            status = getattr(response, "status", 200)
            content_type = response.headers.get("Content-Type", "")
            raw = response.read(MAX_HTTP_BYTES + 1)
    except urllib.error.HTTPError as exc:
        code = exc.code
        exc.close()
        if code in {301, 302, 303, 307, 308}:
            raise TtsError(f"TTS HTTP {code}，通用适配器不跟随重定向。", secrets) from None
        raise TtsError(f"TTS HTTP {code}。", secrets) from None
    except urllib.error.URLError:
        raise TtsError("TTS HTTP 请求失败。", secrets) from None
    except TimeoutError:
        raise TtsError("TTS HTTP 超时。", secrets) from None
    if status != 200:
        raise TtsError(f"TTS HTTP {status}。", secrets)
    if len(raw) > MAX_HTTP_BYTES:
        raise TtsError("TTS 响应超过 2MB，已拒绝。", secrets)
    if len(raw) < 44:
        raise TtsError("TTS 响应不是音频。", secrets)
    mime = content_type.split(";", 1)[0].strip().lower()
    sniffed = _sniff_audio(raw)
    if mime not in {"audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream", ""}:
        raise TtsError("TTS 响应不是音频字节。请为该厂商另写适配器，不要改这个通用适配器。", secrets)
    if sniffed is None:
        raise TtsError("TTS 响应不是 WAV 或 MP3。", secrets)
    external = work / f"remote.{sniffed}"
    external.write_bytes(raw)
    ffmpeg_version = tool_version("ffmpeg", "-version")
    return SynthResult(
        source="AI_GENERATED",
        tool=f"generic-http + ffmpeg {ffmpeg_version}",
        voice=voice,
        voice_applied=bool(voice),
        warning=WARNINGS["AI_GENERATED"],
        pauses_applied_by="remote",
        external_audio=external,
        engine_parameters={
            "adapter": "generic-http",
            "speech_rate": config.speech_rate,
            "speech_rate_applied": True,
            "style": config.style,
            "voice": voice,
            "api_base_host": parsed.hostname,
            "api_base_fingerprint": api_base_fingerprint(env.get("TTS_API_BASE") or ""),
            "response": sniffed,
            "pauses_applied_by": "remote",
            "note": "请求体是通用 JSON，不是某一家 SDK。",
        },
    )


def _sniff_audio(raw: bytes) -> str | None:
    if raw.startswith(b"RIFF") and b"WAVE" in raw[:16]:
        return "wav"
    if raw.startswith(b"ID3") or (raw[0] == 0xFF and (raw[1] & 0xE0) == 0xE0):
        return "mp3"
    return None


def render_line(
    provider: str,
    line: LessonLine,
    config: TtsConfig,
    voice: str,
    work: Path,
    env: dict[str, str],
    secrets: list[str],
) -> SynthResult:
    work.mkdir(parents=True, exist_ok=True)
    if provider == "offline":
        if shutil.which("espeak-ng") is None:
            raise TtsError("找不到 espeak-ng。")
        return render_offline(line, config, voice, work)
    if provider == "placeholder":
        return render_placeholder(line, config, voice, work)
    if provider == "http":
        return render_http(line, config, voice, work, env, secrets)
    raise TtsError(f"没有名为 {provider} 的提供方")


def materialize_wav(result: SynthResult, dest: Path, secrets: list[str]) -> None:
    if result.pcm is not None:
        write_wav(dest, result.pcm, SAMPLE_RATE)
        return
    if result.external_audio is None:
        raise TtsError("提供方没有返回音频")
    transcode_to_wav(result.external_audio, dest, secrets)
    # 确认转码后真的有采样，避免把空响应写成 mp3。
    samples, _rate = read_wav(dest)
    if max((abs(sample) for sample in samples), default=0) <= 0:
        raise TtsError("外部 TTS 转码后是静音")
