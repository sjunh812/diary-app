#!/bin/sh
# 1.7.4 → 새 앱 업그레이드를 에뮬레이터에서 돌려 본다(스펙 001 T050, quickstart §2). 실기기에는 쓰지 않는다.
#
#   ANDROID_SERIAL=emulator-5554 sh scripts/legacy/upgrade-test.sh
#
# 순서: 지우기 → 1.7.4 깔고 한 번 열기 → seed.sh → 원본 해시 → 새 APK 덮어 설치 → 열기 → 이전 끝 기다리기 → 확인.
# 확인: 원본 다섯 파일의 SHA-256이 전후로 같은지, 새 DB의 일기·사진 수, 남은 파일.
# 새 앱은 한국어로 연다. APP_LOCALE=ja-JP처럼 주면 그 언어로 연다(Android 13 이상, 그 아래는 기기 언어, docs/design.md 여러 언어).
# COUNT=1000: 사진이 한 장씩 든 일기를 그만큼 더 넣는다(seed.sh, T051 큰 견본).
# KILL_AT=Copying: 그 단계에서 앱을 끊었다가 다시 연다(개발 빌드의 KillAtHook, quickstart §3). Snapshotting, Verifying, Committing도 된다.
# INSTRUMENT=1: 끝난 뒤 기기 안에서 계측 시험 UpgradeVerificationTest를 돌린다(T050). 먼저 :composeApp:assembleDebugAndroidTest.
# LOCK=0427: 1.7.4에 그 비밀번호로 잠금을 켜 두고(seed.sh), 새 앱이 잠금 화면부터 띄우는지와 잠긴 동안 이전이 시작되지 않는지 본 뒤
# 하드웨어 키로 비밀번호를 넣는다(스펙 001 US2, quickstart §4). BIOMETRIC=1이면 지문 설정도 켜고, 비밀번호 대신 에뮬레이터 지문(손가락 1, 미리 등록)으로 연다.
set -eu
: "${ANDROID_SERIAL:?ANDROID_SERIAL을 에뮬레이터로 정해 주세요(예: emulator-5554)}"
case "$ANDROID_SERIAL" in emulator-*) ;; *) echo "에뮬레이터에만 씁니다: $ANDROID_SERIAL" >&2; exit 1 ;; esac
export ANDROID_SERIAL

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
PKG=org.sjhstudio.diary
LEGACY_APK="$ROOT/scripts/legacy/out/diary-1.7.4-debug.apk"
NEW_APK="$ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
TEST_APK="$ROOT/composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
ORIGINALS="databases/note.db shared_prefs/pref.xml shared_prefs/pref2.xml cache/1699000000001.jpg cache/1699000000002.png"

# 원본 다섯 파일의 SHA-256. API 24에는 기기 쪽 sha256sum이 없어 꺼내서 이 컴퓨터에서 잰다.
hash_originals() {
  for f in $ORIGINALS; do
    printf '%s  %s\n' "$(adb exec-out run-as $PKG cat "$f" | shasum -a 256 | cut -d' ' -f1)" "$f"
  done
}

[ -f "$LEGACY_APK" ] || sh "$ROOT/scripts/legacy/build-legacy-apk.sh"
[ -f "$NEW_APK" ] || (cd "$ROOT" && ./gradlew :composeApp:assembleDebug -q)

adb uninstall $PKG >/dev/null 2>&1 || true
adb install "$LEGACY_APK" >/dev/null
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
sh "$ROOT/scripts/legacy/seed.sh"
hash_originals > "$WORK/before"

