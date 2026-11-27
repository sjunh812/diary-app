# 조사: 기분 15종과 기분 테마

스펙 013 계획의 근거다. 코드는 2026-11-27 `feat/mood-themes`(`3eeda144`)를 읽었고, 바깥 근거는 공식 문서·공식 저장소에서 확인한 것만 적었다. 확인하지 못한 것은 그렇다고 적었다. Figma 시안은 이번 조사에서 열지 않았다(경로 수, 질감 효과의 종류와 값은 확인하지 못함).

## 1. 기분이 나열되는 곳

기분은 `Mood` 하나로 정의되고, 화면 순서는 `Mood.ordered`, 저장은 소문자 `id`다. 아래는 `grep`으로 찾은 곳 전부다. "고침"은 이 스펙에서 손대야 하는지다.

| 자리 | 파일 | 하는 일 | 고침 |
| --- | --- | --- | --- |
| 정의 | `shared/core/model/src/commonMain/kotlin/org/sjhstudio/diary/core/model/Mood.kt` | 9종, `legacyCode: Int`, `ordered`, `fromId`, `fromLegacyCode` | 6종 더함. 새 6종은 레거시 번호가 없어 `legacyCode`를 `Int?`로 |
| 긍정·부정 묶음 | `shared/core/model/.../model/AdvancedStats.kt` `Mood.tone` | GOOD·NEUTRAL·HARD | US2-2대로 넓힘 |
| 기분 수 세기 | `shared/core/model/.../model/EntryViews.kt` `MoodCounts`, `mostFrequent` | 기간별 개수, 많은 기분 | 없음(목록을 돌기만 함) |
| 찾기 조건 | `shared/core/model/.../model/SearchFilter.kt` | `moods: Set<Mood>` | 없음 |
| 임시 저장 | `shared/core/model/.../model/Draft.kt`, `shared/core/database/.../DraftEntity.kt`(`mood: String?`) | 글자 id로 저장 | 없음 |
| DB 변환 | `shared/core/database/src/commonMain/kotlin/org/sjhstudio/diary/core/database/Converters.kt` | `Mood.fromId(mood)`, `mood?.id` | 없음. 모르는 id는 null로 읽고, 저장하면 null로 씀(아래 2절 위험) |
| 저장소 | `shared/core/data/.../diary/DefaultEntryRepository.kt`, `DefaultStatsRepository.kt`, `EntryRows.kt` | `fromId`, `Mood.entries.associateWith { 0 }` | 없음 |
| 레거시 이전 | `shared/core/migration/.../LegacyEntryMapper.kt` `moodOf`(0~8만), `LegacyArchive.kt` | 1.7.4 번호 → id | 없음. 시험에 "새 6종은 레거시 번호로 오지 않음"을 더함 |
| 저장 id 시험 | `shared/core/model/src/commonTest/.../StoredIdsTest.kt`, `MoodTest.kt` | id·번호 고정, 화면 순서에 모두 한 번 | 15종으로 고침 |
| 얼굴 | `shared/core/designsystem/.../icon/DiaryMoodFace.kt` | 9종 경로(44 칸), 크기별 벡터 캐시, `moodFaceLine` | 기본 테마로 옮기고 15종으로 |
| 얼굴·색·이름 잇기 | `shared/core/ui/src/commonMain/kotlin/org/sjhstudio/diary/core/ui/MoodWeather.kt` | `MoodFace`, `MoodFaceLine`, `Mood.label()`, `Mood.face()`, `moodColor`, `DiaryMoodColors.of` | 테마를 읽어 그리게 다시 짬. 나쁨만 흰 선인 규칙을 색 토큰으로 |
| 기분 색 | `shared/core/designsystem/.../theme/DiaryColors.kt` `DiaryMoodColors`(9칸, 라이트·다크 같은 값) | 면·점 | 15칸, `docs/design.md` 15색 |
| 고르기 격자 | `shared/core/ui/.../DiaryMoodGrid.kt` | 5칸 격자(`Mood.ordered.chunked(5)`), 쓰기·찾기 함께 | 15칸이면 3줄. 쓰기에서만 고를 때 움직임(US5) |
| 쓰기 | `shared/feature/write/.../WriteSheets.kt`, `WriteScreen.kt`, `WriteViewModel.kt` | 기분 시트·칩 | 격자를 그대로 써서 거의 없음. 시트 높이 확인 |
| 찾기 | `shared/feature/search/.../SearchSheets.kt`(`Mood.valueOf` 저장), `SearchScreen.kt`, `SearchViewModel.kt`(`Mood.entries`) | 여러 개 고름 | 없음(목록을 돎) |
| 돌아보기 | `shared/feature/insight/.../InsightScreen.kt`(연간 지도 색, 강조 얼굴), `InsightViewModel.kt` `MoodCounts.sorted()` | 막대·흐름 | 없음 |
| 고급 통계 | `shared/feature/insight/.../AdvancedStatsTone.kt` `ToneSheet`, 범례 `insight_adv_legend_neutral`(`%1$s·%2$s`) | 기준 시트, 범례 "보통·쿨" | 가운데 묶음이 셋(쿨·보통·심심함)이라 범례 문자열 자리를 셋으로 |
| 달력·상세·목록·휴지통·이전 읽기 | `CalendarScreen.kt`, `EntryDetailScreen.kt`, `DiaryEntryLine.kt`, `DiaryEntryCard.kt`, `TrashScreen.kt`, `LegacyReaderScreen.kt` | `MoodFace` 부름 | 없음(`MoodFace`가 테마를 읽음) |
| 공유 글 | `shared/core/ui/.../EntryShare.kt` | 기분 이름만 | 없음 |
| 앱 틀 | `shared/app/.../scaffold/AppScaffold.kt:482` | 화면 인자의 id → `Mood` | 없음 |
| 목록 보기 견본 | `shared/feature/settings/.../ListStyleSettingsScreen.kt:205` | `Mood.GOOD` | 없음 |
| 로띠 색 | `shared/core/ui/.../DiaryLottie.kt`(`@face`=좋음, `@moodA`=쿨, `@moodB`=웃음, `@moodC`=피곤함), `scripts/lottie/make_onboarding.py:240` `MOOD_SICK = [0.745, 0.914, 0.706, 1]`(옛 아픔 `#BEE9B4`를 직접 적음) | 온보딩·결제 그림 | 색이 바뀌어 그림 색이 함께 바뀜. `MOOD_SICK`은 자리표시로 바꾸고 다시 만듦 |
| 이름 문자열 | `shared/core/ui/src/commonMain/composeResources/values{,-ko,-ja,-b+zh+Hant,-es,-pt,-de,-fr}/strings.xml` `mood_*` | 8개 언어 | 6개씩 더함 |
| 성능 프로필 | `benchmarks/.../BaselineProfileGenerator.kt:154` `MOOD_CHIP` | 쓰기 기분 시트 열기 | 다시 만듦 |
| 업그레이드 시험 | `composeApp/src/androidTest/.../UpgradeVerificationTest.kt:45` | 1.7.4 번호 7 → 좋음 | 없음(그대로 통과해야 함) |
| 위젯 | 없음 | Glance·WidgetKit·App Group 코드가 저장소에 없다(`grep`으로 확인). PRD R2 일감 | 9절 |

