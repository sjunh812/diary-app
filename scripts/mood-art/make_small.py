"""기분 테마 작은 그림(_s, 120px) 만들기.

240px 그림을 정확히 반으로(2×2 평균) 줄여 같은 폴더에 `<이름>_s.webp`로 둔다.
작은 칸(그림 칸 120px 이하)에서 240px를 한 번에 줄이면 테두리 같은 가는 선이 계단처럼 깨져서다.
도트 테마는 원본처럼 무손실, 나머지는 원본 내보내기와 같은 cwebp 설정.

쓰는 법(Pillow와 cwebp 필요):
  python3 -m venv /tmp/moodart && /tmp/moodart/bin/pip install pillow
  /tmp/moodart/bin/python -I scripts/mood-art/make_small.py
"""
import pathlib
import subprocess
import sys
import tempfile

from PIL import Image

DRAWABLE = pathlib.Path(__file__).resolve().parents[2] / "shared/core/ui/src/commonMain/composeResources/drawable"
SMALL = 120


def main() -> int:
    sources = sorted(p for p in DRAWABLE.glob("*.webp") if p.stem.startswith(("mood_", "clay_")) and not p.stem.endswith("_s"))
    with tempfile.TemporaryDirectory() as tmp:
        for src in sources:
            image = Image.open(src).convert("RGBA")
            if image.size != (SMALL * 2, SMALL * 2):
                print(f"240px가 아님: {src.name} {image.size}", file=sys.stderr)
                return 1
            png = pathlib.Path(tmp) / f"{src.stem}.png"
            image.resize((SMALL, SMALL), Image.BOX).save(png)
            out = DRAWABLE / f"{src.stem}_s.webp"
            quality = ["-lossless"] if src.stem.startswith("mood_dot_") else ["-q", "92", "-alpha_q", "100"]
            subprocess.run(["cwebp", "-quiet", *quality, "-m", "6", str(png), "-o", str(out)], check=True)
    print(f"{len(sources)}장")
    return 0


if __name__ == "__main__":
    sys.exit(main())
