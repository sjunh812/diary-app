# 본문 HTML 계약

`Entry.bodyHtml`, `Draft.bodyHtml`, 백업 스냅샷, 내보내기가 같이 쓰는 형식이다. 바꾸면 백업 형식 버전(`docs/backup.md`)과 이전 시험을 같이 고친다.

## 허용 목록

| 태그 | 속성 | 뜻 |
| --- | --- | --- |
| `p` | 없음 | 문단 |
| `br` | 없음 | 문단 안 줄바꿈 |
| `b`, `strong` | 없음 | 굵게(저장은 `b`로 통일) |
| `u` | 없음 | 밑줄 |
| `span` | `style`에 `color` 또는 `background-color` 하나씩만 | 글자색, 형광펜. 값은 아래 팔레트의 라이트 hex만 |
| `ul`, `li` | 없음 | 글머리 목록. 한 단계만 |

그 밖의 태그는 태그만 버리고 안의 글은 남긴다. 그 밖의 속성은 모두 버린다. `script`, `style`, `iframe`은 안의 글까지 버린다.

## 팔레트

HTML에는 라이트 hex만 저장한다. 그릴 때는 hex로 키를 찾아 지금 테마의 같은 키 토큰 색(`ink/<색>`, `mark/<색>`)을 쓴다
(2026-10-31 D6 확정, 근거는 `docs/design.md` 색).

| 키 | 쓰임 | 라이트 hex(저장 값) | 다크 hex(그릴 때만) |
| --- | --- | --- | --- |
| `ink-red` | 글자색 | `#B91C1C` | `#F87171` |
| `ink-orange` | 글자색 | `#C2410C` | `#FB923C` |
| `ink-green` | 글자색 | `#15803D` | `#4ADE80` |
| `ink-blue` | 글자색 | `#1D4ED8` | `#60A5FA` |
| `ink-gray` | 글자색 | `#6B7280` | `#9CA3AF` |
| `ink-purple` | 글자색 | `#6D28D9` | `#A78BFA` |
| `ink-pink` | 글자색 | `#BE185D` | `#F472B6` |
| `mark-yellow` | 형광펜 | `#FEF08A` | `#713F12` |
| `mark-green` | 형광펜 | `#BBF7D0` | `#14532D` |
| `mark-blue` | 형광펜 | `#BFDBFE` | `#1E3A8A` |
| `mark-purple` | 형광펜 | `#DDD6FE` | `#4C1D95` |
| `mark-pink` | 형광펜 | `#FBCFE8` | `#831843` |

글자색은 두 테마 바탕(`canvas`, `surface`) 위에서 4.5:1 이상, 형광펜 위 `textPrimary`는 7:1 이상이다. hex 비교는 대소문자를 가리지 않는다.
팔레트에 없는 hex는 `span`째 버리고 글은 남긴다.

기본 글자색은 저장 값이 없다. 기본으로 돌리면 `span`의 색이 빠진다. 2026-11-05에 글자색 둘, 형광펜 둘을 더했다. 아직 출시 전이라 백업 형식 버전은 그대로이고, 더하기만 해서 예전 값은 그대로 읽힌다.

## 레거시 글

서식 없는 레거시 `contents`는 HTML 이스케이프 뒤 빈 줄로 나눈 덩어리를 `p`로, 덩어리 안 줄바꿈을 `br`로 바꾼다. `plainText`는 원문 그대로다.
나누는 정확한 규칙과 고정 시험 값은 스펙 001 `contracts/legacy-mapping.md` "본문 변환"이다.
`toPlainText(bodyHtml) == plainText`가 이전 검증에 들어간다(`docs/migration.md`).

## 서식 없는 글 만들기

`p` 사이는 빈 줄 하나, `br`과 `li` 사이는 줄바꿈 하나, `li` 앞에는 `• `. 엔티티는 풀어서 쓴다. 공백, 탭, `\r`은 줄이거나 바꾸지 않는다
(HTML 표시의 `white-space: pre-wrap`과 같은 뜻, 2026-11-01 스펙 001 R23).

## 에디터와 저장 형식 (2026-11-05)

에디터는 줄마다 문단이고, 저장 형식은 빈 줄로 나뉜 덩어리가 문단이고 줄바꿈은 `br`이다. 에디터를 오갈 때 `editorToBody`, `bodyToEditor`로 바꾼다. 옛 글(`legacyToHtml`)과 같은 규칙이라 줄바꿈 한 번이 읽기 화면에서 빈 줄로 보이지 않는다.