정리: DB 스키마는 그대로다. 기분 칸이 글자 id라 새 값은 그냥 들어간다(`Mood.kt` 머리 주석의 설계 그대로). 바뀌는 것은 정의, 묶음, 색, 얼굴, 문자열, 백업 형식 번호다.

## 2. 백업 형식 번호와 옛 앱

### 지금

- `shared/core/backup/src/commonMain/kotlin/org/sjhstudio/diary/core/backup/ManifestCodec.kt`: `FORMAT_VERSION = 3`(드라이브), `BUNDLE_FORMAT_VERSION = 2`(파일로 내보내기), `MAX_DB_VERSION = 2`. `ManifestCodec.check`는 `formatVersion > FORMAT_VERSION`, `dbVersion > MAX_DB_VERSION`, 모르는 `encoding`이면 `NEEDS_NEWER_APP`이다.
- `NEEDS_NEWER_APP`은 `RestoreCatalog`에서 `BackupEntryState.NeedsNewerApp`, `RestorePlanner.readable`에서 `RestoreNeedsUpdate`가 되고 화면에 "앱을 업데이트하면 복원할 수 있어요"(`restore_needs_update`, 파일은 `file_read_needs_update`)로 나온다.
- 출시된 2.2.0(`v2.2.0`)의 `FORMAT_VERSION`은 2다. 형식 3은 `80a42e39`(2026-11-25)에서 생겼고 아직 태그에 없다. 2.2.x는 다음 판의 드라이브 백업을 이미 막고, 파일로 내보내기(형식 2)는 읽는다(`docs/backup.md` "스냅샷").
- 드라이브 목록 파일은 `BackupRunner`에서 `Manifest.FORMAT_VERSION`으로 쓰고 같은 값을 드라이브 속성 `diary.format`(`DriveLayout.KEY_FORMAT`)에도 적는다. 묶음 파일은 `BundleExporter`가 `BUNDLE_FORMAT_VERSION`으로 쓴다.
- 스냅샷은 `SnapshotWriter.write`가 `VACUUM INTO`로 뜬 사본에서 기기 것인 표를 비운 뒤 개수를 센다. 휴지통 일기도 사본에 든다.

