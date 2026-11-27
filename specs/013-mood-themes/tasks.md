# 작업 목록: 기분 15종과 기분 테마

**입력**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/mood-art.md](contracts/mood-art.md), [quickstart.md](quickstart.md)

**시험**: 스펙 FR-009가 백업·복원·이전 시험을 요구하고 `CLAUDE.md`가 저장·복원·이전에 실패 먼저 재현을 요구해서 시험 작업을 넣었다. 위험한 곳(백업 번호, Plus 되돌리기, 설정 복원)은 시험을 먼저 쓰고 실패를 본 뒤 고친다.

**형식**: `- [ ] T번호 [P] [US번호] 설명과 파일 경로`. `[P]`는 다른 파일이라 함께 할 수 있는 것.

경로 줄임: `model` = `shared/core/model/src/commonMain/kotlin/org/sjhstudio/diary/core/model`, `ds` = `shared/core/designsystem/src/commonMain/kotlin/org/sjhstudio/diary/core/designsystem`, `ui` = `shared/core/ui/src/commonMain/kotlin/org/sjhstudio/diary/core/ui`, `backup` = `shared/core/backup/src/commonMain/kotlin/org/sjhstudio/diary/core/backup`, `settings` = `shared/feature/settings/src/commonMain/kotlin/org/sjhstudio/diary/feature/settings`. 각 `…Test`는 같은 모듈의 `src/commonTest`(화면 시험은 `src/iosTest`) 같은 패키지다.

## 1단계: 디자인 (Figma와 문서 먼저)

- [ ] T001 Figma "프로토타입 · 시안" 페이지에 "프로토타입 · 기분 테마" 구역을 만들고 흐름을 놓음: 쓰기 기분 시트 15칸 → 고르기 움직임, 설정 > 화면 > 기분 테마 → 무료 테마 고름 → 앱 전체 바뀜, Plus 테마 → 미리보기 시트 → 결제 화면 → 돌아와 적용, 클레이 꼴 고르기, Plus 끝남. 겹치는 카드 흐름까지 확정을 받기 전에는 다음으로 가지 않음
- [ ] T002 [P] 같은 구역에 고급 통계 기준 시트(세 묶음 15종)와 돌아보기 막대(새 15색)를 둠
- [ ] T003 쓰기 시트에서 "누르면 바로 닫힘"과 고르기 움직임을 어떻게 함께 보일지 프로토타입에서 정함(research 7절: 닫힘을 움직임만큼 늦출지, 칩 자리 얼굴이 움직일지)
- [ ] T004 Figma "디자인 시스템" 페이지에 기분 15색 변수(`mood/<id>`), 기분 얼굴 부품을 테마마다 15종 × 라이트·다크로 등록. 시안 SVG가 contracts/mood-art.md 2·3절 규칙(44 칸, 금지 요소 없음, 색 자리)을 지키게 고침
- [ ] T005 Figma "화면" 페이지: 쓰기 기분 시트 15칸(폰·태블릿·큰 글자), 설정 "화면" 묶음의 "기분 테마" 줄, 기분 테마 화면(라이트·다크·태블릿), 미리보기 시트, 클레이 꼴 고르기, Plus 끝난 상태, 결제 화면 "기분 테마" 줄, 고급 통계 기준 시트
- [ ] T006 `/figma-audit`로 규격을 재고 어긋난 부품은 메인 컴포넌트부터 고침, 페이지 전체를 훑음
- [ ] T007 `docs/design.md`: 기분 고르기 순서 15종, 기분 얼굴 표시 규칙(새 6종), 기분 테마 화면·미리보기 시트 줄, 질감 규칙(열린 결정 1), 선 색 규칙, 통계 "보통·쿨" → "쿨·보통·심심함", 칸 수 "9" 남은 곳(4·17·21·64·69·333·335행 근처)을 모두 고침
- [ ] T008 [P] `docs/business.md` 결제 화면 혜택(열린 결정 2), `docs/backup.md`·`specs/003-backup-v2/contracts/backup-format.md` 형식 4 규칙, `docs/migration.md` 새 6종은 레거시 번호 없음, `docs/DECISIONS.md`에 날짜와 까닭
- [ ] T009 디자인 점검 `specs/013-mood-themes/design-review.md`(TDS·SEED·HIG·M3, 16·24·28 크기에서 15종 구별을 흑백 캡처로, 다크 대비, 큰 글자, 다섯 언어 이름 길이)

