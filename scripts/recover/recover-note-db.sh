#!/bin/sh
# 복구 문의로 받은 예전 일기 파일 복구(스펙 001 FR-010a, contracts/migration-flow.md "고치는 쪽")
#
#   sh scripts/recover/recover-note-db.sh 받은파일.zip [만든 파일을 둘 폴더]
#
# 받은 zip 안의 note.db(-wal, -shm)에서 SQLite .recover로 읽히는 줄만 살려 새 note.db와 돌려줄 zip 생성
# 받은 파일은 고치지 않고 임시 폴더의 사본으로만 작업
# 만든 zip은 저장소 안에 두지 않음
# 돌려준 뒤 받은 파일과 만든 파일을 지우는 순서는 README.md 참고
set -eu
IN=${1:?"받은 zip 경로를 주세요"}
OUT_DIR=${2:-"$HOME/Desktop"}
command -v sqlite3 >/dev/null || { echo "sqlite3가 필요해요" >&2; exit 1; }

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
unzip -q "$IN" -d "$WORK/in"
SRC="$WORK/in/legacy-snapshot"
[ -f "$SRC/note.db" ] || { echo "zip 안에 legacy-snapshot/note.db가 없어요" >&2; exit 1; }

# WAL에 남은 최근 일기도 읽히도록 세 파일을 같은 폴더에 둠
# 사본이라 열 때 WAL이 합쳐져도 받은 파일은 그대로
mkdir -p "$WORK/db"
cp "$SRC"/note.db* "$WORK/db/"
BEFORE=$(sqlite3 "$WORK/db/note.db" "SELECT count(*) FROM Note;" 2>/dev/null || echo "읽지 못함")

sqlite3 "$WORK/db/note.db" ".recover" > "$WORK/recover.sql"
sqlite3 "$WORK/fixed.db" < "$WORK/recover.sql" > /dev/null
CHECK=$(sqlite3 "$WORK/fixed.db" "PRAGMA integrity_check;")
[ "$CHECK" = "ok" ] || { echo "고친 파일 검사에 실패했어요: $CHECK" >&2; exit 1; }
ROWS=$(sqlite3 "$WORK/fixed.db" "SELECT count(*) FROM Note;")
# lost_and_found는 어느 표에도 넣지 못한 조각
# 개수만 알리고 돌려주는 파일에서는 제외
LOST=$(sqlite3 "$WORK/fixed.db" "SELECT count(*) FROM lost_and_found;" 2>/dev/null || echo 0)
sqlite3 "$WORK/fixed.db" "DROP TABLE IF EXISTS lost_and_found; VACUUM;"

mkdir -p "$WORK/out/legacy-snapshot" "$OUT_DIR"
cp "$WORK/fixed.db" "$WORK/out/legacy-snapshot/note.db"
NAME="1일1일기-고친파일-$(date +%Y%m%d).zip"
rm -f "$OUT_DIR/$NAME"
(cd "$WORK/out" && zip -qr "$OUT_DIR/$NAME" legacy-snapshot)
echo "받은 파일에서 읽힌 일기: $BEFORE"
echo "고친 파일의 일기: $ROWS편, 넣지 못한 조각: $LOST개"
echo "돌려줄 파일: $OUT_DIR/$NAME"