### 막아야 하는 까닭

옛 앱의 복원은 스냅샷 줄을 글자 그대로 읽는다(`SnapshotReader`가 `mood`를 `text(5)`로). 합치면 `excited` 같은 id가 옛 앱 DB에 그대로 들어간다. 옛 앱은 그것을 `Mood.fromId`에서 null(기분 없음)로 보이고, 사용자가 그 일기를 고쳐 저장하면 `Converters`의 `mood = mood?.id`가 null을 써서 기분이 지워진다. 헌법 I에 걸린다.

### 결정: 내용이 요구하는 형식 번호를 쓴다

- 새 형식 4는 "기분 15종을 아는 앱"이다. 이 판의 앱은 4까지 읽는다(`FORMAT_VERSION = 4`가 "읽을 수 있는 가장 새 형식"이 된다).
- 쓰는 번호는 스냅샷 사본에 새 6종(옛 9종이 아닌 기분)이 든 일기가 있는지로 정한다. 휴지통 일기도 센다.
  - 드라이브: 있으면 4, 없으면 3.
  - 파일로 내보내기: 있으면 4, 없으면 2.
- 세는 곳은 `SnapshotWriter.write`의 사본이다. 살아 있는 DB에서 세면 세고 나서 뜨는 사이에 새 기분 일기가 저장될 수 있다. 사본에서 세면 담긴 내용과 번호가 늘 맞는다.

까닭:

1. 파일로 내보내기를 형식 2로 둔 것은 "옛 앱도 읽게"라는 정한 약속이다(`docs/backup.md`, `specs/003-backup-v2/contracts/backup-format.md` 128행). 늘 올리면 새 기분을 한 번도 안 쓴 사람까지 이 약속이 깨진다.
2. 같은 원칙이 이미 코드에 있다. `MAX_DB_VERSION`은 "임시 저장본은 스냅샷에 싣지 않아 그 표만 바뀐 DB 버전 3은 이 값을 올리지 않음"(`ManifestCodec.kt`)이다. 번호는 앱 판이 아니라 담긴 내용이 요구하는 것을 말한다.
3. 폰은 업데이트했고 태블릿은 아직인 사람이 새 기분을 쓰기 전까지는 서로 복원된다.
4. 비용은 사본에서 `SELECT COUNT(*) FROM entry WHERE mood IS NOT NULL AND mood NOT IN (옛 9종)` 한 번이다.

늘 올리는 안(드라이브 4, 묶음 4)도 따져 봤다. 코드는 한 줄 더 짧지만 1번 약속을 깨고, 막을 까닭이 없는 백업까지 막는다. 형식 3을 늘 올린 것은 gzip이라는 읽는 방식 자체가 바뀌어서였고, 이번은 내용에 따라 다르다.

이 판이 다섯 언어 판(형식 3이 처음 나가는 판)과 함께 나가든 그 뒤에 나가든 규칙은 같다. 형식 3까지만 아는 앱(형식 3을 실은 판)도 4는 막는다.

시험(`shared/core/backup/src/commonTest/.../`): 새 기분 없는 스냅샷 → 드라이브 3·묶음 2, 휴지통에만 새 기분 → 4, 살아 있는 일기에 새 기분 → 4, 형식 3까지만 아는 `check`(기존 `ManifestCodecTest`의 `formatVersion = 4` 줄)를 "이 판은 4를 OK, 5를 NEEDS_NEWER_APP"으로 고침, 드라이브 속성 `diary.format`이 목록 파일 번호와 같음. 옛 앱 쪽은 `v2.2.0`의 `check`가 2 초과를 막는 것을 시험 이름에 적고, 실제 확인은 `/backup-roundtrip`의 옛 앱 왕복으로 한다.

## 3. 설정 저장과 Plus가 끝날 때

### 지금(스펙 010 테마 색)

