#!/bin/sh
# .githooks/commit-msg 시험(FR-031).
# 실행: 저장소 루트에서 sh scripts/test-commit-msg.sh
hook="$(dirname "$0")/../.githooks/commit-msg"
tmp=$(mktemp)
failures=0

expect() {
  want=$1
  shift
  printf '%s\n' "$@" > "$tmp"
  if sh "$hook" "$tmp" 2>/dev/null; then got=pass; else got=fail; fi
  if [ "$got" != "$want" ]; then
    echo "틀림($want 기대, $got): $1"
    failures=$((failures + 1))
  fi
}

expect pass "feat(write): 사진 여러 장 한 번에 고르기"
expect pass "docs: 개발 전 디자인 점검 단계 추가"
expect pass "fix!: 저장 형식 바꿈" "" "BREAKING CHANGE: 열 이름이 바뀐다"
expect pass "chore(release): 2.0.0 (37)"
expect pass "# 주석은 건너뛴다" "" "refactor(database): 색인 이름 정리"
expect pass "Merge branch 'develop' into feat/foundation-migration"
expect pass "Revert \"feat(write): 사진 여러 장 한 번에 고르기\""
expect pass "feat: $(printf '가%.0s' $(seq 1 71))"
expect fail "feat: $(printf '가%.0s' $(seq 1 72))"
expect fail "refact: 예전 type은 받지 않는다"
expect fail "feat(Write): scope는 소문자"
expect fail "feat:콜론 뒤 빈칸 없음"
expect fail "feat:  빈칸 두 번"
expect fail "사진 여러 장 한 번에 고르기"
expect fail "Feat: 대문자 type"

rm -f "$tmp"
if [ "$failures" -eq 0 ]; then echo "커밋 형식 검사 시험 통과"; else echo "${failures}개 틀림"; exit 1; fi
