#!/bin/sh
# 1.7.4 → 새 앱 업그레이드를 에뮬레이터에서 돌려 본다(스펙 001 T050, quickstart §2). 실기기에는 쓰지 않는다.
#
#   ANDROID_SERIAL=emulator-5554 sh scripts/legacy/upgrade-test.sh
#
# 순서: 지우기 → 1.7.4 깔고 한 번 열기 → seed.sh → 원본 해시 → 새 APK 덮어 설치 → 열기 → 이전 끝 기다리기 → 확인.
# 확인: 원본 다섯 파일의 SHA-256이 전후로 같은지, 새 DB의 일기·사진 수, 남은 파일.
set -eu
: "${ANDROID_SERIAL:?ANDROID_SERIAL을 에뮬레이터로 정해 주세요(예: emulator-5554)}"
case "$ANDROID_SERIAL" in emulator-*) ;; *) echo "에뮬레이터에만 씁니다: $ANDROID_SERIAL" >&2; exit 1 ;; esac
export ANDROID_SERIAL

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
PKG=org.sjhstudio.diary
LEGACY_APK="$ROOT/scripts/legacy/out/diary-1.7.4-debug.apk"
NEW_APK="$ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
ORIGINALS="databases/note.db shared_prefs/pref.xml shared_prefs/pref2.xml cache/1699000000001.jpg cache/1699000000002.png"

[ -f "$LEGACY_APK" ] || sh "$ROOT/scripts/legacy/build-legacy-apk.sh"
[ -f "$NEW_APK" ] || (cd "$ROOT" && ./gradlew :composeApp:assembleDebug -q)

adb uninstall $PKG >/dev/null 2>&1 || true
adb install "$LEGACY_APK" >/dev/null
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
sh "$ROOT/scripts/legacy/seed.sh"
adb shell run-as $PKG sha256sum $ORIGINALS > "$WORK/before"

adb install -r "$NEW_APK" >/dev/null
START=$(date +%s)
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
i=0
until adb shell run-as $PKG cat files/migration/state.json 2>/dev/null | grep -qE '"step":"(Done|Partial|Failed)"'; do
  i=$((i + 1)); [ $i -gt 120 ] && { echo "이전이 2분 안에 끝나지 않았어요" >&2; exit 1; }
  sleep 1
done
echo "이전 끝: $(( $(date +%s) - START ))초, $(adb shell run-as $PKG cat files/migration/state.json)"

adb shell run-as $PKG sha256sum $ORIGINALS > "$WORK/after"
if diff "$WORK/before" "$WORK/after" >/dev/null; then echo "원본 해시: 그대로"; else echo "원본 해시가 바뀌었어요" >&2; diff "$WORK/before" "$WORK/after" >&2; exit 1; fi
adb exec-out run-as $PKG cat databases/diary.db > "$WORK/diary.db"
echo "새 DB: 일기 $(sqlite3 "$WORK/diary.db" 'SELECT COUNT(*) FROM entry')편, 사진 줄 $(sqlite3 "$WORK/diary.db" "SELECT COUNT(*) FROM media WHERE status='normal'")개, 찾을 수 없는 사진 $(sqlite3 "$WORK/diary.db" "SELECT COUNT(*) FROM media WHERE status='missing'")개"
echo "이전 폴더: $(adb shell run-as $PKG ls files/migration | tr '\n' ' ')"
echo "결과: $(adb shell run-as $PKG cat files/migration/report.json)"