- 설정 모양: `shared/core/model/.../AppSettings.kt`(`theme: ThemeChoice`), 값: `ThemeColor.kt`(`ThemeChoice(colorId, customHex)`는 원문 문자열을 그대로 들고 모르는 이름은 기본으로 보임).
- 저장: `shared/core/datastore/.../SettingsStore.kt` 키 `theme_color`, `custom_theme_color`. 쓰기는 `SettingsRepository.updateTheme(transform)` 한 번으로(나눠 쓰면 잠깐 다른 색이 됨, `BackupSettingsBridge` 주석).
- 요금제: `Plan.customTheme`(`shared/core/model/.../Plan.kt`). 화면과 기능은 `PlanRepository`만 본다. `confirmed`는 재설치 직후 스토어 답이 오기 전 null이다.
- 보이는 값: `shared/core/ui/.../ThemeBrand.kt` `shownColor(customAllowed)`. Plus가 아니면 저장값과 상관없이 기본으로 그린다. `DiaryApp.kt`가 `plan.customTheme`(기기에 남긴 값)으로 첫 프레임부터 정한다.
- 되돌리기: `shared/core/data/.../CustomThemeReset.kt`가 `plans.confirmed.filterNotNull()`과 설정을 함께 보고, 확정된 요금제가 Plus가 아니면 저장값을 기본으로 고친다. `AppRootViewModel`이 `launchSafely { customTheme.watch() }`로 띄운다. 무료일 때 Plus 값이 든 백업을 복원해도 이 감시가 지운다.
- 백업: `ManifestSettings.themeColor`, `customThemeColor`(있어도 그만인 칸, 옛 앱은 `ignoreUnknownKeys`로 무시), `shared/app/.../backup/BackupSettingsBridge.kt`가 담고 적용한다. 모르는 값은 건너뛴다.

### 결정: 같은 꼴을 그대로 쓴다

- 설정 값 `MoodLook(themeId, clayHair, claySkin, clayGlasses)`를 `AppSettings.moodLook`에 둔다. 원문 문자열을 그대로 들고, 모르는 id는 기본으로 보인다(새 판에서 테마가 늘어도 옛 판이 저장값을 지우지 않음).
- 요금제 칸 `Plan.moodThemes`(Plus만 참).
- 보이는 테마: `MoodLook.shownTheme(plusAllowed)`. Plus 테마인데 Plus가 아니면 기본. `DiaryApp`에서 `plan.moodThemes`로 정해 첫 프레임부터 맞춘다.
- 되돌리기: `CustomThemeReset`를 "Plus 꾸미기 되돌리기" 하나(`PlusLookReset`)로 넓혀 테마 색과 기분 테마를 함께 본다. 감시를 둘 띄우면 같은 요금제 흐름을 두 번 모으고, 같은 규칙이 두 곳에 갈라진다. 확정된 요금제만 보는 규칙(산 사람의 값을 지우지 않음)은 그대로다.
- Plus가 끝나면 고른 테마만 기본으로 고친다. 클레이 머리·피부톤·안경 값은 남긴다. 클레이를 쓰지 않으면 보이는 곳이 없고, 다시 Plus가 되어 클레이를 고르면 고르던 꼴이 돌아와 US4-3 "다시 고를 수 있다"에 맞는다. 테마 색에서 직접 고른 색을 지운 것은 그 값이 곧 "쓰는 색"이었기 때문이다.
- 백업: `ManifestSettings.moodTheme`, `clayHair`, `claySkin`, `clayGlasses`(모두 있어도 그만인 칸). 적용은 테마 색처럼 한 번에 쓴다. Plus가 아닌 기기에서 Plus 테마를 복원하면 값은 들어가지만 보이는 테마는 기본이고, 확정된 요금제가 Plus가 아니면 `PlusLookReset`가 기본으로 고친다(스펙 010 US6-4와 같은 길).

## 4. 테마 얼굴 그리기

### 지금

`DiaryMoodFace`는 44 칸 SVG 경로 글자를 `addPathNodes`로 읽어 `ImageVector`를 만들고 `(얼굴, 크기)`마다 캐시한다. 선 굵기가 크기마다 달라서다. `MoodFace`가 원을 기분 색으로 칠하고 그 위에 `ColorFilter.tint`로 선 색을 입힌다. 앱 아이콘(`DiaryIcons.kt`)도 Lucide SVG 경로를 그대로 옮긴 `ImageVector`다.

### 레퍼런스

| 출처 | 확인한 것 |
| --- | --- |
| Now in Android `2982e63` | 아이콘을 코드 `ImageVector`로 둔다(`core/designsystem/.../icon/NiaIcons.kt`, GitHub 트리에서 확인) |
| KotlinConf 앱 `146e891` | 공유 아이콘을 `app/shared/src/commonMain/composeResources/drawable/`의 벡터 XML 243개로 둔다(GitHub 트리에서 확인, 다크용 `drawable-dark`도 있음) |
| Android `RuntimeShader`(AGSL) | API 33부터다. 우리 `minSdk`는 24라 질감을 셰이더로 하면 옛 기기에 다른 길이 또 필요하다 |

CMP 리소스의 `painterResource`가 벡터 XML을 첫 프레임 전에 동기로 읽는지는 확인하지 못했다.

