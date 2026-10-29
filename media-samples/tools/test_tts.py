#!/usr/bin/env python3
"""S05 验收：无密钥能生成十段音频，元数据含语速、停顿、原文、工具和生成时间。"""

from __future__ import annotations

import array
import json
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import unittest
import wave
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from ttslib.audio import file_declares_audio, read_wav, silence_runs, transcode_to_wav
from ttslib.catalog import LESSON_LINES, speech_tokens
from ttslib.config import load_config
from ttslib.errors import TtsError
from ttslib.generate import check_assets, generate_assets
from ttslib.providers import api_base_fingerprint, select_provider

MEDIA_ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = MEDIA_ROOT / "tts" / "tts.config.yaml"
AUDIO_DIR = MEDIA_ROOT / "audio" / "animals"
CONTRACT_LINES = (
    ("lexeme-cat", "cat", "audio/animals/cat-v1.mp3"),
    ("lexeme-dog", "dog", "audio/animals/dog-v1.mp3"),
    ("lexeme-rabbit", "rabbit", "audio/animals/rabbit-v1.mp3"),
    ("lexeme-bird", "bird", "audio/animals/bird-v1.mp3"),
    ("lexeme-fish", "fish", "audio/animals/fish-v1.mp3"),
    ("sent-i-see-a-cat", "I see a cat.", "audio/animals/i-see-a-cat-v1.mp3"),
    ("sent-i-see-a-dog", "I see a dog.", "audio/animals/i-see-a-dog-v1.mp3"),
    ("sent-i-like-rabbits", "I like rabbits.", "audio/animals/i-like-rabbits-v1.mp3"),
    ("sent-its-a-dog", "It's a dog.", "audio/animals/its-a-dog-v1.mp3"),
    ("sent-whats-this", "What's this?", "audio/animals/whats-this-v1.mp3"),
)
SECRET = "local-test-key-do-not-store"


def write_config(path: Path, speech_rate: str = "0.80") -> None:
    path.write_text(
        "\n".join(
            [
                f"speechRate: {speech_rate}",
                'voice: ""',
                "style: clear-warm",
                "pauseBetweenWordsMs: 600",
                "pauseBetweenSentencesMs: 900",
                "pauseAfterPromptMs: 400",
                "",
            ]
        ),
        encoding="utf-8",
    )


def peak_of(mp3: Path) -> float:
    with tempfile.TemporaryDirectory() as temporary:
        wav = Path(temporary) / "decoded.wav"
        transcode_to_wav(mp3, wav, [])
        samples, _rate = read_wav(wav)
    return max((abs(sample) for sample in samples), default=0) / 32768


