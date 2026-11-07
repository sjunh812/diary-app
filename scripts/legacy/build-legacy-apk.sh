#!/bin/sh
# 1.7.4(Play 마지막 레거시, 커밋 94179a4) 디버그 APK를 임시 worktree에서 만든다(스펙 001 T050).
# 업그레이드 시험용이다.
# 새 앱 디버그 빌드와 같은 디버그 키로 서명돼 `adb install -r`로 덮어 설치할 수 있다.
#
#   sh scripts/legacy/build-legacy-apk.sh [만들 폴더]
#
# - 레거시 빌드는 Gradle 8.9라 JDK 17(JBR 17)이 있어야 한다.
# LEGACY_JAVA_HOME을 주지 않으면 macOS의 JDK 17을 찾는다.
# - 레거시 app/.../utils/ApiKey.kt는 저장소에 없다(.gitignore).
# 로컬에 있으면 임시 worktree에만 복사한다.
#   그 안의 네이버 지도 키는 2026-11-01에 해제됐다(CLAUDE.md).
set -eu

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
COMMIT=94179a4
WORK=${1:-"${TMPDIR:-/tmp}/diary-legacy-1.7.4"}
OUT="$ROOT/scripts/legacy/out"

# 셸의 JAVA_HOME이 더 새 JDK여도 레거시 빌드는 17로 돌린다(Kotlin 1.8은 JVM 25를 모른다).
JAVA_HOME=${LEGACY_JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}
[ -n "$JAVA_HOME" ] || { echo "JDK 17을 찾지 못했어요. LEGACY_JAVA_HOME을 주세요." >&2; exit 1; }
export JAVA_HOME

if [ ! -d "$WORK" ]; then
  git -C "$ROOT" worktree add --detach "$WORK" "$COMMIT"
fi
KEY=app/src/main/java/org/sjhstudio/diary/utils/ApiKey.kt
[ -f "$ROOT/$KEY" ] && cp "$ROOT/$KEY" "$WORK/$KEY"
[ -f "$ROOT/local.properties" ] && cp "$ROOT/local.properties" "$WORK/local.properties"

(cd "$WORK" && sh ./gradlew :app:assembleDebug -q)
mkdir -p "$OUT"
cp "$WORK/app/build/outputs/apk/debug/app-debug.apk" "$OUT/diary-1.7.4-debug.apk"
echo "만들었어요: $OUT/diary-1.7.4-debug.apk"
echo "worktree를 치우려면: git worktree remove --force \"$WORK\""