### 결정: Figma SVG → 생성 스크립트 → Kotlin 경로 코드

- 원본은 Figma 시안에서 내보낸 SVG다. `scripts/mood/make_mood_art.py`(`python3 -I`, 로띠 스크립트와 같은 자리)가 SVG를 읽어 테마마다 Kotlin 파일 하나(`shared/core/designsystem/.../mood/art/MoodArt<테마>.kt`)를 만든다. 손으로 고치지 않고, 같은 SVG로 다시 돌리면 같은 파일이 나와야 한다(시험).
- 색은 로띠의 `@accent` 자리표시와 같은 방식으로 자리를 둔다: 기분 색 값 → `MOOD` 자리, `#1A1C20` → `LINE` 자리, 그 밖(채소 잎·꼭지, 클레이 피부톤, 볼 터치)은 고정 색. 규칙은 [contracts/mood-art.md](contracts/mood-art.md).
- 런타임은 지금 `DiaryMoodFace`와 같다. `(테마, 기분, 크기)`마다 `ImageVector`를 한 번 만들어 캐시하고 `rememberVectorPainter`로 그린다. 테마별 그림 표는 `by lazy`라 고른 테마만 읽힌다(테마 화면을 열 때만 나머지를 읽음).
- 기본 테마도 같은 생성기로 만든다(FR-005 "지금 얼굴을 다듬고 15종으로"). 지금 `DiaryMoodFace`의 9종 경로는 Figma "기분 칸"을 옮긴 것이라 시안에서 다듬은 판이 원본이 된다. 테마마다 길이 둘이면 크기별 선 굵기(`moodFaceLine`) 같은 규칙이 갈라진다. 다 옮기면 `DiaryMoodFace.kt`는 지운다.
- 원 바탕은 테마가 정한다. 기본·테두리·도트처럼 원 위에 그리는 테마는 원 자리를 그림의 첫 면으로 두고, 도형·채소·과일처럼 테두리가 원이 아닌 테마는 제 몸 모양이 바탕이다. 그래서 `MoodFace`의 `clip(CircleShape).background(…)`를 테마 그림 안으로 옮긴다.
- 클레이 얼굴은 겹으로 만든다: 머리(피부톤 고정 색), 표정 15, 머리 모양 9(채움은 `MOOD`), 안경 1. 15 × 9 × 6 × 2 = 1,620가지를 따로 그리지 않고 26겹으로 만든다.

까닭: 코드 경로는 리소스 읽기가 없어 목록·달력 첫 프레임에 빈 얼굴이 스칠 일이 없고(위 확인 못 한 점과 상관없이), 지금 코드와 같은 꼴이라 새로 배울 것이 없다(NIA와 같은 방식). KotlinConf처럼 벡터 XML로 두는 안은 위젯(Android 리소스)과 맞는 장점이 있어, 생성기가 나중에 같은 원본에서 XML도 낼 수 있게 한다(9절).

다른 안:

- 테마마다 PNG를 크기별로: 15 × 9 × 해상도 3벌 = 405장, 다크 판까지면 더 늘고, 기분 색이 바뀌면 다시 뽑아야 한다. 뺐다.
- 테마마다 로띠 파일: 움직임까지 한 번에 되지만 얼굴 135개 × 파일이고, 목록 18 칸마다 로띠 그리기는 무겁다. 움직임은 7절의 공통 움직임으로 한다.

### 질감(종이 결, 붓 결, 알갱이)

Figma 노이즈 효과는 SVG로 나오지 않는다. 코드에서는 바둑판처럼 이어 붙일 수 있는 작은 회색 PNG(64×64 안팎, 질감 종류마다 하나, 셋 정도)를 `ImageShader(TileMode.Repeated)`로 그림 모양 안에만 옅게 겹친다. 그림 하나에 그리기 한 번이 늘고, 비트맵은 앱 전체에서 하나를 나눠 쓴다. 셰이더(AGSL)는 위 표의 까닭으로 쓰지 않는다. 경로로 알갱이를 찍는 안은 경로 수가 크게 늘어 뺐다.

질감을 몇 크기부터 그릴지는 열린 결정 1이다.

### 성능과 크기 예산

