#!/usr/bin/env python3
"""生成或检查动物课十段慢速示范音。

在仓库根目录执行：

    python3 media-samples/tools/generate_animal_audio.py
    python3 media-samples/tools/generate_animal_audio.py --check

不设置 TTS_API_KEY 时，有 espeak-ng 就用离线语音，否则用占位音。
密钥只从环境变量读取，不会写进音频、清单或日志。
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from ttslib.errors import TtsError, scrub
from ttslib.generate import check_assets, generate_assets, summary_lines
from ttslib.providers import secret_values

MEDIA_ROOT = Path(__file__).resolve().parents[1]


def main(argv: list[str] | None = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description="生成或检查动物课慢速示范音频")
    parser.add_argument("--config", type=Path, default=MEDIA_ROOT / "tts" / "tts.config.yaml")
    parser.add_argument("--out", type=Path, default=MEDIA_ROOT / "audio" / "animals")
    parser.add_argument("--check", action="store_true", help="核对已有文件的文件头、哈希和清单")
    parser.add_argument(
        "--allow-placeholder",
        action="store_true",
        help="允许用占位音覆盖已经生成的语音",
    )
    args = parser.parse_args(argv)
    secrets = secret_values(dict(os.environ))
    try:
        if args.check:
            document = check_assets(args.out)
        else:
            explicit_placeholder = (os.environ.get("TTS_PROVIDER") or "").strip().lower() == "placeholder"
            document = generate_assets(
                args.config,
                args.out,
                dict(os.environ),
                allow_placeholder=args.allow_placeholder or explicit_placeholder,
            )
    except TtsError as exc:
        print(f"错误：{exc}", file=sys.stderr)
        return 1
    except Exception as exc:
        print(f"错误：{scrub(str(exc), secrets)}", file=sys.stderr)
        return 1
    for line in summary_lines(document):
        print(line)
    if args.check:
        print("已核对 10 个文件：文件头是音频，哈希和字节数与清单一致。")
    else:
        print(f"目录：{args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
