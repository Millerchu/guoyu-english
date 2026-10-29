"""生成十段音频，并写出清单。密钥只存在于当次请求，不进入文件。"""

from __future__ import annotations

import json
import shutil
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from ttslib.audio import encode_mp3, file_declares_audio, probe_mp3, require_binary, sha256_file
from ttslib.catalog import LESSON_LINES, LessonLine
from ttslib.config import TtsConfig, load_config
from ttslib.errors import TtsError
from ttslib.manifest import render_manifest
from ttslib.providers import (
    SOURCE_BY_PROVIDER,
    SynthResult,
    materialize_wav,
    render_line,
    resolve_voice,
    secret_values,
    select_provider,
)

MEDIA_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = MEDIA_ROOT.parent
LICENSE_TEXT = {
    "OFFLINE_TTS": "合成示范音。生成程序 eSpeak NG 为 GPL-3.0，本仓库不包含其源码。音频是程序输出，不是真人录音，没有单独的教材授权。",
    "AI_GENERATED": "外部 HTTP 服务返回的音频。授权以该服务条款为准，本工具没有自动取得授权。",
    "PLACEHOLDER": "程序生成的占位音，不是语音，也不是第三方素材。不能当作教材音频。",
}
ATTRIBUTION_TEXT = {
    "OFFLINE_TTS": "不要把合成音写成真人录音或官方教材音。工具名称见 tool。",
    "AI_GENERATED": "按该服务自己的署名要求处理。这里没有署名许可。",
    "PLACEHOLDER": "不需要署名。禁止标成正式教材音。",
}


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _existing_source(out_dir: Path) -> str | None:
    path = out_dir / "metadata.json"
    if not path.exists():
        return None
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise TtsError("已有 metadata.json 无法解析，没有覆盖它。") from exc
    source = document.get("source")
    return source if isinstance(source, str) else None


def _asset(
    line: LessonLine,
    config: TtsConfig,
    result: SynthResult,
    generated_at: str,
    mp3_path: Path,
    probed: dict[str, object],
) -> dict[str, Any]:
    size_bytes = mp3_path.stat().st_size
    if size_bytes != probed["size_bytes"]:
        raise TtsError(f"{line.filename} 的探测大小和文件大小不一致")
    if size_bytes < 1000:
        raise TtsError(f"{line.filename} 太小，不像一段音频")
    if not file_declares_audio(mp3_path):
        raise TtsError(f"{line.filename} 的文件头不是 mp3")
    return {
        "storage_key": line.storage_key,
        "kind": line.kind,
        "lexeme_id": line.lexeme_id,
        "sentence_id": line.sentence_id,
        "source_text": line.source_text,
        "source": result.source,
        "speech_rate": config.speech_rate,
        "style": config.style,
        "voice": result.voice,
        "voice_applied": result.voice_applied,
        "pause_between_words_ms": config.pause_between_words_ms,
        "pause_between_sentences_ms": config.pause_between_sentences_ms,
        "pause_after_prompt_ms": config.pause_after_prompt_ms,
        "tool": result.tool,
        "generated_at": generated_at,
        "mime": "audio/mpeg",
        "size_bytes": size_bytes,
        "sha256": sha256_file(mp3_path),
        "duration_ms": probed["duration_ms"],
        "engine_parameters": result.engine_parameters,
        "license": LICENSE_TEXT[result.source],
        "attribution": ATTRIBUTION_TEXT[result.source],
        "credential": "无。生成时不把 API Key 写入清单。",
        "version": 1,
        "purpose": line.purpose,
        "warning": result.warning,
    }


def _document(
    config: TtsConfig,
    config_path: Path,
    provider: str,
    voice_requested: str,
    generated_at: str,
    assets: list[dict[str, Any]],
) -> dict[str, Any]:
    first = assets[0]
    try:
        config_display = config_path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        config_display = config_path.name
    shared_engine = {
        key: value
        for key, value in first["engine_parameters"].items()
        if key != "espeak_tokens"
    }
    return {
        "manifest_version": 1,
        "lesson_id": "animals-001",
        "lesson_version": 1,
        "generated_at": generated_at,
        "config_path": config_display,
        "provider": provider,
        "source": SOURCE_BY_PROVIDER[provider],
        "tool": first["tool"],
        "voice": first["voice"],
        "voice_requested": voice_requested,
        "speech_rate": config.speech_rate,
        "style": config.style,
        "pause_between_words_ms": config.pause_between_words_ms,
        "pause_between_sentences_ms": config.pause_between_sentences_ms,
        "pause_after_prompt_ms": config.pause_after_prompt_ms,
        "pause_placement": {
            "pause_after_prompt_ms": "leading-silence",
            "pause_between_words_ms": "between-words",
            "pause_between_sentences_ms": "trailing-silence-for-repeat",
        },
        "engine_parameters": shared_engine,
        "warning": first["warning"],
        "config": config.as_file_dict(),
        "assets": assets,
    }


def _assert_clean(paths: list[Path], secrets: list[str]) -> None:
    for path in paths:
        data = path.read_bytes()
        for secret in secrets:
            if len(secret) >= 8 and secret.encode("utf-8") in data:
                raise TtsError("输出里出现了 API Key，已丢弃这一批文件。")