- 목록 한 줄의 얼굴(18)은 `ImageVector` 캐시를 다시 쓰고, 고른 테마가 바뀔 때만 새로 만든다. 상태는 테마 id 하나라 `MoodFace`가 테마를 `CompositionLocal`로 읽어도, 테마를 바꿀 때 말고는 다시 그리지 않는다.
- SC-004: `benchmarks`의 `StartupBenchmark`·`ScrollBenchmark`를 기본 테마와 가장 무거운 테마(클레이 + 질감 있는 테마 하나)로 실기기 `benchmarkRelease`에서 재고, 프레임 시간 중앙값과 느린 프레임 비율이 기본보다 나쁘지 않아야 한다. 에뮬레이터 값은 비교에만 쓴다(`/cmp-architecture` 7절).
- 크기 예산: 9 테마 그림 코드의 경로 글자 합계 1MB 이하, Android 내려받기 크기 증가 500KB 이하(`apkanalyzer apk download-size`로 잼), iOS 앱 크기 증가 1MB 이하. 넘으면 Figma에서 점을 줄이거나 생성기에서 소수 자리를 줄인다. 한 상수가 JVM 상수 한도(64KB)를 넘지 않게 생성기가 경로를 나눈다. 아직 재지 않았다.

## 5. 결제 화면과 Plus 잠금

- 혜택: `shared/feature/plus/.../PlusBenefit.kt`. 핵심 넷(광고 없음, 사진 30장, 고급 통계, 테마 색)이 결제 화면 그림 `plus_benefits.json`의 아이콘 차례와 같다. 다른 혜택(체크리스트, 서식 그대로 보기)에서 들어오면 `enteredOther`가 그 혜택을 결제 화면에 한 줄 더 보인다(스펙 008 US6).
- 잠금: `SettingsEntries.kt`가 `PlusKey(PlusBenefit.THEME)`처럼 혜택을 넘겨 결제 화면을 연다.

결정(권장): `PlusBenefit.MOOD_THEME(core = false)`를 더하고, 테마 미리보기 시트의 [Plus 알아보기]는 `PlusKey(PlusBenefit.MOOD_THEME)`로 연다. 결제 화면에 "기분 테마" 줄이 체크와 함께 보이고(US4-4), 모든 혜택 시트에도 들어간다. 핵심 넷과 로띠는 그대로다. 핵심을 바꾸면 로띠를 다시 짜야 하고(`docs/design.md` 결제 화면), 핵심 넷을 정한 지 닷새다(2026-11-22). 핵심에 넣을지는 열린 결정 2다.

결제 뒤 적용(US4-2): 결제 화면이 결과를 돌려주지 않는다. 테마 화면은 결제 화면 아래 묶음에 남아 있으므로, 테마 화면 모델이 "Plus가 되면 적용할 테마"(`pendingTheme`)를 상태로 들고 요금제 흐름을 본다. 요금제가 Plus로 확정되면 그 테마를 저장하고 지운다. 결제를 취소하고 돌아오면 지운다. 화면 모델끼리 직접 잇지 않는다는 `/cmp-architecture` 4절에 맞고, 요금제의 주인은 `PlanRepository` 하나 그대로다. 앱이 그 사이 꺼지면 적용하지 않는다(테마는 다시 누르면 됨).

## 6. 기분 색과 선 색

- `DiaryMoodColors`를 15칸으로 바꾸고 값은 `docs/design.md` 15색이다. 라이트·다크가 같은 값이다(문서 그대로).
- 선 색 규칙: `#1A1C20`과 흰색 가운데 대비가 큰 쪽. 지금은 `MoodFace`가 `mood == Mood.BAD`로 정한다. 15색을 재면 나쁨만 흰색이 크고 나머지는 모두 어두운 선이 크다(아래). 그래서 결과는 문서의 "나쁨만 흰색"과 같고, 규칙을 색 토큰 옆(`DiaryMoodColors.lineOn`)에 두어 색이 바뀌어도 따라가게 한다.

| 기분 | 색 | `#1A1C20`과 | 흰색과 |
| --- | --- | --- | --- |
| 좋음 | `#FFC298` | 10.9 | 1.6 |
| 웃음 | `#FFDB67` | 12.7 | 1.3 |
| 설렘 | `#F6A0C1` | 8.7 | 2.0 |
| 뿌듯함 | `#67CC9A` | 8.7 | 2.0 |
| 쿨 | `#65D2D2` | 9.5 | 1.8 |
| 편안함 | `#C1AEED` | 8.6 | 2.0 |
| 보통 | `#CAC0AC` | 9.5 | 1.8 |
| 심심함 | `#ACB2B9` | 8.0 | 2.1 |
| 피곤함 | `#ABD9EC` | 11.3 | 1.5 |
| 아픔 | `#B4DA83` | 10.8 | 1.6 |
| 슬픔 | `#639BE1` | 5.9 | 2.9 |
| 불안 | `#C88BD1` | 6.6 | 2.6 |
| 짜증 | `#F8935D` | 7.6 | 2.3 |
| 화남 | `#E15A50` | 4.7 | 3.6 |
| 나쁨 | `#636F87` | 3.4 | 5.1 |

