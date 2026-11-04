#!/bin/sh
# 쓰는 도중 앱을 강제 종료해도 5초 전까지 쓴 글이 남는지 COUNT번 본다(스펙 002 SC-002).
# 에뮬레이터에서만 돈다. 잠금이 없고 앱을 한 번 연 기기에 쓴다.
#
# ANDROID_SERIAL=emulator-5558 COUNT=100 WRITE_X=80 WRITE_Y=168 BODY_X=900 BODY_Y=400 \
#   CONTINUE_TEXT="Keep writing" sh scripts/qa/kill-while-writing.sh
set -eu

: "${ANDROID_SERIAL:?ANDROID_SERIAL에 에뮬레이터를 적어 주세요}"
case "$ANDROID_SERIAL" in
  emulator-*) ;;
  *) echo "에뮬레이터에서만 돌립니다: $ANDROID_SERIAL"; exit 2 ;;
esac

COUNT="${COUNT:-100}"
WRITE_X="${WRITE_X:-80}"
WRITE_Y="${WRITE_Y:-168}"
BODY_X="${BODY_X:-900}"
BODY_Y="${BODY_Y:-400}"
CONTINUE_TEXT="${CONTINUE_TEXT:-Keep writing}"
SAVE_TEXT="${SAVE_TEXT:-Save}"
PACKAGE=org.sjhstudio.diary
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

pause() { perl -e "select(undef,undef,undef,$1)"; }

# 화면에서 글자를 찾아 가운데를 누른다. 없으면 실패를 돌려준다.
tap_text() {
  adb shell uiautomator dump /sdcard/qa.xml >/dev/null 2>&1 || return 1
  adb shell cat /sdcard/qa.xml > "$WORK/ui.xml"
  point="$(python3 -I - "$1" "$WORK/ui.xml" <<'PY'
import re, sys
text = open(sys.argv[2], encoding="utf-8").read()
pattern = r'(?:text|content-desc)="%s"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(sys.argv[1])
found = re.search(pattern, text)
if found:
    x1, y1, x2, y2 = map(int, found.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
PY
)"
  [ -n "$point" ] || return 1
  # shellcheck disable=SC2086
  adb shell input tap $point
}

# 임시 저장본에 표시 글자가 있는지 DB 파일을 받아 본다. 일기 내용을 출력하지 않는다.
has_mark() {
  rm -f "$WORK"/diary.db*
  adb exec-out run-as "$PACKAGE" cat databases/diary.db > "$WORK/diary.db"
  adb exec-out run-as "$PACKAGE" cat databases/diary.db-wal > "$WORK/diary.db-wal" 2>/dev/null || true
  python3 -I - "$WORK/diary.db" "$1" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
rows = db.execute("SELECT body_html FROM draft").fetchall()
sys.exit(0 if any(sys.argv[2] in (row[0] or "") for row in rows) else 1)
PY
}

passed=0
index=1
while [ "$index" -le "$COUNT" ]; do
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  # 앱이 늦게 뜰 때가 있어서 쓰기 화면이 열린 것을 본 뒤에 입력한다.
  attempt=0
  opened=no
  while [ "$attempt" -lt 6 ]; do
    pause 3
    adb shell input tap "$WRITE_X" "$WRITE_Y"
    pause 2
    tap_text "$CONTINUE_TEXT" || true
    pause 1.5
    adb shell uiautomator dump /sdcard/qa.xml >/dev/null 2>&1 || true
    if adb shell cat /sdcard/qa.xml | grep -q "text=\"$SAVE_TEXT\""; then
      opened=yes
      break
    fi
    attempt=$((attempt + 1))
  done
  if [ "$opened" = no ]; then
    echo "$index 시험 준비 실패: 쓰기 화면이 열리지 않았어요"
    exit 3
  fi
  adb shell input tap "$BODY_X" "$BODY_Y"
  pause 1
  mark="qa${index}x"
  adb shell input text "$mark"
  # 쓴 뒤 5초가 지나면 종료 시점까지 남아 있어야 한다.
  pause 5
  # 실패를 살펴볼 때 SHOT에 경로를 주면 종료 직전 화면을 남긴다.
  if [ -n "${SHOT:-}" ]; then adb exec-out screencap -p > "$SHOT"; fi
  adb shell am force-stop "$PACKAGE"
  pause 1
  if has_mark "$mark"; then
    passed=$((passed + 1))
    echo "$index 통과"
  else
    echo "$index 실패: $mark"
    if [ -n "${SHOT:-}" ]; then exit 1; fi
  fi
  index=$((index + 1))
done

echo "결과: $passed/$COUNT"
[ "$passed" -eq "$COUNT" ]