adb install -r "$NEW_APK" >/dev/null
# 앱 언어는 한국어가 기본이다. 앱별 언어는 Android 13(API 33)부터 있어 그 아래는 기기 언어를 따른다.
if [ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" -ge 33 ]; then
  adb shell cmd locale set-app-locales $PKG --locales "${APP_LOCALE:-ko-KR}"
fi
if [ -n "${KILL_AT:-}" ]; then
  printf '%s' "$KILL_AT" > "$WORK/kill_at"
  adb push -q "$WORK/kill_at" /data/local/tmp/diary-kill_at
  adb shell run-as $PKG sh -c "'mkdir -p files/debug && cp /data/local/tmp/diary-kill_at files/debug/kill_at'"
  adb shell rm -f /data/local/tmp/diary-kill_at
fi
# 잠금 화면이 뜨기를 기다려, 잠긴 동안 이전이 시작되지 않았는지 보고 비밀번호를 넣는다.
unlock() {
  i=0
  # 생체 인증을 켜 두었으면(BIOMETRIC=1) 시스템 생체 인증 창이 잠금 화면을 덮는다. 그 창(지문 센서)도 잠금 화면으로 본다.
  until adb shell uiautomator dump /data/local/tmp/diary-ui.xml >/dev/null 2>&1 &&
    adb shell cat /data/local/tmp/diary-ui.xml | grep -qE '비밀번호를 입력해 주세요|Enter your passcode|パスコードを入力してください|content-desc="Fingerprint sensor"'; do
    i=$((i + 1)); [ $i -gt 60 ] && { echo "잠금 화면이 뜨지 않았어요" >&2; exit 1; }
    sleep 1
  done
  adb shell rm -f /data/local/tmp/diary-ui.xml
  if adb shell run-as $PKG cat files/migration/state.json 2>/dev/null | grep -qE '"step":"(Snapshotting|Copying|Verifying|Committing|Done|Partial|Failed)"'; then
    echo "잠긴 동안 이전이 시작됐어요" >&2; exit 1
  fi
  echo "잠금 화면: 잠긴 동안 이전 시작 안 함"
  if [ "${BIOMETRIC:-0}" = 1 ]; then
    # 에뮬레이터에 등록한 지문(손가락 1)을 댄다. 에뮬레이터에서 지문을 먼저 등록해 둬야 한다.
    # 창이 막 뜨는 중에 대면 인식하지 않아서, 창이 사라질 때까지 몇 번 댄다.
    n=0
    while adb shell uiautomator dump /data/local/tmp/diary-ui.xml >/dev/null 2>&1 &&
      adb shell cat /data/local/tmp/diary-ui.xml | grep -q 'content-desc="Fingerprint sensor"'; do
      n=$((n + 1)); [ $n -gt 5 ] && { echo "지문으로 열리지 않았어요" >&2; exit 1; }
      sleep 2; adb emu finger touch 1 >/dev/null; sleep 1; adb emu finger remove 1 >/dev/null
    done
    adb shell rm -f /data/local/tmp/diary-ui.xml
    echo "생체 인증으로 열기"
  else
    for d in $(printf '%s' "$LOCK" | sed 's/./& /g'); do adb shell input keyevent "KEYCODE_$d"; done
  fi
}
START=$(date +%s)
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
if [ -n "${LOCK:-}" ]; then unlock; fi
if [ -n "${KILL_AT:-}" ]; then
  # 훅이 그 단계에 들어서며 프로세스를 끝낸다. 끊긴 것을 본 뒤 다시 연다.
  i=0
  until ! adb shell pidof $PKG >/dev/null 2>&1 && adb shell run-as $PKG cat files/migration/state.json 2>/dev/null | grep -q "\"step\":\"$KILL_AT\""; do
    i=$((i + 1)); [ $i -gt 300 ] && { echo "$KILL_AT 단계에서 끊기지 않았어요" >&2; exit 1; }
    sleep 1
  done
  echo "끊김: $(adb shell run-as $PKG cat files/migration/state.json)"
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  # 프로세스가 새로 시작했으니 다시 잠겨 있다.
  if [ -n "${LOCK:-}" ]; then unlock; fi
fi
i=0
until adb shell run-as $PKG cat files/migration/state.json 2>/dev/null | grep -qE '"step":"(Done|Partial|Failed)"'; do
  i=$((i + 1)); [ $i -gt 600 ] && { echo "이전이 10분 안에 끝나지 않았어요" >&2; exit 1; }
  sleep 1
done
echo "이전 끝: $(( $(date +%s) - START ))초, $(adb shell run-as $PKG cat files/migration/state.json)"

hash_originals > "$WORK/after"
if diff "$WORK/before" "$WORK/after" >/dev/null; then echo "원본 해시: 그대로"; else echo "원본 해시가 바뀌었어요" >&2; diff "$WORK/before" "$WORK/after" >&2; exit 1; fi
adb exec-out run-as $PKG cat databases/diary.db > "$WORK/diary.db"
echo "새 DB: 일기 $(sqlite3 "$WORK/diary.db" 'SELECT COUNT(*) FROM entry')편, 사진 줄 $(sqlite3 "$WORK/diary.db" "SELECT COUNT(*) FROM media WHERE status='normal'")개, 찾을 수 없는 사진 $(sqlite3 "$WORK/diary.db" "SELECT COUNT(*) FROM media WHERE status='missing'")개"
echo "이전 폴더: $(adb shell run-as $PKG ls files/migration | tr '\n' ' ')"
echo "결과: $(adb shell run-as $PKG cat files/migration/report.json)"
# 이전 시간: 마지막 시도의 시작(state.startedAt)부터 끝(report.finishedAt)까지
STARTED=$(adb shell run-as $PKG cat files/migration/state.json | sed -n 's/.*"startedAt":\([0-9]*\).*/\1/p')
FINISHED=$(adb shell run-as $PKG cat files/migration/report.json | sed -n 's/.*"finishedAt":\([0-9]*\).*/\1/p')
echo "이전 시간: $((FINISHED - STARTED))ms (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
if [ "${INSTRUMENT:-0}" = 1 ]; then
  adb install -r -t "$TEST_APK" >/dev/null
  RESULT=$(adb shell am instrument -w -e class org.sjhstudio.diary.UpgradeVerificationTest $PKG.test/androidx.test.runner.AndroidJUnitRunner)
  if echo "$RESULT" | grep -q "OK (1 test)"; then echo "계측 시험: 통과"; else echo "계측 시험이 실패했어요" >&2; echo "$RESULT" >&2; exit 1; fi
fi