(WCAG 2 상대 휘도 식, 2026-11-27 계산.) 다크 바탕(`canvas #16171B`)에서 가장 낮은 것은 나쁨 3.5:1이라 바탕 원도 보인다. 라이트 바탕에서 웃음은 1.35:1로 원 테두리가 흐리지만, 지금 9색에서도 같은 자리이고 얼굴은 모양과 이름으로 구별한다(FR-006).

- 테마 그림의 `LINE` 자리도 같은 규칙을 쓴다. 테마가 선을 다른 색(예: 과슈의 진한 물감 색)으로 그리면 그 색은 고정 색이고, 대비는 디자인 점검에서 잰다.
- 색이 바뀌면 따라 바뀌는 곳: 돌아보기 막대·연간 지도, 달력 겹친 원, 기분 칸 바탕, 온보딩·결제 로띠(`DiaryLottie.kt` 자리표시). `make_onboarding.py`의 `MOOD_SICK`만 옛 값을 직접 적어 두어 자리표시로 바꾼다.

## 7. 고를 때 움직임(US5)

- 테마마다 로띠를 두지 않고 `graphicsLayer`의 크기·기울기·위치 하나로 움직인다(통통 튀기, 살짝 흔들기, 말랑하게 눌렸다 펴기 같은 몇 가지를 `DiaryMotion`에 두고 테마가 하나를 고름). 1초 안쪽, 한 번.
- `DiaryMoodGrid`는 쓰기(하나 고름)와 찾기(여러 개 고름)가 함께 쓴다. 움직임은 쓰기에서만이라 인자로 켠다.
- 동작 줄이기는 이미 있는 `rememberReduceMotion()`(`shared/core/ui/src/iosMain/.../ReduceMotion.ios.kt`, `DiarySkeleton.kt`가 씀)을 따른다.
- 쓰기 시트는 누르면 바로 닫힌다(`docs/design.md` 기분 고르기). 닫히기 전에 움직임이 보이려면 닫힘을 움직임만큼 늦추거나, 칩 자리의 얼굴에서 움직이게 해야 한다. 어느 쪽인지는 Figma 프로토타입에서 정한다(tasks T003).

## 8. 문자열(8개 언어)

- 기분 이름 6개: `shared/core/ui/src/commonMain/composeResources/values*/strings.xml` 8벌에 `mood_excited`, `mood_proud`, `mood_calm`, `mood_bored`, `mood_anxious`, `mood_annoyed`.
- 테마 이름 9개, 클레이 머리 9개·피부톤 6개·안경, 테마 화면 제목·안내, 미리보기 시트, 설정 줄: `shared/feature/settings/.../composeResources/values*/strings.xml` 8벌.
- Plus 혜택 "기분 테마": `shared/feature/plus/.../composeResources/values*/strings.xml` 8벌.
- 고급 통계 범례 `insight_adv_legend_neutral`을 자리 셋(`%1$s·%2$s·%3$s`)으로: `shared/feature/insight/.../composeResources/values*/strings.xml` 8벌. `stringResourceCheck`가 언어마다 자리가 같은지 본다.
- 다섯 언어(번체 중국어, 스페인어, 포르투갈어, 독일어, 프랑스어) 기분 이름은 짧은 한 낱말이 칸(5칸 격자)에 들어가야 한다. 긴 이름은 `MoodName`이 한 낱말이면 80%까지 줄이고, 여러 낱말이면 두 줄까지다.

## 9. 위젯

저장소에 위젯 코드가 없다. Glance·WidgetKit·App Group을 찾았지만 `shared/feature/welcome`의 문구 하나뿐이다. PRD는 위젯을 R2 일감으로 둔다(`docs/PRD.md` 75행). 그래서 이 스펙에서 위젯에 할 일은 없고, 위젯 스펙이 이어받을 것만 정한다.

- Glance는 Compose `ImageVector`를 그리지 못하고 Android 리소스나 비트맵을 받는다. WidgetKit은 SwiftUI라 공유 Kotlin 그림을 쓰지 못한다.
- 그래서 원본 SVG를 저장소에 두고, 생성기가 나중에 Android 벡터 XML과 iOS 에셋도 같은 원본에서 낼 수 있게 짠다. 고른 테마 id는 App Group 스냅샷(JSON)에 싣는다.

스펙 Edge Cases의 위젯 줄을 이 스펙 범위에서 뺄지는 열린 결정 3이다.

## 10. 설정 자리