## 2단계: 바탕 (모든 이야기가 기대는 것)

- [ ] T010 [P] `StoredIdsTest`·`MoodTest`(`shared/core/model/src/commonTest/kotlin/org/sjhstudio/diary/core/model/`)를 15종으로 먼저 고쳐 실패를 봄: 옛 9종 id와 번호 그대로, 새 6종 `legacyCode == null`, `fromLegacyCode(9..)` null, `ordered` 15칸 한 번씩, 차례가 data-model "Mood" 표와 같음
- [ ] T011 `model/Mood.kt`: 새 6종(`excited`, `proud`, `calm`, `bored`, `anxious`, `annoyed`), `legacyCode: Int?`, `ordered` 15칸, `LEGACY` 집합. 머리 주석 "9종"을 고침
- [ ] T012 [P] `shared/core/migration/src/commonTest/.../LegacyEntryMapperTest.kt`에 "0~8만 옛 9종으로, 새 6종은 나오지 않음" 줄을 더하고 `LegacyEntryMapper.kt`의 `moodOf`가 그대로 통과하는지 봄
- [ ] T013 [P] `ds/theme/DiaryColors.kt` `DiaryMoodColors` 15칸과 `lineOn`, 라이트·다크 값을 `docs/design.md` 15색 표로. `shared/core/designsystem/src/commonTest/.../theme/`에 "15색 값이 표와 같음", "lineOn은 나쁨만 흰색, 대비가 큰 쪽" 시험
- [ ] T014 `ui/MoodWeather.kt`: `Mood.label()`, `DiaryMoodColors.of` 15종, `MoodFace`의 `mood == Mood.BAD` 대신 `lineOn` 사용
- [ ] T015 [P] 기분 이름 6개를 `shared/core/ui/src/commonMain/composeResources/values/strings.xml`, `values-ko`, `values-ja`, `values-b+zh+Hant`, `values-es`, `values-pt`, `values-de`, `values-fr`에 더함(`mood_excited` … `mood_annoyed`). 한국어는 `korean-humanizer`·`korean-spell-check`. 다섯 언어는 초안(열린 결정 5)
- [ ] T016 [P] `scripts/mood/make_mood_art.py`와 시험 원본 `scripts/mood/fixtures/`(바른 원본 1, 금지 요소·다른 기분 색·`viewBox` 어긋남 각 1). `--check`는 다시 만든 결과가 생성 파일과 같은지 봄(contracts/mood-art.md 6절)
- [ ] T017 `ds/mood/MoodArt.kt`: `ArtLayer`, `Slot`(Mood·Line·Highlight·Skin·Fixed), `MoodArtSet`, `(테마, 기분 id, 크기)` 벡터 캐시, 질감 겹치기(`ImageShader(TileMode.Repeated)`, 질감 비트맵 하나를 나눠 씀). `shared/core/designsystem/src/commonTest/.../mood/MoodArtTest.kt`: 모든 테마가 15종을 주고 경로가 읽힘
- [ ] T018 `model/MoodTheme.kt`: `MoodTheme` 9개 id와 Plus 여부(data-model 표), 도형 스티커 효과 값, `ClayHair` 9, `ClaySkin` 6, `MoodLook`(원문 보관, `theme`, `shownTheme(plusAllowed)`, `withTheme`, `withClay`, `withoutPlusTheme`). `StoredIdsTest`에 테마·머리·피부톤 id 고정 시험
- [ ] T019 `model/AppSettings.kt` `moodLook`, `model/Plan.kt` `moodThemes`(PLUS만 참)
- [ ] T020 `shared/core/datastore/.../SettingsStore.kt` 키 `mood_theme`·`clay_hair`·`clay_skin`·`clay_glasses`, `shared/core/data/.../SettingsRepository.kt`·`DefaultSettingsRepository.kt` `updateMoodLook`, `shared/core/testing/.../FakeSettingsRepository.kt`. `SettingsStoreTest`·`DefaultSettingsRepositoryTest`: 저장·읽기, 모르는 id를 지우지 않고 다시 씀
- [ ] T021 `ui/MoodLook.kt`: `LocalMoodLook`(`staticCompositionLocalOf`), `MoodTheme` → `MoodArtSet` 잇기. `MoodFace`가 `LocalMoodLook`을 읽고 바탕 원을 테마 그림으로 옮김(research 4절)
- [ ] T022 `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/DiaryApp.kt`: `settings.moodLook.shownTheme(plan.moodThemes)`를 `LocalMoodLook`으로 내려 줌(첫 프레임부터, 테마 색과 같은 자리)

