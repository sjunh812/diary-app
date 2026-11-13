#!/bin/sh
# 이번 변경에서 추가하거나 수정한 파일 중 큰 파일 찾기. CI "저장소 위생"에서 호출
#   sh scripts/check-large-files.sh <기준 커밋> <끝 커밋> <한도 KB>
set -eu

base=$1
head=$2
limit_kb=$3

# 첫 푸시와 새 브랜치는 비교 기준이 없어 끝 커밋 하나만 확인
case "$base" in
  "" | 0000000000000000000000000000000000000000) range="$head~1 $head" ;;
  *) range="$base $head" ;;
esac

found=0
# shellcheck disable=SC2086
for path in $(git diff --name-only --diff-filter=AM $range); do
  bytes=$(git cat-file -s "$head:$path" 2>/dev/null || echo 0)
  if [ "$bytes" -gt $((limit_kb * 1024)) ]; then
    echo "큰 파일: $path ($((bytes / 1024)) KB, 한도 $limit_kb KB)"
    found=1
  fi
done
exit "$found"
