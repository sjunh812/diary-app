# 데이터 모델: 기분 15종과 기분 테마

일기 DB 스키마는 바꾸지 않는다. 기분 칸은 이미 글자 id(`entry.mood`, `draft.mood` TEXT)다. 바뀌는 것은 `Mood` 정의, 설정 값 넷, 백업 목록 파일의 형식 번호 규칙과 설정 칸 넷, 요금제 칸 하나다.

## Mood (`shared/core/model/.../Mood.kt`)

| id | 이름(한국어) | 레거시 번호 | 묶음(`tone`) | 화면 순서 |
| --- | --- | --- | --- | --- |
| `excited` | 설렘 | 없음 | GOOD | 1 |
| `good` | 좋음 | 7 | GOOD | 2 |
| `laugh` | 웃음 | 4 | GOOD | 3 |
| `proud` | 뿌듯함 | 없음 | GOOD | 4 |
| `calm` | 편안함 | 없음 | GOOD | 5 |
| `angry` | 화남 | 0 | HARD | 6 |
| `annoyed` | 짜증 | 없음 | HARD | 7 |
| `sick` | 아픔 | 3 | HARD | 8 |
| `tired` | 피곤함 | 8 | HARD | 9 |
| `sad` | 슬픔 | 2 | HARD | 10 |
| `anxious` | 불안 | 없음 | HARD | 11 |
| `bad` | 나쁨 | 6 | HARD | 12 |
| `cool` | 쿨 | 1 | NEUTRAL | 13 |
| `meh` | 보통 | 5 | NEUTRAL | 14 |
| `bored` | 심심함 | 없음 | NEUTRAL | 15 |

- 규칙: 옛 9종의 id와 레거시 번호는 바꾸지 않는다(`StoredIdsTest`). 새 6종은 `legacyCode = null`이고 `fromLegacyCode`로 나오지 않는다.
- `entries` 차례는 옛 9종 뒤에 새 6종을 붙인다(옛 앱 번호 차례를 그대로 둠). 화면은 `ordered`만 쓴다.
- `ordered` 15칸이 5칸 3줄로 놓인다(US1-1). 묶음(GOOD → HARD → NEUTRAL)마다 한 덩어리이고 묶음 안은 색 차례(무지개)다. 고급 통계 기준 시트가 묶음별로 걸러도 같은 차례다(2026-11-27 사용자 지적). `MoodTest`가 "모두 한 번"을 본다.
- 묶음: 설렘·뿌듯함·편안함은 GOOD, 불안·짜증은 HARD, 심심함은 NEUTRAL(US2-2). 쿨·보통은 그대로 NEUTRAL.
- `Mood.LEGACY`(옛 9종 id 집합)를 두어 백업 형식 판단(아래)에 쓴다.

## 기분 색 (`shared/core/designsystem/.../theme/DiaryColors.kt` `DiaryMoodColors`)

15칸, 값은 `docs/design.md` 15색 표. 라이트·다크 같은 값. `lineOn(mood)`는 `#1A1C20`과 흰색 가운데 대비가 큰 쪽(결과는 나쁨만 흰색, research 6절). 디자인 시스템 모듈은 `Mood`를 모르므로 칸 이름으로 받고, `shared/core/ui/.../MoodWeather.kt`의 `DiaryMoodColors.of(mood)`가 잇는다.

## MoodTheme (`shared/core/model/.../MoodTheme.kt`, 새 파일)

| id | 이름 | 요금 | 질감 | 바탕 |
| --- | --- | --- | --- | --- |
| `basic` | 기본 | 무료 | 없음 | 원 |
| `shape` | 도형 | 무료 | 없음 | 제 모양 |
| `gouache` | 과슈 붓 | 무료 | `brush` | 원 |
| `line` | 테두리 | 무료 | 없음 | 원 |
| `dot` | 도트 | 무료 | 없음 | 원 |
| `bubble` | 비눗방울 | Plus | 없음 | 원 |
| `doodle` | 말랑 낙서 친구 | Plus | 없음 | 제 모양 |
| `veggie` | 채소·과일 | Plus | 없음 | 제 모양 |
| `clay` | 클레이 얼굴 | Plus | `grain` | 원(얼굴) |

- `DEFAULT = BASIC`. 테마 화면 차례는 위 표 차례다(사용자 결정, 무료·Plus 묶음 제목 없이 Plus 칩만). 도형은 "스티커 효과"(흰 테두리와 그림자, 종이 결)를 켜고 끄는 값 `moodThemeShapeSticker`(Boolean, 처음 값 false)를 따로 둔다(FR-005).
- 질감·바탕 칸은 Figma 시안을 보고 디자인 단계에서 확정한다(이 표는 시안 이름에서 짐작한 초안).
- id는 저장 값이라 정한 뒤 바꾸지 않는다(`StoredIdsTest`에 더함).

## 클레이 꼴

| 칸 | 값(id) | 처음 값 |
| --- | --- | --- |
| 머리 모양 | `bob`(단발), `long`(긴 생머리), `short`(숏컷), `dandy`(댄디컷), `part`(가르마), `bun`(똥머리), `ponytail`(포니테일), `curly`(곱슬), `buzz`(버즈컷) | `bob` |
| 피부톤 | `skin1`~`skin6`(밝은 쪽부터) | Figma "밝음" 단계(id는 디자인에서 확정) |
| 안경 | 켬·끔 | 끔 |

머리색은 칸이 없다. 늘 기분 색이다(FR-005a).

## MoodLook (설정, `shared/core/model/.../MoodTheme.kt`)