**여기까지 끝나면**: 기본 테마 하나로 15종이 앱 어디서나 그려진다.

## 3단계: US1 기분을 15가지에서 고른다 (P1)

**목표**: 쓰기 시트 15칸, 모든 화면에 새 기분. **독립 시험**: 새 일기에 설렘을 골라 저장하고 목록·상세·달력·찾기에서 본다.

- [ ] T030 [US1] Figma 기본 테마 15종을 SVG로 내보내 `design/mood-art/basic/`에 두고 생성기로 `ds/mood/art/MoodArtBasic.kt`를 만듦. 옛 9종이 `DiaryMoodFace`와 같은 표시(찌푸린 눈썹, 선글라스, 눈물…)를 지키는지 캡처로 견줌
- [ ] T031 [US1] `ds/icon/DiaryMoodFace.kt`를 쓰는 곳을 모두 `MoodArtBasic`으로 옮기고 파일을 지움, `moodFaceLine`은 `ds/mood/MoodArt.kt`로 옮김(`NoMoodFace`가 씀). 안 쓰는 import 정리
- [ ] T032 [US1] `ui/DiaryMoodGrid.kt`: 15칸 3줄, 머리 주석 "9개" 고침. 작은 폰(360dp)·큰 글자에서 쓰기 시트가 스크롤 없이 들어가는지 보고, 넘치면 `DiaryWriteSize.moodCellHeight`부터 고침
- [ ] T033 [P] [US1] `shared/core/ui/src/iosTest/.../MoodNameTest.kt`에 새 6종 이름(8개 언어)이 칸 폭에서 잘리지 않는지 더함
- [ ] T034 [P] [US1] `shared/feature/write/src/iosTest/kotlin/org/sjhstudio/diary/feature/write/MoodSheetUiTest.kt`(새 파일): 시트에 15칸, 누르면 들어가고 닫힘, 큰 글자에서 이름 두 줄까지
- [ ] T035 [P] [US1] `shared/feature/search/src/commonTest/.../SearchViewModelTest.kt`: 새 기분으로 거르기, 저장된 화면 상태 복원(`SearchSheets.kt`의 `Mood.valueOf`)
- [ ] T036 [P] [US1] `shared/core/data/src/commonTest/.../DiaryReadRepositoryTest.kt`: 새 기분 일기가 달력(`toDayMoods`)·찾기·통계 개수(`moodCounts`, 15칸 모두 0으로 시작)에 들어감
- [ ] T037 [P] [US1] `ui/preview/MoodWeatherPreviews.kt`: 15종, 한국어 먼저, 라이트·다크, 18·28·44 크기
- [ ] T038 [US1] `composeApp/src/androidTest/kotlin/org/sjhstudio/diary/UpgradeVerificationTest.kt`가 그대로 통과하는지(1.7.4 번호 7 → 좋음) 에뮬레이터에서 돌림

## 4단계: US2 통계가 15종을 읽기 쉽게 보여 준다 (P1)

**목표**: 돌아보기와 고급 통계가 15종을 다룸. **독립 시험**: 새 기분이 든 기간의 막대와 기준 시트를 본다.

