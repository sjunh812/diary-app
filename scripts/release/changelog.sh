#!/bin/sh
# 변경 기록을 feat·fix 커밋 제목에서 뽑는다(CLAUDE.md 커밋 규칙).
#   sh scripts/release/changelog.sh v2.0.0            # 바로 앞 v* 태그부터 이 태그까지
#   sh scripts/release/changelog.sh HEAD 88631feb     # 범위를 직접 준다
# 앞 태그가 없으면 처음부터 본다.
set -eu

end=${1:?끝 지점을 주세요(예: v2.0.0)}
start=${2:-$(git describe --tags --abbrev=0 --match 'v*' "$end^" 2>/dev/null || true)}

if [ -n "$start" ]; then
  range="$start..$end"
else
  range=$end
fi

# 앞의 type(scope)!: 를 떼고 제목만 남긴다.
section() {
  title=$1
  type=$2
  lines=$(git log --no-merges --format='%s' "$range" |
    grep -E "^$type(\([a-z0-9-]+\))?!?: " |
    sed -E "s/^$type(\([a-z0-9-]+\))?!?: /- /" || true)
  [ -n "$lines" ] || return 0
  printf '## %s\n\n%s\n\n' "$title" "$lines"
}

section "새 기능" feat
section "고친 것" fix