def generate_assets(
    config_path: Path,
    out_dir: Path,
    env: dict[str, str],
    *,
    allow_placeholder: bool = False,
    offline_available: bool | None = None,
    generated_at: str | None = None,
) -> dict[str, Any]:
    secrets = secret_values(env)
    config = load_config(config_path)
    if offline_available is None:
        offline_available = shutil.which("espeak-ng") is not None
    provider = select_provider(env, offline_available=offline_available)
    require_binary("ffmpeg")
    require_binary("ffprobe")
    out_dir = Path(out_dir)
    if provider == "placeholder" and not allow_placeholder:
        previous = _existing_source(out_dir)
        if previous in {"OFFLINE_TTS", "AI_GENERATED"}:
            raise TtsError(
                "当前没有可用的语音引擎，继续会用占位音覆盖已有的示范语音。"
                "请安装 espeak-ng 后重试。若确实要写占位音，加上 --allow-placeholder，"
                "或把 TTS_PROVIDER 明确设为 placeholder。"
            )
    voice_requested = resolve_voice(config.voice, env)
    stamp = generated_at or utc_now()
    staging_files: list[Path] = []
    with tempfile.TemporaryDirectory(prefix="guoyu-tts-") as temporary:
        root = Path(temporary)
        staged = root / "out"
        staged.mkdir()
        assets: list[dict[str, Any]] = []
        for line in LESSON_LINES:
            result = render_line(
                provider,
                line,
                config,
                voice_requested,
                root / line.filename,
                env,
                secrets,
            )
            wav_path = root / f"{line.filename}.wav"
            mp3_path = staged / line.filename
            materialize_wav(result, wav_path, secrets)
            encode_mp3(
                wav_path,
                mp3_path,
                title=line.source_text,
                comment=result.warning,
                secrets=secrets,
            )
            assets.append(_asset(line, config, result, stamp, mp3_path, probe_mp3(mp3_path)))
        document = _document(config, config_path, provider, voice_requested, stamp, assets)
        metadata_path = staged / "metadata.json"
        manifest_path = staged / "MANIFEST.md"
        metadata_path.write_text(json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        manifest_path.write_text(render_manifest(document), encoding="utf-8")
        staging_files = [path for path in staged.iterdir() if path.is_file()]
        _assert_clean(staging_files, secrets)
        rendered = render_manifest(json.loads(metadata_path.read_text(encoding="utf-8")))
        if rendered != manifest_path.read_text(encoding="utf-8"):
            raise TtsError("清单和 metadata.json 不一致，没有发布这一批文件。")
        out_dir.mkdir(parents=True, exist_ok=True)
        for item in staging_files:
            destination = out_dir / item.name
            if destination.exists():
                destination.unlink()
            shutil.move(str(item), str(destination))
    return document


def check_assets(out_dir: Path) -> dict[str, Any]:
    out_dir = Path(out_dir)
    metadata_path = out_dir / "metadata.json"
    manifest_path = out_dir / "MANIFEST.md"
    if not metadata_path.exists() or not manifest_path.exists():
        raise TtsError("缺少 metadata.json 或 MANIFEST.md")
    try:
        document = json.loads(metadata_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise TtsError("metadata.json 无法解析") from exc
    if render_manifest(document) != manifest_path.read_text(encoding="utf-8"):
        raise TtsError("MANIFEST.md 与 metadata.json 不一致")
    assets = document.get("assets")
    if not isinstance(assets, list) or len(assets) != len(LESSON_LINES):
        raise TtsError("音频数量不是十段")
    require_binary("ffprobe")
    for line, asset in zip(LESSON_LINES, assets, strict=True):
        if not isinstance(asset, dict):
            raise TtsError(f"{line.filename} 的元数据不是对象")
        if asset.get("storage_key") != line.storage_key or asset.get("source_text") != line.source_text:
            raise TtsError(f"{line.filename} 的路径或原文和契约不一致")
        if any("\u4e00" <= char <= "\u9fff" for char in str(asset.get("source_text", ""))):
            raise TtsError(f"{line.filename} 的 source_text 含有中文")
        for field in (
            "speech_rate",
            "pause_between_words_ms",
            "pause_between_sentences_ms",
            "pause_after_prompt_ms",
            "source_text",
            "tool",
            "generated_at",
            "sha256",
            "engine_parameters",
        ):
            if asset.get(field) in (None, ""):
                raise TtsError(f"{line.filename} 缺少 {field}")
        if asset.get("source") not in {"OFFLINE_TTS", "AI_GENERATED", "PLACEHOLDER"}:
            raise TtsError(f"{line.filename} 的来源不在契约枚举里")
        path = out_dir / line.filename
        if not path.exists():
            raise TtsError(f"缺少 {line.filename}")
        if sha256_file(path) != asset["sha256"]:
            raise TtsError(f"{line.filename} 的 sha256 和清单不一致")
        if path.stat().st_size != asset["size_bytes"]:
            raise TtsError(f"{line.filename} 的字节数和清单不一致")
        if not file_declares_audio(path):
            raise TtsError(f"{line.filename} 的文件头不是 mp3")
        probed = probe_mp3(path)
        if probed["size_bytes"] != asset["size_bytes"]:
            raise TtsError(f"{line.filename} 探测到的大小和清单不一致")
        if int(probed["duration_ms"]) <= 0:
            raise TtsError(f"{line.filename} 时长为 0")
    return document


def summary_lines(document: dict[str, Any]) -> list[str]:
    rate = float(document["speech_rate"])
    engine = document.get("engine_parameters") or {}
    speed = engine.get("speed_wpm")
    speed_text = f"，引擎 {speed} wpm" if speed else ""
    voice = document.get("voice") or "（由提供方默认）"
    return [
        f"提供方：{document['provider']}",
        f"来源：{document['source']}",
        f"音色：{voice}",
        f"语速：{rate:.2f}{speed_text}",
        f"文件：{len(document['assets'])}",
        document["warning"],
    ]