- [ ] T040 [US2] `shared/core/model/src/commonTest/.../AdvancedStatsTest.kt`를 먼저 고쳐 실패를 봄: 설렘·뿌듯함·편안함 GOOD, 불안·짜증 HARD, 심심함 NEUTRAL(긍정·부정 비율에 들지 않음)
- [ ] T041 [US2] `model/AdvancedStats.kt` `Mood.tone` 15종
- [ ] T042 [US2] `shared/feature/insight/.../AdvancedStatsTone.kt`: 범례 가운데 묶음을 `Mood.entries.filter { it.tone == NEUTRAL }`의 `ordered` 차례 이름으로, `insight_adv_legend_neutral`을 자리 셋(`%1$s·%2$s·%3$s`)으로 8개 언어(`shared/feature/insight/src/commonMain/composeResources/values*/strings.xml`) 함께 고침
- [ ] T043 [P] [US2] `shared/feature/insight/src/commonTest/.../InsightViewModelTest.kt`·`AdvancedStatsViewModelTest.kt`: 새 기분이 든 기간에 있는 기분만 많은 순 막대, 연간 지도 색 15종
- [ ] T044 [P] [US2] `shared/feature/insight/src/iosTest/.../InsightSummaryUiTest.kt`: 기준 시트에 세 묶음 15종
- [ ] T045 [P] [US2] `shared/feature/insight/.../preview/AdvancedStatsPreviews.kt`·`InsightPreviews.kt`에 새 기분 든 견본

## 5단계: US3 기분 테마를 고른다 (P1)

**목표**: 무료 테마를 골라 앱 전체가 바뀜. **독립 시험**: 설정에서 도트를 고르고 홈·달력·돌아보기를 본다.

- [ ] T050 [P] [US3] 무료 테마 원본을 `design/mood-art/line/`, `shape/`(스티커 효과 겹 포함), `dot/`, `gouache/`에 두고 생성(`ds/mood/art/MoodArtLine.kt` 등). 질감 PNG(`paper`, `brush`)를 `design/mood-art/textures/`와 `shared/core/designsystem/src/commonMain/composeResources/drawable/mood_texture_*.png`에
- [ ] T051 [US3] 질감을 그리는 크기 기준(열린 결정 1 권장: 28 이상)을 `ds/mood/MoodArt.kt`에 두고 `MoodArtTest`에 크기별 시험
- [ ] T052 [US3] `shared/feature/settings/src/commonTest/.../MoodThemeViewModelTest.kt`(새 파일)를 먼저: 처음 값 기본, 무료 테마를 누르면 저장, 연타하면 마지막 값만, 모르는 저장 id는 기본으로 보이고 지우지 않음, 요금제를 모를 때 `canPlus == null`
- [ ] T053 [US3] `settings/MoodThemeViewModel.kt`: `MoodThemeUiState`(data-model "화면 상태"), `stateInSafely`, 쓰기는 `launchWrite`, `SettingsModule.kt`에 등록
- [ ] T054 [US3] `settings/MoodThemeSettingsScreen.kt`: 테마마다 이름·대표 얼굴 몇 개·무료/Plus 표시(`DiaryPlusChip`), 고른 테마 체크, 도형 줄에 "스티커 효과" 켜고 끄기. 원시 색·숫자 크기 없이 토큰만(`designTokenCheck`)
- [ ] T055 [US3] `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/navigation/ScreenKeys.kt` `MoodThemeSettingsKey`(`@SerialName` 붙임), `shared/app/.../settings/SettingsEntries.kt` 연결, 태블릿 설정 오른쪽 칸(`settingsPane`)
- [ ] T056 [US3] `settings/SettingsScreen.kt` "화면" 묶음 "테마" 아래 "기분 테마" 줄과 값(보이는 테마 이름). 열린 결정 4를 다르게 정하면 자리를 옮김
- [ ] T057 [P] [US3] 테마 이름·화면 문구를 `shared/feature/settings/src/commonMain/composeResources/values*/strings.xml` 8벌에. 한국어 검사기 두 개
- [ ] T058 [P] [US3] `settings/preview/MoodThemePreviews.kt`: 한국어 먼저, 라이트·다크, 기본·도트 고름, 태블릿 폭, 큰 글자
- [ ] T059 [P] [US3] `shared/feature/settings/src/iosTest/.../MoodThemeSettingsUiTest.kt`: 무료 테마를 누르면 체크가 옮겨 감, Plus 표시는 무료일 때만, 큰 글자에서 이름이 잘리지 않음
- [ ] T060 [US3] 백업 설정: `shared/app/src/commonTest/kotlin/org/sjhstudio/diary/app/backup/BackupSettingsBridgeTest.kt`를 먼저(새 백업 왕복, 옛 백업은 이 기기 값 그대로, 모르는 테마 id 건너뜀), 그다음 `backup/ManifestCodec.kt` `ManifestSettings`에 `moodTheme`·`clayHair`·`claySkin`·`clayGlasses`, `shared/app/.../backup/BackupSettingsBridge.kt` 담기·적용(한 번에 씀)
- [ ] T061 [US3] 통계 이벤트에 테마 id만(일기 내용·기분 기록 없음, 헌법 II). 쓰는 이벤트 이름은 기존 설정 이벤트 꼴을 따름

