#!/bin/sh
# 스토어 스크린샷용 캡처를 찍는다. 견본만 쓰는 시뮬레이터에서만 돌린다(그 기기의 일기와 사진을 모두 지움)
#   sh scripts/store/store_shots.sh <phone|ipad> <기기 UDID> <ko|en|ja> [land] <결과 폴더>
# phone: iPhone 17 Pro Max, ipad: iPad Pro 13"(land를 주면 가로). 개발 빌드를 먼저 깐다(-diaryPlan PLUS로 Plus 테마를 엶)
# 시험 프로젝트는 cd iosApp && xcodegen generate --spec uitest.yml 로 만든다
set -eu
kind=$1; udid=$2; lang=$3
if [ "${4:-}" = land ]; then land=1; out=$5; else land=0; out=$4; fi
case "$lang" in
  ko) locale=ko_KR; place="한강공원" ;;
  en) locale=en_US; place="Riverside park" ;;
  ja) locale=ja_JP; place="川沿いの公園" ;;
esac
root=$(cd "$(dirname "$0")/../.." && pwd)
work=$(mktemp -d)

echo "$lang" > /tmp/diary-store-lang.txt
if [ "$land" = 1 ]; then touch /tmp/diary-store-land; else rm -f /tmp/diary-store-land; fi

xcrun simctl terminate "$udid" org.sjhstudio.diary 2>/dev/null || true
xcrun simctl spawn "$udid" defaults write -g AppleLanguages -array "$lang"
xcrun simctl spawn "$udid" defaults write -g AppleLocale "$locale"
xcrun simctl shutdown "$udid" 2>/dev/null || true
xcrun simctl boot "$udid"

mkdir -p "$work/art"; swift "$root/scripts/store/make_photos.swift" "$work/art"
python3 -I "$root/scripts/store/seed_ios.py" "$udid" "$lang" "$work/art" mood
container=$(xcrun simctl get_app_container "$udid" org.sjhstudio.diary data)
python3 -I "$root/scripts/store/seed_extra.py" "$container/Library/Application Support/diary.db" "$place"
prefs="$container/Library/Application Support/datastore/backup.preferences_pb"
grep -q store-sample "$prefs" 2>/dev/null || python3 -I "$root/scripts/store/seed_backup_prefs.py" "$prefs"

# 잠금 비밀번호는 키체인에 두어 서명 없는 빌드로는 저장되지 않음. 임시 서명(ad-hoc)으로 만든다
if [ "$kind" = ipad ]; then tests="StoreShots/testIPad"; else tests="StoreShots/testCapture StoreShots/testExtra"; fi
mkdir -p "$out"
for t in $tests; do
  rm -rf "$work/r.xcresult"
  (cd "$root" && xcodebuild test -project iosApp/iosAppUITest.xcodeproj -scheme DiaryUITests -destination "id=$udid" \
    -derivedDataPath iosApp/build -only-testing:"DiaryUITests/$t" -resultBundlePath "$work/r.xcresult" \
    CODE_SIGN_IDENTITY=- CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM= PROVISIONING_PROFILE_SPECIFIER= >"$work/test.log" 2>&1) || true
  grep -E "Test Case .*(passed|failed)|failed - " "$work/test.log" || true
  rm -rf "$work/raw"; xcrun xcresulttool export attachments --path "$work/r.xcresult" --output-path "$work/raw" >/dev/null 2>&1
  python3 -I - "$work/raw" "$out" <<'PY'
import json, os, re, shutil, sys
raw, out = sys.argv[1], sys.argv[2]
for t in json.load(open(os.path.join(raw, "manifest.json"))):
    for a in t.get("attachments", []):
        m = re.match(r"\d\d-([^_]+)_", a.get("suggestedHumanReadableName", ""))
        if m and a["exportedFileName"].endswith(".png") and not m.group(1).startswith("missing"):
            shutil.copy(os.path.join(raw, a["exportedFileName"]), os.path.join(out, m.group(1) + ".png"))
PY
done
ls "$out"
