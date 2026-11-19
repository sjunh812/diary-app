# 데이터 모델: 목록 미리보기 줄바꿈과 일기 전체 보기

일기 DB 스키마는 바꾸지 않는다.

## PreviewLength

| 값 | 저장 id | 뜻 |
| --- | --- | --- |
| `THREE_LINES` | `three` | 기본. 줄바꿈은 지키고 빈 줄은 없앤 글을 3줄까지 |
| `FIVE_LINES` | `five` | 3줄과 같은 규칙으로 5줄까지 |
| `FULL` | `full` | 줄바꿈과 띄어쓰기는 쓴 그대로, 연달아 빈 줄은 하나, 상한 없이 끝까지 |

모르는 id는 `THREE_LINES`로 읽는다.

## ListPreferences (설정, 백업에 들어감)

| 칸 | 기본 | 저장 키 | 백업(`ManifestSettings`) |
| --- | --- | --- | --- |
| `listStyle` | `FEED` | `list_style`(있음) | `listStyle`(있음) |
| `previewLength` | `THREE_LINES` | `preview_length` | `previewLength: String?` |
| `showFormatting` | `false` | `show_list_formatting` | `showListFormatting: Boolean?` |
| `previewChosen` | `false` | `preview_chosen` | 넣지 않음(기기 사정) |
| `legacyChecked` | `false` | `legacy_preview_checked` | 넣지 않음(기기 사정) |
| `favoritesOnly` | `false` | `favorites_only`(있음) | `favoritesOnly`(있음) |

- `showFormatting`은 저장 값이다. 실제로 서식을 그리는지는 `showFormatting && Plus`다(FR-014: Plus가 끝나도 값은 지우지 않음).
- 복원: 백업 칸이 null이면 복원 기기의 값을 그대로 둔다. 값이 있으면 `listStyle`과 같은 규칙으로 덮는다.

## 옛 설정 판단 (기기에만)

`ListPreferences.legacyChecked`(`legacy_preview_checked`)가 옛 `pref.xml`의 `skip_note`를 판단했는지 적는다. 한 번만.

판단(`LegacyListPreview`, 시작 화면 동안): checked가 없고 `previewChosen`이 거짓이면 `pref.xml`을 읽는다. `skip_note == false`면 `previewLength = FULL`, 팁 `HOME_FULL_PREVIEW_KEPT`를 연다. 파일이 없거나 읽지 못하면 아무것도 바꾸지 않는다. 어느 경우든 checked를 적는다. iOS는 늘 checked로 본다.

## 팁 기록 (`WelcomeStore`, 기기에만)

| 키 | 뜻 |
| --- | --- |
| `tips_seen`(있음) | 닫았거나 할 일을 한 팁 |
| `last_tip_day: String?` | 어떤 팁이든 마지막으로 보이기 시작한 날(기기 시간대 날짜) |
| `clipped_opens: Int` | 3줄 보기에서 잘린 일기를 상세로 연 횟수. 3에 닿으면 `HOME_FULL_PREVIEW` 조건 |
| `tips_opened: Set<String>` | 일이 생겨 열린 팁(`ContextTip.byEvent`) |
| `last_tip: String?` | 그날 보인 팁 |

새 `ContextTip`: `HOME_FULL_PREVIEW`("home_full_preview"), `HOME_FULL_PREVIEW_KEPT`("home_full_preview_kept").

규칙: 같은 날 이미 다른 팁이 보였으면 오늘은 새 팁을 보이지 않는다. 이미 보이던 팁은 그날 안에서 계속 보인다. 목록 위 자리는 백업 카드 > 옛 설정 이음 팁 > 전체 보기 팁 > 목록 보기 팁 순서다.

## 목록 줄 미리보기 (`EntryRow`, 불변)

| 칸 | 뜻 |
| --- | --- |
| `preview: String` | [줄 규칙](contracts/preview-text.md)을 거친 글. 피드·카드·격자·휴지통·검색이 함께 써서 타입은 그대로 둔다 |
| `previewLines: Int` | 3, 5 또는 끝까지(`PreviewText.UNLIMITED`). 검색 결과는 3 |
| `previewSpans: List<RichSpan>` | Plus 서식 보기일 때 `preview` 기준 서식. 아니면 빈 목록 |

잘렸는지는 화면이 글을 배치한 결과(`hasVisualOverflow`)로 알고 누를 때만 읽는다. 일기 내용이라 `toString`에 담지 않는다(헌법 II).

## PreviewMode

`PreviewMode(length, formatting)`. 홈 화면 모델이 `ListPreferences`와 확정된 요금제로 만들고(`PreviewMode.of`), 저장소 `timeline(favoritesOnly, modes)`에 흐름으로 넘긴다. 바뀌면 저장소가 같은 Pager를 무효화해 보던 자리 근처에서 다시 읽는다.