## 6단계: US4 Plus 테마를 미리 입혀 보고 산다 (P1)

**목표**: Plus 테마 미리보기, 결제 뒤 적용, Plus가 끝나면 기본. **독립 시험**: 무료에서 클레이를 눌러 미리보기 → 개발자 모드로 Plus → 적용 → 무료로 → 기본.

- [ ] T070 [P] [US4] Plus 테마 원본 `design/mood-art/bubble/`, `doodle/`, `veggie/`, 클레이 겹 `design/mood-art/clay/`(head, face 15, hair 9, glasses)와 질감 `grain`, 생성 `ds/mood/art/MoodArtBubble.kt` 등. 채소 잎·꼭지·씨와 클레이 피부톤은 고정 색(contracts 3·4절). 피부톤 6단계 이름과 처음 값("밝음")을 Figma에서 확인해 `model/MoodTheme.kt` `ClaySkin`에 맞춤
- [ ] T071 [US4] `MoodArtTest`에 클레이 9 × 6 × 2 조합이 모두 그려지고 머리 채움이 기분 색 자리인지
- [ ] T072 [US4] `shared/core/data/src/commonTest/.../CustomThemeResetTest.kt`를 `PlusLookResetTest.kt`로 넓혀 먼저: 확정된 무료면 Plus 테마 → 기본, 클레이 꼴은 남김, 요금제를 모르면 아무것도 지우지 않음, 테마 색 규칙(스펙 010 US6)은 그대로, 무료 기기에서 Plus 테마 백업 복원 → 기본
- [ ] T073 [US4] `shared/core/data/.../CustomThemeReset.kt` → `PlusLookReset.kt`(이름 바꿈, 테마 색과 기분 테마를 함께), `shared/app/.../AppRootViewModel.kt`·DI 등록 고침
- [ ] T074 [US4] `MoodThemeViewModelTest`에 더함: 무료에서 Plus 테마를 누르면 미리보기만 열리고 저장하지 않음, `pendingTheme`이 있는 채 요금제가 Plus로 확정되면 그 테마 저장, 결제 없이 돌아오면 `pendingTheme` 지움
- [ ] T075 [US4] `settings/MoodThemePreviewSheet.kt`: 그 테마로 15종과 최근 일기 3편(기기 안, 없으면 견본 문구), [Plus 알아보기]. 최근 일기는 저장소에서 읽어 `MoodPreviewSample` 불변 값으로
- [ ] T076 [US4] 클레이 꼴 고르기(머리 9, 피부톤 6, 안경 켜고 끄기)를 테마 화면 안에. 고르면 그 자리에서 15종이 바뀜
- [ ] T077 [US4] `shared/feature/plus/.../PlusBenefit.kt` `MOOD_THEME(core = false)`, `PlusScreen.kt` 아이콘·이름·설명, `shared/feature/plus/src/commonMain/composeResources/values*/strings.xml` 8벌. `PlusBenefitTest`: 핵심 넷 차례 그대로, 테마 화면에서 들어오면 `enteredOther`가 `MOOD_THEME`. 열린 결정 2를 "핵심"으로 정하면 `scripts/lottie/make_onboarding.py` `plus()`와 `plus_benefits.json`도
- [ ] T078 [US4] `SettingsEntries.kt`: 미리보기 [Plus 알아보기] → `PlusKey(PlusBenefit.MOOD_THEME)`
- [ ] T079 [P] [US4] `settings/preview/MoodThemePreviews.kt`에 미리보기 시트·클레이 고르기·Plus 끝난 상태
- [ ] T080 [P] [US4] `MoodThemeSettingsUiTest.kt`에 미리보기 시트가 뜨고 테마가 그대로인지, 클레이 꼴을 바꾸면 얼굴이 바뀌는지

