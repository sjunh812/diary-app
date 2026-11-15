# 디자인 점검: 의견 카드

점검일 2026-11-15. 대상은 Figma "화면 > 2 일기 > 홈 상태"의 `S1 · 의견 카드`(라이트·다크·태블릿). 새 부품 없이 백업 권하기 카드와 같은 `DiaryStatusBox`를 쓴다. 홈 상태 섹션 규격 검사(`scripts/figma/audit.js`를 섹션으로 좁혀 돌림)에서 남은 것은 라이트 전용 `icon-button`(검사가 허용하는 예외)뿐이다.

| 항목 | 지금 값 | 레퍼런스 | 판단 |
| --- | --- | --- | --- |
| 닫을 수 있는 안내의 쓰임 | 한 번 알리고, 닫으면 다시 안 띄움 | SEED Callout: 닫을 수 있는 안내는 한 번만 알려도 되는 정보, 닫은 뒤 같은 메시지를 다시 띄우지 않음(<https://seed-design.io/components/callout>) | 그대로 |
| 한 화면 안내 수 | 홈 맨 위 카드 한 장(백업 카드 먼저) | SEED Page Banner: 한 화면에 하나(<https://seed-design.io/components/page-banner>) | 그대로(FR-014) |
| 안쪽 여백 | 20 | SEED Callout 14, M3 카드 16(<https://m3.material.io/components/cards/specs>) | 그대로. 홈 백업 카드와 같은 값이 통일에 맞고, 두 줄 머리에서 14는 빽빽함 |
| 모서리 | 16 | SEED Callout 10, M3 카드 12 | 그대로. 이 앱 카드 모서리 단계(`docs/design.md`) |
| 머리·설명 간격 | 6 | 없음 | 그대로(두 줄 머리 붙어 보임 방지, 기존 값) |
| 버튼 | 작은 채운 버튼 32 + 글자 버튼, 누르는 곳 48 | M3 48dp, HIG 44pt | 그대로(`DiaryButton`이 48을 채움) |
| 아이콘 | 16, 선 1.75×16/24, 점 셋 말풍선 | `docs/design.md` 아이콘 16·20·24·32 | 아이콘 세트에 `icon/message-circle-more/16` 더함 |
| 이모지 | 제목 끝 😊 하나 | 메모리 "이모지는 아껴서": 한 화면 하나 이하, 버튼·목록 줄 제외 | 그대로. 홈의 다른 이모지 없음 |
| 줄바꿈 | 앱은 띄어쓰기에서만 바뀜 | 기존 `keepWords`, `PhraseText` | Figma는 낱말 중간에서 끊겨 보이나 앱은 아님 |
| 다크 | 바탕 `sunken`, 글자 `textPrimary`/`textSecondary` | `docs/design.md` 다크 토큰 | 그대로 |
| 태블릿 | 목록 칸 맨 위, 좌우 24 | 홈 목록 칸 머리와 같은 여백 | 그대로 |
| 문구 | 해요체, 질문 하나와 할 일 하나 | 토스 UX 라이팅(`korean-copy-skills`) | humanizer·맞춤법 확인 |

고칠 것: 없음. 아이콘 하나를 더했다.