class CatalogAndConfigTest(unittest.TestCase):
    def test_catalog_matches_contract(self) -> None:
        self.assertEqual(len(LESSON_LINES), 10)
        for line, expected in zip(LESSON_LINES, CONTRACT_LINES, strict=True):
            item_id, text, storage_key = expected
            self.assertEqual(line.item_id, item_id)
            self.assertEqual(line.source_text, text)
            self.assertEqual(line.storage_key, storage_key)
            self.assertFalse(any("\u4e00" <= char <= "\u9fff" for char in line.source_text))

    def test_article_a_is_marked_as_schwa(self) -> None:
        self.assertEqual(speech_tokens("I see a cat."), ["I", "see", "[[a#]]", "cat."])
        self.assertEqual(speech_tokens("It's a dog."), ["It's", "[[a#]]", "dog."])
        self.assertEqual(speech_tokens("What's this?"), ["What's", "this?"])
        self.assertEqual(speech_tokens("cat"), ["cat"])

    def test_committed_config_is_the_contract_default(self) -> None:
        config = load_config(CONFIG_PATH)
        self.assertEqual(config.speech_rate, 0.8)
        self.assertEqual(config.voice, "")
        self.assertEqual(config.style, "clear-warm")
        self.assertEqual(config.pause_between_words_ms, 600)
        self.assertEqual(config.pause_between_sentences_ms, 900)
        self.assertEqual(config.pause_after_prompt_ms, 400)
        raw = CONFIG_PATH.read_text(encoding="utf-8")
        self.assertNotIn(SECRET, raw)
        self.assertNotIn("TTS_API_KEY=", raw)

    def test_config_rejects_unknown_fields(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "tts.config.yaml"
            path.write_text(CONFIG_PATH.read_text(encoding="utf-8") + "vendor: acme\n", encoding="utf-8")
            with self.assertRaises(TtsError):
                load_config(path)

    def test_provider_selection(self) -> None:
        self.assertEqual(select_provider({}, offline_available=True), "offline")
        self.assertEqual(select_provider({}, offline_available=False), "placeholder")
        self.assertEqual(
            select_provider({"TTS_API_KEY": SECRET, "TTS_API_BASE": "https://tts.example/speech"}, offline_available=True),
            "http",
        )
        with self.assertRaises(TtsError):
            select_provider({"TTS_API_KEY": SECRET}, offline_available=True)
        with self.assertRaises(TtsError):
            select_provider({"TTS_PROVIDER": "vendor-sdk"}, offline_available=True)
        with self.assertRaises(TtsError):
            select_provider({"TTS_PROVIDER": "http"}, offline_available=True)
        with self.assertRaises(TtsError):
            select_provider({"TTS_PROVIDER": "offline"}, offline_available=False)

    def test_api_base_fingerprint_changes_without_storing_query(self) -> None:
        first = api_base_fingerprint("https://tts.example/v1/speech")
        second = api_base_fingerprint("https://tts.example/v2/speech")
        leaked = api_base_fingerprint("https://tts.example/v1/speech?token=local-test-key-do-not-store")
        self.assertNotEqual(first, second)
        self.assertEqual(first, leaked)
        self.assertNotIn(SECRET, first)


class PlaceholderEnvTest(unittest.TestCase):
    def test_voice_and_provider_change_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            config = root / "tts.config.yaml"
            write_config(config)
            first_dir = root / "a"
            second_dir = root / "b"
            slower = root / "slower.yaml"
            write_config(slower, "0.75")
            first = generate_assets(
                config,
                first_dir,
                {"TTS_PROVIDER": "placeholder", "TTS_VOICE": "warm-a", "TTS_API_KEY": SECRET},
                allow_placeholder=True,
                offline_available=False,
                generated_at="2026-10-29T01:00:00Z",
            )
            second = generate_assets(
                config,
                second_dir,
                {"TTS_PROVIDER": "placeholder", "TTS_VOICE": "warm-b"},
                allow_placeholder=True,
                offline_available=False,
                generated_at="2026-10-29T01:00:00Z",
            )
            slower_doc = generate_assets(
                slower,
                root / "c",
                {"TTS_PROVIDER": "placeholder"},
                allow_placeholder=True,
                offline_available=False,
                generated_at="2026-10-29T01:00:00Z",
            )
            self.assertEqual(first["source"], "PLACEHOLDER")
            self.assertEqual(first["voice"], "warm-a")
            self.assertEqual(second["voice"], "warm-b")
            self.assertNotEqual(first["voice"], second["voice"])
            self.assertEqual(first["speech_rate"], 0.8)
            self.assertEqual(slower_doc["speech_rate"], 0.75)
            self.assertEqual(first["assets"][0]["pause_between_words_ms"], 600)
            self.assertEqual(first["assets"][0]["pause_between_sentences_ms"], 900)
            self.assertEqual(first["assets"][0]["pause_after_prompt_ms"], 400)
            self.assertIn("placeholder-tone", first["tool"])
            self.assertTrue(first["generated_at"])
            self.assertIn("占位音", first["warning"])
            self.assertFalse(first["engine_parameters"]["speech_rate_applied"])
            blob = (first_dir / "metadata.json").read_text(encoding="utf-8")
            blob += (first_dir / "MANIFEST.md").read_text(encoding="utf-8")
            self.assertNotIn(SECRET, blob)
            for asset in first["assets"]:
                path = first_dir / Path(asset["storage_key"]).name
                self.assertTrue(file_declares_audio(path))
                self.assertGreater(path.stat().st_size, 1000)
                self.assertGreater(asset["duration_ms"], 1000)
                self.assertEqual(asset["source_text"], asset["source_text"].encode("ascii").decode("ascii"))
            self.assertLess(peak_of(first_dir / "cat-v1.mp3"), 0.45)
            self.assertGreater(peak_of(first_dir / "cat-v1.mp3"), 0.05)
            check_assets(first_dir)

    def test_auto_without_engine_does_not_overwrite_speech(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            out = Path(temporary)
            (out / "metadata.json").write_text(json.dumps({"source": "OFFLINE_TTS"}), encoding="utf-8")
            with self.assertRaises(TtsError):
                generate_assets(
                    CONFIG_PATH,
                    out,
                    {},
                    offline_available=False,
                )
            self.assertEqual(json.loads((out / "metadata.json").read_text(encoding="utf-8"))["source"], "OFFLINE_TTS")


class HttpAdapterTest(unittest.TestCase):
    def test_http_adapter_records_voice_and_hides_key(self) -> None:
        wav_bytes = _tiny_wav()
        captured: list[tuple[str | None, bytes]] = []

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self) -> None:  # noqa: N802
                length = int(self.headers.get("Content-Length", "0"))
                body = self.rfile.read(length)
                captured.append((self.headers.get("Authorization"), body))
                payload = wav_bytes
                self.send_response(200)
                self.send_header("Content-Type", "audio/wav")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def log_message(self, fmt: str, *args: object) -> None:
                return

        server = HTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        port = server.server_address[1]
        base = f"http://127.0.0.1:{port}/v1/speech"
        try:
            with tempfile.TemporaryDirectory() as temporary:
                out = Path(temporary)
                document = generate_assets(
                    CONFIG_PATH,
                    out,
                    {
                        "TTS_PROVIDER": "http",
                        "TTS_API_BASE": base,
                        "TTS_API_KEY": SECRET,
                        "TTS_VOICE": "remote-warm",
                    },
                    offline_available=True,
                    generated_at="2026-10-29T02:00:00Z",
                )
                self.assertEqual(document["source"], "AI_GENERATED")
                self.assertEqual(document["voice"], "remote-warm")
                self.assertEqual(document["engine_parameters"]["api_base_fingerprint"], api_base_fingerprint(base))
                self.assertEqual(document["engine_parameters"]["pauses_applied_by"], "remote")
                self.assertEqual(len(captured), 10)
                authorization, body = captured[0]
                self.assertEqual(authorization, f"Bearer {SECRET}")
                payload = json.loads(body.decode("utf-8"))
                self.assertEqual(payload["text"], "cat")
                self.assertEqual(payload["speechRate"], 0.8)
                self.assertEqual(payload["voice"], "remote-warm")
                self.assertEqual(payload["style"], "clear-warm")
                self.assertEqual(payload["pauseBetweenWordsMs"], 600)
                self.assertNotIn(SECRET, body.decode("utf-8"))
                stored = (out / "metadata.json").read_text(encoding="utf-8")
                stored += (out / "MANIFEST.md").read_text(encoding="utf-8")
                self.assertNotIn(SECRET, stored)
                self.assertTrue(file_declares_audio(out / "cat-v1.mp3"))
                check_assets(out)
        finally:
            server.shutdown()
            thread.join(timeout=3)
            server.server_close()

    def test_http_adapter_does_not_follow_redirects(self) -> None:
        class Handler(BaseHTTPRequestHandler):
            def do_POST(self) -> None:  # noqa: N802
                self.send_response(302)
                self.send_header("Location", "http://127.0.0.1/should-not-be-called")
                self.end_headers()

            def log_message(self, fmt: str, *args: object) -> None:
                return

        server = HTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        port = server.server_address[1]
        try:
            with tempfile.TemporaryDirectory() as temporary:
                out = Path(temporary) / "out"
                with self.assertRaises(TtsError) as caught:
                    generate_assets(
                        CONFIG_PATH,
                        out,
                        {
                            "TTS_PROVIDER": "http",
                            "TTS_API_BASE": f"http://127.0.0.1:{port}/v1/speech",
                            "TTS_API_KEY": SECRET,
                        },
                        generated_at="2026-10-29T02:30:00Z",
                    )
                self.assertIn("不跟随重定向", str(caught.exception))
                self.assertNotIn(SECRET, str(caught.exception))
                self.assertFalse(out.exists())
        finally:
            server.shutdown()
            thread.join(timeout=3)
            server.server_close()


class OfflineGenerationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if shutil.which("espeak-ng") is None:
            raise unittest.SkipTest("没有 espeak-ng，跳过离线发音测试")
        cls.temporary = tempfile.TemporaryDirectory()
        cls.out = Path(cls.temporary.name)
        cls.document = generate_assets(
            CONFIG_PATH,
            cls.out,
            {},
            generated_at="2026-10-29T03:00:00Z",
        )

    @classmethod
    def tearDownClass(cls) -> None:
        temporary = getattr(cls, "temporary", None)
        if temporary is not None:
            temporary.cleanup()

    def test_without_api_key_produces_ten_spoken_files(self) -> None:
        self.assertEqual(self.document["provider"], "offline")
        self.assertEqual(self.document["source"], "OFFLINE_TTS")
        self.assertEqual(self.document["voice"], "en-us+m3")
        self.assertEqual(self.document["speech_rate"], 0.8)
        self.assertEqual(self.document["engine_parameters"]["speed_wpm"], 140)
        self.assertEqual(self.document["engine_parameters"]["pitch"], 42)
        self.assertEqual(self.document["style"], "clear-warm")
        self.assertEqual(len(self.document["assets"]), 10)
        self.assertIn("espeak-ng", self.document["tool"])
        check_assets(self.out)
        durations = {}
        for asset in self.document["assets"]:
            self.assertEqual(asset["speech_rate"], 0.8)
            self.assertEqual(asset["pause_between_words_ms"], 600)
            self.assertEqual(asset["pause_between_sentences_ms"], 900)
            self.assertEqual(asset["pause_after_prompt_ms"], 400)
            self.assertTrue(asset["source_text"])
            self.assertTrue(asset["tool"])
            self.assertTrue(asset["generated_at"])
            self.assertEqual(asset["source"], "OFFLINE_TTS")
            self.assertNotIn("espeak_tokens", self.document["engine_parameters"])
            self.assertIn("espeak_tokens", asset["engine_parameters"])
            path = self.out / Path(asset["storage_key"]).name
            self.assertTrue(file_declares_audio(path))
            self.assertGreater(path.stat().st_size, 1000)
            probed_duration = asset["duration_ms"]
            self.assertGreater(probed_duration, 1200)
            durations[asset["source_text"]] = probed_duration
            self.assertGreater(peak_of(path), 0.45)
        self.assertGreater(durations["I see a cat."] - durations["cat"], 1500)
        self.assertIn("不是正式教材录音", self.document["warning"])

    def test_pauses_are_audible_and_article_is_schwa(self) -> None:
        phonemes = subprocess.check_output(
            ["espeak-ng", "-q", "-x", "-v", "en-us+m3", "-s", "140", "[[a#]]"],
            text=True,
        )
        self.assertIn("a#", phonemes)
        self.assertNotIn("eI", phonemes)
        sentence = self._runs("i-see-a-cat-v1.mp3")
        word = self._runs("cat-v1.mp3")
        self.assertTrue(sentence[0][0])
        self.assertGreaterEqual(sentence[0][1], 250)
        self.assertLessEqual(sentence[0][1], 600)
        self.assertTrue(sentence[-1][0])
        self.assertGreaterEqual(sentence[-1][1], 650)
        internal = [duration for silent, duration in sentence[1:-1] if silent and duration >= 450]
        self.assertGreaterEqual(len(internal), 3)
        word_internal = [duration for silent, duration in word[1:-1] if silent and duration >= 450]
        self.assertEqual(word_internal, [])

    def _runs(self, filename: str) -> list[tuple[bool, int]]:
        with tempfile.TemporaryDirectory() as temporary:
            wav = Path(temporary) / "decoded.wav"
            transcode_to_wav(self.out / filename, wav, [])
            samples, rate = read_wav(wav)
        return silence_runs(samples, rate)


class CommittedCatalogTest(unittest.TestCase):
    def test_shipped_animals_match_manifest(self) -> None:
        document = check_assets(AUDIO_DIR)
        self.assertEqual(document["source"], "OFFLINE_TTS")
        self.assertEqual(document["speech_rate"], 0.8)
        self.assertEqual(len(document["assets"]), 10)
        manifest = (AUDIO_DIR / "MANIFEST.md").read_text(encoding="utf-8")
        self.assertIn("不是真人录音", manifest)
        self.assertNotIn(SECRET, manifest)
        self.assertNotIn(SECRET, (AUDIO_DIR / "metadata.json").read_text(encoding="utf-8"))


class CliTest(unittest.TestCase):
    def test_cli_placeholder_does_not_print_key(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            out = Path(temporary) / "out"
            env = os.environ.copy()
            env["TTS_PROVIDER"] = "placeholder"
            env["TTS_VOICE"] = "cli-voice"
            env["TTS_API_KEY"] = SECRET
            env.pop("TTS_API_BASE", None)
            completed = subprocess.run(
                [
                    sys.executable,
                    str(MEDIA_ROOT / "tools" / "generate_animal_audio.py"),
                    "--config",
                    str(CONFIG_PATH),
                    "--out",
                    str(out),
                ],
                check=False,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=env,
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            self.assertNotIn(SECRET, completed.stdout)
            self.assertNotIn(SECRET, completed.stderr)
            self.assertIn("PLACEHOLDER", completed.stdout)
            self.assertIn("cli-voice", completed.stdout)
            self.assertNotIn(SECRET, (out / "metadata.json").read_text(encoding="utf-8"))


def _tiny_wav() -> bytes:
    samples = array.array("h", (int(8000 * __import__("math").sin(2 * __import__("math").pi * 440 * i / 22050)) for i in range(11025)))
    buffer = __import__("io").BytesIO()
    with wave.open(buffer, "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(22050)
        handle.writeframes(samples.tobytes())
    return buffer.getvalue()


if __name__ == "__main__":
    unittest.main()