## 7단계: US5 고를 때 살짝 움직인다 (P2)

**목표**: 쓰기 시트에서 고를 때만 한 번 움직임. **독립 시험**: 쓰기에서 고르면 움직이고, 찾기·목록에서는 움직이지 않고, 동작 줄이기에서는 움직이지 않는다.

- [ ] T090 [US5] `ds/theme/DiaryMotion.kt`에 고르기 움직임 몇 가지(1초 안쪽, 한 번), 테마마다 하나를 `MoodArtSet`에. T003에서 정한 방식대로
- [ ] T091 [US5] `ui/DiaryMoodGrid.kt`에 움직임 인자(쓰기만 켬), `rememberReduceMotion()`을 따름. `shared/feature/write/.../WriteSheets.kt`에서 켬
- [ ] T092 [P] [US5] `MoodSheetUiTest.kt`에 동작 줄이기면 움직임 없이 바로 들어감, 찾기 시트에는 움직임 없음

## 8단계: 백업 형식 4 (FR-009, 이야기 모두에 걸림)

US1이 새 기분을 저장할 수 있게 되는 순간 필요하므로 3단계와 같이 끝낸다(출시 전 필수).

- [ ] T100 `shared/core/backup/src/commonTest/kotlin/org/sjhstudio/diary/core/backup/SnapshotWriterTest.kt`를 먼저: `Snapshot.newMoods`가 옛 9종만이면 0, 살아 있는 일기·휴지통 일기의 새 기분을 셈, 임시 저장본(사본에서 지움)은 세지 않음
- [ ] T101 `ManifestCodecTest.kt`·`BackupRunnerTest.kt`·`BundleTest.kt`를 먼저: 새 기분 없으면 드라이브 3·묶음 2, 있으면 둘 다 4, 이 판은 4를 `OK`·5를 `NEEDS_NEWER_APP`, 드라이브 속성 `diary.format`이 목록 파일 번호와 같음. 지금 `ManifestCodecTest`의 `formatVersion = 4` → `NEEDS_NEWER_APP` 줄은 5로 옮김
- [ ] T102 `backup/SnapshotWriter.kt` `newMoods`(사본에서 `mood IS NOT NULL AND mood NOT IN (Mood.LEGACY)`), `backup/ManifestCodec.kt` `FORMAT_VERSION = 4`와 쓰는 번호를 정하는 함수 하나, `backup/BackupRunner.kt`(목록 파일·속성), `backup/BundleExporter.kt`. 주석 "3부터 스냅샷을 gzip으로"를 형식별 뜻으로 고침
- [ ] T103 [P] `RestoreRoundTripTest.kt`·`CompressedBackupTest.kt`: 형식 2·3 옛 백업이 그대로 복원, 형식 4 백업 왕복에서 새 기분이 그대로
- [ ] T104 `/backup-roundtrip`: 두 에뮬레이터 왕복, 2.2.0 앱(`v2.2.0`)에서 형식 4 드라이브·묶음 백업이 "앱을 업데이트하면 복원할 수 있어요"로 막히고 형식 2 묶음은 복원되는지, 사진 해시 대조

## 9단계: 마무리

