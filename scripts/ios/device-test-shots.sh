#!/bin/sh
# 사용: scripts/ios/device-test-shots.sh <xcresult> <결과 폴더> — 테스트가 남긴 캡처를 단계 이름으로 꺼내고 작게 줄임
R="$1"; O="$2"; rm -rf "$O"; mkdir -p "$O/raw"
xcrun xcresulttool export attachments --path "$R" --output-path "$O/raw" >/dev/null 2>&1
python3 -I - "$O" <<'PY'
import json, sys, os, shutil, re
o = sys.argv[1]
for t in json.load(open(os.path.join(o, "raw", "manifest.json"))):
    for a in t.get("attachments", []):
        n = a.get("suggestedHumanReadableName", "")
        m = re.match(r"(\d\d-[^_]+)", n)
        if m and a["exportedFileName"].endswith(".png"):
            shutil.copy(os.path.join(o, "raw", a["exportedFileName"]), os.path.join(o, m.group(1) + ".png"))
PY
for f in "$O"/*.png; do sips -Z 520 "$f" >/dev/null 2>&1; done
ls "$O" | grep png