```text
MoodLook(
  themeId: String?,      // MoodTheme.id. 없으면 기본
  clayHair: String?,     // 없거나 모르면 bob
  claySkin: String?,     // 없거나 모르면 처음 값
  clayGlasses: Boolean?  // 없으면 끔
)
```

- 원문 그대로 든다. 모르는 id는 기본으로 보이고 저장값은 사용자가 다시 고를 때까지 지우지 않는다(스펙 010 `ThemeChoice`와 같음).
- `theme`: 아는 id면 그 테마, 아니면 기본.
- `shownTheme(plusAllowed)`: Plus 테마인데 `plusAllowed`가 거짓이면 기본.
- `withTheme(t)`, `withClay(hair, skin, glasses)`: 바꾼 설정. 쓰기는 `SettingsRepository.updateMoodLook(transform)` 한 번.
- `withoutPlusTheme()`: Plus 테마면 `themeId = basic`, 클레이 꼴은 그대로.

## 설정 저장 (`shared/core/datastore/.../SettingsStore.kt`)

| 키 | 값 | 없을 때 |
| --- | --- | --- |
| `mood_theme` | `MoodTheme.id` | 기본 |
| `clay_hair` | 머리 모양 id | `bob` |
| `clay_skin` | 피부톤 id | 처음 값 |
| `clay_glasses` | Boolean | 끔 |

`setOrRemove`로 쓰고, 기본을 고르면 `mood_theme`에 `basic`을 적는다(다른 기기에 복원해도 기본으로 돌아오게, 스펙 010 `themeColor`와 같은 까닭).

## 요금제 (`shared/core/model/.../Plan.kt`)

`moodThemes: Boolean` 칸을 더한다. `FREE`·`AD_FREE` 거짓, `PLUS` 참.

## Plus 꾸미기 되돌리기 (`shared/core/data/.../PlusLookReset.kt`, `CustomThemeReset.kt`를 넓혀 이름을 바꿈)

```text
확정된 요금제(null이면 아무것도 안 함) + 설정
  → !plan.customTheme 이고 직접 고른 색이 있으면 theme.withoutCustom()
  → !plan.moodThemes 이고 Plus 테마를 쓰면 moodLook.withoutPlusTheme()
  → 바뀐 것이 있을 때만 씀(테마 색은 updateTheme, 기분 테마는 updateMoodLook)
```

## Plus 혜택 (`shared/feature/plus/.../PlusBenefit.kt`)

`MOOD_THEME(core = false)`를 `THEME` 다음, `CHECKLIST` 앞에 더한다(열린 결정 2를 권장대로 갈 때). 아이콘은 기존 `DiaryIcons`에서 고르고, 없으면 Lucide에서 하나 더한다.

## 백업 목록 파일 (`shared/core/backup/.../ManifestCodec.kt`)

### 형식 번호

| 상수 | 지금 | 바뀐 뒤 |
| --- | --- | --- |
| `FORMAT_VERSION`(읽을 수 있는 가장 새 형식) | 3 | 4 |
| 드라이브에 쓰는 번호 | 늘 3 | 새 기분 일기가 있으면 4, 없으면 3 |
| 묶음 파일에 쓰는 번호(`BUNDLE_FORMAT_VERSION`) | 늘 2 | 새 기분 일기가 있으면 4, 없으면 2 |
| `MAX_DB_VERSION` | 2 | 2(스키마 그대로) |

- "새 기분 일기": 스냅샷 사본에서 `mood IS NOT NULL AND mood NOT IN (Mood.LEGACY)`인 줄. 휴지통 일기 포함. `Snapshot.newMoods: Int`로 센다(`SnapshotWriter.write`).
- 드라이브 속성 `diary.format`도 같은 번호를 적는다.
- 형식 4의 읽는 방법은 3(드라이브 gzip)·2(묶음 원본)와 같다. 번호만 "새 기분을 안다"는 뜻을 더한다. `encoding`이 있는 묶음 파일을 받지 않는 규칙도 그대로다.

### 설정 칸 (`ManifestSettings`, 모두 있어도 그만인 칸)

| 칸 | 값 | 없을 때(옛 백업) |
| --- | --- | --- |
| `moodTheme` | `MoodTheme.id` | 복원 기기 값 그대로 |
| `clayHair` | 머리 모양 id | 그대로 |
| `claySkin` | 피부톤 id | 그대로 |
| `clayGlasses` | Boolean | 그대로 |

적용(`BackupSettingsBridge.applyMoodLook`): 아는 값만, 한 번에 쓴다. 모르는 테마 id는 건너뛴다. Plus가 아닌 기기에서는 값이 들어가도 보이는 테마는 기본이고, 확정된 요금제가 무료면 `PlusLookReset`가 저장값도 기본으로 고친다.

## 화면 상태

- `MoodThemeUiState(look: MoodLook, canPlus: Boolean?, preview: MoodTheme?, pendingTheme: MoodTheme?, samples: List<MoodPreviewSample>)`. `canPlus`가 null이면 요금제를 아직 모름(잠금 표시를 하지 않고 누르면 기다림).
- 미리보기 시트의 일기 몇 줄(`MoodPreviewSample(date, mood, firstLine)`, 불변 값)은 기기 안 최근 일기 3편이고 없으면 견본 문구다. 기기 밖으로 나가지 않는다(헌법 II).
- 고른 테마는 `DiaryApp`이 `LocalMoodLook`(`shared/core/ui`)으로 내려 주고 `MoodFace`가 읽는다. 테마 미리보기는 같은 `CompositionLocal`을 그 칸 안에서만 바꿔 그린다.
