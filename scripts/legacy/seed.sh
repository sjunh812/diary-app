#!/bin/sh
# 에뮬레이터의 1.7.4에 시험용 일기·사진·설정을 넣는다(스펙 001 T050).
# 실기기에는 쓰지 않는다.
#
#   ANDROID_SERIAL=emulator-5554 sh scripts/legacy/seed.sh
#
# 1.7.4를 한 번 연 뒤(DB와 설정 파일이 생긴 뒤) 돌린다.
# ALARM=0을 주면 매일 알림을 끈 설정을 넣는다.
# COUNT=1000을 주면 일기를 그만큼 더 넣는다(T051 큰 견본).
# PHOTO_EVERY=3이면 세 편마다 사진 한 장이고 기본은 한 편마다다.
# PHOTO_SOURCE=폴더를 주면 그 폴더의 JPEG를 차례로 돌려 쓴다(성능 시험용 큰 사진). 없으면 fixtures의 작은 사진.
# 넣은 수는 files/debug/seed.json에 적어 계측 시험이 읽는다.
# 1.7.4는 알림이 켜져 있으면 Android 14 이상에서 정확한 알람 권한이 없어 꺼진다(2026-11-01 Android 16 에뮬레이터에서 확인).
# 옛 앱과 같은 SQL로 만든 note.db, 캐시 사진,
# pref.xml·pref2.xml을 run-as로 앱 자리에 넣는다.
# 디버그 빌드라 run-as가 된다.
set -eu
: "${ANDROID_SERIAL:?ANDROID_SERIAL을 에뮬레이터로 정해 주세요(예: emulator-5554)}"
case "$ANDROID_SERIAL" in emulator-*) ;; *) echo "에뮬레이터에만 씁니다: $ANDROID_SERIAL" >&2; exit 1 ;; esac
export ANDROID_SERIAL

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
PKG=org.sjhstudio.diary
DATA=/data/user/0/$PKG
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# 옛 앱 NoteDatabase.java의 표 정의 그대로
sqlite3 "$WORK/note.db" <<SQL
PRAGMA user_version = 2;
CREATE TABLE android_metadata (locale TEXT);
INSERT INTO android_metadata VALUES ('ko_KR');
CREATE TABLE IF NOT EXISTS Note (_id INTEGER PRIMARY KEY AUTOINCREMENT, weather INTEGER DEFAULT -1, address TEXT DEFAULT '', location_x TEXT DEFAULT '', location_y TEXT DEFAULT '', contents TEXT DEFAULT '', mood INTEGER DEFAULT -1, picture TEXT DEFAULT '', create_date TIMESTAMP DEFAULT (datetime('now','localtime')), modify_date TIMESTAMP DEFAULT (datetime('now','localtime')), year INTEGER DEFAULT 1900, month INTEGER DEFAULT 1,star INTEGER DEFAULT 0);
CREATE UNIQUE INDEX IF NOT EXISTS Note_Index ON Note(create_date);
INSERT INTO Note(weather, address, contents, mood, picture, create_date, modify_date, year, month, star) VALUES
 (0, '서울 마포구', '퇴근길에 한강까지 걸었다.
바람이 차가워졌는데 그래서 더 좋았다.

내일은 일찍 자야지.', 7, '$DATA/cache/1699000000001.jpg,$DATA/cache/1699000000002.png,', '2024-10-31 21:30:00', '2024-10-31 21:40:00', 2024, 10, 1),
 (3, '', '마감 전날이라 밤늦게까지 일했다. 😵', 8, '', '2024-10-30 23:59:457', '2024-10-30 23:59:457', 2024, 10, 0),
 (4, '집', '비 오는 소리 들으면서 책 한 권. "작은 아씨들" & 따뜻한 차.', 5, '$DATA/cache/gone.jpg,', '2024-10-29 15:10:22', '2024-10-29 15:10:22', 2024, 10, 0),
 (-1, '', '', 0, '$DATA/cache/1699000000001.jpg,', '2024-10-28 08:00:00', '2024-10-28 08:00:00', 2024, 10, 0);
SQL
COUNT=${COUNT:-0}
PHOTO_EVERY=${PHOTO_EVERY:-1}
if [ "$COUNT" -gt 0 ]; then
  # 오래 쓴 사용자처럼 날짜를 12시간 간격으로 펴고, 글 길이도 다르게 한다.
  # 사진은 PHOTO_EVERY편마다 한 장이다.
  sqlite3 "$WORK/note.db" <<SQL
WITH RECURSIVE c(x) AS (SELECT 1 UNION ALL SELECT x + 1 FROM c WHERE x < $COUNT)
INSERT INTO Note(weather, address, contents, mood, picture, create_date, modify_date, year, month, star)
SELECT x % 7, CASE WHEN x % 5 = 0 THEN '서울 마포구' ELSE '' END,
       printf('큰 견본 %d번째 일기. 오늘도 한강까지 걸었다.', x) || substr(replace(hex(zeroblob(x % 40)), '00', ' 바람이 차가워졌는데 그래서 더 좋았다.'), 1, 1200),
       x % 9, CASE WHEN x % $PHOTO_EVERY = 0 THEN printf('$DATA/cache/big-%d.jpg,', x) ELSE '' END,
       datetime('2009-01-01 07:00:00', printf('+%d minutes', x * 720)), datetime('2009-01-01 07:00:00', printf('+%d minutes', x * 720)),
       CAST(strftime('%Y', datetime('2009-01-01 07:00:00', printf('+%d minutes', x * 720))) AS INTEGER),
       CAST(strftime('%m', datetime('2009-01-01 07:00:00', printf('+%d minutes', x * 720))) AS INTEGER), CASE WHEN x % 50 = 0 THEN 1 ELSE 0 END
FROM c;
SQL
  # 사진마다 해시가 다르게 JPEG 끝(EOI) 뒤에 번호를 붙인다.
  # 그림은 그대로 읽힌다.
  mkdir -p "$WORK/big"
  if [ -n "${PHOTO_SOURCE:-}" ]; then
    set -- "$PHOTO_SOURCE"/*.jpg
  else
    set -- "$ROOT/scripts/legacy/fixtures/photo-landscape-exif6.jpg"
  fi
  n=0
  i=$PHOTO_EVERY
  while [ $i -le "$COUNT" ]; do
    n=$((n % $# + 1))
    eval "src=\${$n}"
    cp "$src" "$WORK/big/big-$i.jpg"
    printf '%s' "$i" >> "$WORK/big/big-$i.jpg"
    i=$((i + PHOTO_EVERY))
  done
fi
printf '{"extra":%s}' "$COUNT" > "$WORK/seed.json"

# 사진 두 장.
# 썸네일과 크기 읽기를 보려고 진짜 그림을 쓴다(fixtures/README.md).
# gone.jpg는 일부러 넣지 않는다(캐시에서 지워진 사진).
cp "$ROOT/scripts/legacy/fixtures/photo-landscape-exif6.jpg" "$WORK/1699000000001.jpg"
cp "$ROOT/scripts/legacy/fixtures/photo-portrait.png" "$WORK/1699000000002.png"

cat > "$WORK/pref.xml" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="font_key" value="5" />
    <int name="font_size" value="3" />
    <int name="mode_key" value="0" />
    <boolean name="ask_location" value="false" />
$(if [ -n "${LOCK:-}" ]; then printf '    <string name="password">%s</string>\n    <boolean name="use_pw" value="true" />\n    <boolean name="finger_print" value="%s" />' "$LOCK" "$([ "${BIOMETRIC:-0}" = 1 ] && echo true || echo false)"; else printf '    <boolean name="use_pw" value="false" />'; fi)
</map>
XML
cat > "$WORK/pref2.xml" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="is_alarm_key" value="$([ "${ALARM:-1}" = 0 ] && echo false || echo true)" />
    <int name="hour_key" value="21" />
    <int name="minute_key" value="30" />
</map>
XML

adb shell am force-stop $PKG
adb shell mkdir -p /data/local/tmp/diary-seed
for f in note.db 1699000000001.jpg 1699000000002.png pref.xml pref2.xml; do adb push -q "$WORK/$f" /data/local/tmp/diary-seed/$f; done
adb push -q "$WORK/seed.json" /data/local/tmp/diary-seed/seed.json
if [ "$COUNT" -gt 0 ]; then adb push -q "$WORK/big" /data/local/tmp/diary-seed/; fi
adb shell run-as $PKG sh -c "'mkdir -p databases cache shared_prefs && rm -f databases/note.db-wal databases/note.db-shm databases/note.db-journal && cp /data/local/tmp/diary-seed/note.db databases/note.db && cp /data/local/tmp/diary-seed/1699000000001.jpg /data/local/tmp/diary-seed/1699000000002.png cache/ && cp /data/local/tmp/diary-seed/pref.xml /data/local/tmp/diary-seed/pref2.xml shared_prefs/ && mkdir -p files/debug && cp /data/local/tmp/diary-seed/seed.json files/debug/seed.json'"
if [ "$COUNT" -gt 0 ]; then adb shell run-as $PKG sh -c "'cp /data/local/tmp/diary-seed/big/* cache/'"; fi
adb shell rm -rf /data/local/tmp/diary-seed
echo "넣었어요: 일기 $((4 + COUNT))편, 사진 경로 $((5 + COUNT / PHOTO_EVERY))개(그중 1개는 없는 파일), 설정 두 파일"