스펙은 "설정 > 꾸미기 > 기분 테마"라 썼지만 설정에 "꾸미기" 묶음은 없다. 가까운 묶음은 "화면"(`settings_group_style`)이고 글꼴, 글자 크기, 테마, 목록 보기 순서다(`SettingsScreen.kt` 209행). 권장은 "화면" 묶음의 "테마" 바로 아래 "기분 테마" 줄이다. 묶음 이름을 "꾸미기"로 바꾸거나 새로 만들지는 열린 결정 4다.

## 11. 테마 개수와 이름

테마는 9벌이고 고르는 값(id)도 9개다. 종이 오리기는 테마로 두지 않고 도형의 "스티커 효과" 설정으로 둔다(사용자 결정). 이름은 사용자가 확정했다.

| 한국어 | 영어 | 일본어 | 번체 중국어 | 스페인어 | 포르투갈어 | 독일어 | 프랑스어 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 기본 | Classic | ベーシック | 基本 | Clásico | Clássico | Klassisch | Classique |
| 테두리 | Outline | ふちどり | 描邊 | Contorno | Contorno | Umriss | Contour |
| 도형 | Shapes | かたち | 圖形 | Formas | Formas | Formen | Formes |
| 도트 | Pixel | ドット | 像素 | Píxel | Pixel | Pixel | Pixel |
| 과슈 붓 | Gouache | ガッシュ | 水粉 | Gouache | Guache | Gouache | Gouache |
| 비눗방울 | Bubble | シャボン玉 | 泡泡 | Burbujas | Bolhas | Seifenblasen | Bulles |
| 말랑 낙서 친구 | Doodle Pals | らくがき | 塗鴉夥伴 | Garabatos | Rabiscos | Kritzelfreunde | Gribouillis |
| 클레이 얼굴 | Clay Faces | ねんど | 黏土臉 | Caras de arcilla | Rostos de massinha | Knetgesichter | Visages en pâte |
| 채소·과일 | Fruit & Veg | やさい・くだもの | 蔬果 | Frutas y verduras | Frutas e legumes | Obst & Gemüse | Fruits et légumes |
| 스티커 효과(도형 설정) | Sticker effect | ステッカー風 | 貼紙效果 | Efecto pegatina | Efeito adesivo | Sticker-Effekt | Effet autocollant |

새 기분 6종 이름(사용자 확인 2026-11-27):

| 한국어 | 영어 | 일본어 | 번체 중국어 | 스페인어 | 포르투갈어 | 독일어 | 프랑스어 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 설렘 | Excited | ワクワク | 心動 | Ilusionado | Empolgado | Begeistert | Emballé |
| 뿌듯함 | Proud | 誇らしい | 自豪 | Orgulloso | Orgulhoso | Stolz | Fier |
| 편안함 | Calm | 穏やか | 平靜 | En calma | Sereno | Entspannt | Serein |
| 심심함 | Bored | 退屈 | 無聊 | Aburrido | Entediado | Gelangweilt | Ennuyé |
| 불안 | Anxious | 不安 | 不安 | Ansioso | Ansioso | Unruhig | Anxieux |
| 짜증 | Annoyed | イライラ | 煩躁 | Molesto | Irritado | Genervt | Agacé |

## 열린 결정

| 번호 | 무엇 | 권장 | 다르게 갈 조건 |
| --- | --- | --- | --- |
| 1 | 질감을 몇 크기부터 그리나 | 28 이상(카드·쓰기 시트·테마 화면)만. 18·24(목록·통계)는 질감 없이 | 디자인 점검에서 18 칸 캡처에 질감 차이가 보이면 모든 크기 |
| 2 | "기분 테마"를 결제 화면 핵심에 넣나 | 다른 혜택(`core = false`)으로. 테마 화면에서 들어오면 결제 화면에 그 줄이 보임 | 핵심에 넣으면 무엇을 뺄지 정하고 `plus_benefits.json`을 다시 만듦 |
| 3 | 위젯 줄을 이 스펙에서 뺄지 | 뺀다(위젯이 아직 없음). 원본 SVG와 테마 id만 남겨 위젯 스펙이 이어받음 | 위젯을 이 판에 함께 내기로 하면 별도 스펙 |
| 4 | 설정 줄 자리 | "화면" 묶음, "테마" 아래 | "꾸미기" 묶음을 새로 두기로 하면 다른 줄도 옮길지 함께 |
| 5 | 다섯 언어 기분·테마 이름 | 개발 때 초안을 넣고(`stringResourceCheck`가 빈 칸을 막음) 출시 전에 사용자가 확인 | 확인에서 고칠 말이 나오면 같은 판 안에서 고치고 다시 확인 |
| 6 | 실을 판 | 다섯 언어 판 다음 판 | 같은 판이어도 2절 규칙은 같고, 업데이트 안내 문구만 합침 |