- [ ] T110 [P] `shared/core/ui/.../DiaryLottie.kt` 자리표시 확인, `scripts/lottie/make_onboarding.py`의 `MOOD_SICK`을 자리표시로 바꾸고 온보딩·결제 그림을 다시 만듦(`python3 -I scripts/lottie/make_onboarding.py`)
- [ ] T111 [P] 오픈소스 그림을 쓰지 않았는지 확인. 썼다면 `./gradlew :composeApp:exportLibraryDefinitions`와 라이선스 목록(FR-011)
- [ ] T112 크기 예산: `apkanalyzer apk download-size`로 `develop`과 비교(+500KB 이하), iOS 출시형 앱 크기(+1MB 이하). 넘으면 생성기에서 소수 자리·점을 줄임
- [ ] T113 성능: 실기기 `benchmarkRelease`로 `StartupBenchmark`·`ScrollBenchmark`를 기본·클레이·질감 테마에서(SC-004). `benchmarks/.../BaselineProfileGenerator.kt` 확인 뒤 Baseline Profile 다시 만듦(`/release-prep`). `adb logcat -s StrictMode`에 새 위반 없음
- [ ] T114 전체 검사(`spotlessCheck designTokenCheck coroutineSafetyCheck stringResourceCheck unusedImportCheck detekt :composeApp:lint allTests assembleDebug`), `python3 -I scripts/mood/make_mood_art.py --check`, iOS 빌드(`xcodebuild … -derivedDataPath iosApp/build`)
- [ ] T115 `/emulator-check`: 한국어 먼저, 라이트·다크, 큰 글자, 태블릿, 영어·일본어·다섯 언어, 무료·Plus·Plus 끝남, 1.7.4·2.2.0 업그레이드(quickstart 2·4절). 캡처를 남기고 못 본 것은 그대로 적음
- [ ] T116 9개 테마 × 15종을 16·28, 라이트·다크로 나란히 캡처해 이름 없이 구별되는지(SC-003)
- [ ] T117 업데이트 안내 문구(기분 15종, 색이 바뀜, 기분 테마) 세 언어 초안을 사용자에게 확인받음. Remote Config·출시 노트에 넣지 않고 확인만
- [ ] T118 `/self-review`, 안 쓰는 코드·import 정리와 포맷
- [ ] T119 커밋(논리 단위, Conventional Commits, 형식 변경 커밋은 `feat(backup)!`와 `BREAKING CHANGE`), 푸시, PR(`/pr-review`). 머지는 사용자에게 물음

## 순서와 의존

- 1단계(디자인) → 2단계(바탕) → 3단계 US1 + 8단계(백업 형식 4)는 함께 끝냄 → 4단계 US2, 5단계 US3는 서로 독립 → 6단계 US4는 US3 화면 위에 → 7단계 US5는 US1 격자 위에 → 9단계.
- T011(Mood) 뒤에 T014·T032·T041·T102가 열린다. T017(그림 틀) 뒤에 T030·T050·T070이 열린다. T018~T020(설정) 뒤에 T053·T060·T073이 열린다.
- T001 프로토타입 확정 전에는 T004 이후 디자인과 개발을 시작하지 않는다.

## 함께 할 수 있는 것

- 2단계: T010, T012, T013, T015, T016은 서로 다른 파일이라 함께.
- US1: T033~T037을 함께.
- US3: T050(그림), T057(문자열), T058(프리뷰), T059(화면 시험)를 함께.
- US4: T070(그림)과 T072(되돌리기 시험)를 함께.

## 구현 전략

1. **처음 낼 수 있는 단위(MVP)**: 2단계 + US1 + 백업 형식 4 + US2. 기본 테마 15종과 새 색만으로도 내보낼 수 있고, 옛 앱이 새 기분을 지우는 길이 막혀 있다.
2. 다음: US3(무료 테마). 테마 화면과 설정 백업.
3. 다음: US4(Plus 테마, 미리보기, 되돌리기), US5(움직임).
4. 각 단계 끝마다 검사 전체와 그 단계 이야기의 독립 시험을 돌린다.
