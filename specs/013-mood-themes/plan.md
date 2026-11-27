# 구현 계획: 기분 15종과 기분 테마

**브랜치**: `feat/mood-themes` | **작성**: 2026-11-27 | **명세**: [spec.md](spec.md) | **조사**: [research.md](research.md) | **데이터**: [data-model.md](data-model.md) | **계약**: [contracts/mood-art.md](contracts/mood-art.md) | **확인**: [quickstart.md](quickstart.md)

## 요약

기분을 9종에서 15종으로 늘리고(새 id `excited`·`proud`·`calm`·`bored`·`anxious`·`annoyed`), 기분 색을 15색으로 바꾸고, 기분 얼굴을 테마 가운데 골라 앱 전체에 쓴다. 테마는 9벌(무료 5, Plus 4)이고, 도형은 "스티커 효과"를 켜고 끄는 설정이 있다(research 11절).
DB 스키마는 그대로다. 기분 칸이 이미 글자 id라 새 값이 그냥 들어간다. 옛 앱이 새 기분을 "기분 없음"으로 읽고 지우지 않게, 새 기분 일기가 든 백업만 형식 4로 써서 옛 앱이 "앱을 업데이트하면 복원할 수 있어요"로 막게 한다.
테마 그림은 Figma 판을 내보낸 webp로 넣고, 클레이는 겹 그림을 쌓아 그린다. 설정·Plus 잠금·백업은 스펙 010 테마 색과 같은 꼴(원문 값 보관, 보일 때 거르기, 확정된 요금제로 되돌리기)을 쓴다.

## 기술 배경

> **2026-11-28 개발에서 바뀜.** 테마 그림은 SVG 생성기 대신 Figma 판(72칸)을 240px로 내보낸 webp로 넣었다(`shared/core/ui/src/commonMain/composeResources/drawable/mood_<테마>_<기분>.webp`, 다크 그림이 따로 있는 테마만 `_dark`). 클레이는 겹 webp를 앱에서 쌓는다(`ShownMoodLook.clayLayers`). 기본 테마는 코드 경로(`DiaryMoodFace`) 그대로다. 생성기·벡터 캐시·질감 합성은 하지 않았다(contracts/mood-art.md 머리글, `docs/DECISIONS.md` 2026-11-28).


- 축은 스펙 001과 같다(Kotlin, CMP, Koin, DataStore, Room). 새 라이브러리는 없다.
- 그림: Figma 72칸 판을 240px webp로 내보내 `shared/core/ui/src/commonMain/composeResources/drawable/mood_<테마>_<기분>.webp`에 둔다. 클레이는 겹 webp를 앱에서 쌓는다(`ShownMoodLook.clayLayers`). 기본 테마만 코드 경로(`DiaryMoodFace`)다.
- 질감: 그림 파일에 들어 있어 따로 그리지 않는다.
- (2026-11-28 바뀜) 처음 계획한 SVG 생성기(`scripts/mood/make_mood_art.py`), `ImageVector` 캐시, 질감 타일은 하지 않았다. 질감·속 그림자를 경로로 옮기지 못해서다(contracts/mood-art.md 머리글, `docs/DECISIONS.md` 2026-11-28).
- 움직임: `graphicsLayer` 한 가지 움직임을 테마가 고름, `rememberReduceMotion()`을 따름.
- 백업: `SnapshotWriter`가 사본에서 새 기분 일기를 세고, 드라이브·묶음 목록 파일 번호를 정한다(research 2절).
- 시험: `commonTest`(기분 정의·묶음, 백업 번호 규칙과 왕복, 설정 저장·모르는 값 지키기, Plus 되돌리기, 그림 생성 결과, 테마 화면 모델), `iosTest`(15칸 시트, 테마 화면, 미리보기 시트, 큰 글자), Android `androidTest` 업그레이드 시험(옛 기분 그대로).
- 성능 목표(SC-004): 기본 테마 대비 시작 시간과 목록 스크롤 프레임이 나빠지지 않음(실기기 `benchmarkRelease`). 크기 예산: Android 내려받기 크기 +500KB, iOS +1MB 이하(research 4절).

## 헌법 점검

| 원칙 | 이 계획에서 지키는 자리 |
| --- | --- |
| I 기록은 잃지 않는다 | 옛 9종 id·레거시 번호 그대로(`StoredIdsTest`). DB 스키마 그대로. 새 기분 일기가 든 백업은 형식 4라 옛 앱이 열지 않아, 옛 앱이 기분을 null로 다시 쓰는 길이 막힌다. 번호는 사본에서 세어 담긴 내용과 늘 맞는다. 옛 백업(형식 2·3)은 그대로 복원 |
| II 일기는 사용자 것 | 미리보기 시트의 일기 줄은 기기 안에서만 그린다. 통계에는 테마 id만 보낸다(일기 내용·기분 기록은 보내지 않음) |
| III 무료 기능·광고 | 기분 15종 모두 무료, 지금 얼굴(기본)을 포함한 무료 6벌은 무료. Plus 4개는 새로 만든 꾸미기(`docs/business.md`). 테마 화면은 설정 안이라 광고 없음. Plus가 끝나도 일기와 기분은 그대로이고 얼굴만 기본으로 |
| IV 돈 드는 서버 없음 | 서버 없음. 그림은 앱에 든다 |
| V 심플 | 테마는 기분 얼굴 그림만 바꾼다. 기분 색·바탕·부품은 디자인 시스템 토큰 그대로. 테마 그림(webp)과 고르는 코드는 `shared/core/ui`(`ShownMoodLook.kt`, `MoodWeather.kt`, `designTokenCheck` 대상 밖)에, 클레이 피부톤 고정 색은 디자인 시스템(`DiaryClaySkin`)에 두고 화면 코드는 `MoodFace`만 부른다. Figma·`docs/design.md`를 먼저 고친다 |
| VI 어떤 기기에서도 | 공통 코드 하나. 15칸 격자는 폰 세로에서 스크롤 없이, 큰 글자에서 이름 두 줄. 태블릿 쓰기 옆 칸과 설정 오른쪽 칸 확인. 위젯은 아직 없어 이 스펙에서 다루지 않음(열린 결정 3) |
| VII 근거 | `research.md` 1~11절, 스펙 레퍼런스 |

설계 뒤 다시 봄: 위반 없음. 복잡도 표에 적을 것 없음.

## 구조

```text
(2026-11-28 바뀜) 그림은 Figma "시안 · 기분 테마 후보"가 원본이다. design/mood-art/ 원본, 질감 PNG, scripts/mood/make_mood_art.py 생성기는 만들지 않았다

shared/core/model/.../Mood.kt                        15종, legacyCode Int?, ordered, LEGACY
shared/core/model/.../AdvancedStats.kt               Mood.tone 15종
shared/core/model/.../MoodTheme.kt                   MoodTheme 9개 id·Plus·도형 스티커 효과, ClayHair, ClaySkin, MoodLook
shared/core/model/.../AppSettings.kt                 moodLook
shared/core/model/.../Plan.kt                        moodThemes
shared/core/datastore/.../SettingsStore.kt           mood_theme, clay_hair, clay_skin, clay_glasses
shared/core/data/.../SettingsRepository.kt           updateMoodLook (DefaultSettingsRepository, FakeSettingsRepository)
shared/core/data/.../PlusLookReset.kt                CustomThemeReset를 넓혀 이름 바꿈
shared/core/backup/.../SnapshotWriter.kt             Snapshot.newMoods
shared/core/backup/.../ManifestCodec.kt              FORMAT_VERSION 4, 쓰는 번호 정하기, ManifestSettings 칸 넷
shared/core/backup/.../BackupRunner.kt               드라이브 번호·diary.format 속성
shared/core/backup/.../BundleExporter.kt             묶음 번호
shared/core/designsystem/.../theme/DiaryColors.kt    DiaryMoodColors 15칸, lineOn
shared/core/designsystem/.../icon/DiaryMoodFace.kt   기본 테마 얼굴(코드 경로, 15종)
shared/core/ui/src/commonMain/composeResources/drawable/mood_*.webp, clay_*.webp   테마 그림(Figma 판 240px)
shared/core/ui/.../MoodWeather.kt                    MoodFace(테마 읽기), 이름 15종, DiaryMoodColors.of 15종
shared/core/ui/.../ShownMoodLook.kt                  LocalMoodLook, 테마 → 그림 고르기(art), 클레이 겹(clayLayers)
shared/core/ui/.../DiaryMoodGrid.kt                  네 묶음(5·5·2·3) 최대 5칸, 고를 때 움직임(쓰기만)
shared/core/ui/.../preview/MoodWeatherPreviews.kt    15종 × 테마
shared/app/.../DiaryApp.kt                           LocalMoodLook 내려 주기
shared/app/.../AppRootViewModel.kt                   PlusLookReset 감시
shared/app/.../backup/BackupSettingsBridge.kt        기분 테마 담기·적용
shared/app/.../navigation/ScreenKeys.kt              MoodThemeSettingsKey
shared/app/.../settings/SettingsEntries.kt           테마 화면 연결, PlusKey(PlusBenefit.MOOD_THEME)
shared/feature/settings/.../SettingsScreen.kt        "화면" 묶음 "기분 테마" 줄
shared/feature/settings/.../MoodThemeSettingsScreen.kt  테마 목록, 클레이 꼴 고르기, 미리보기 시트
shared/feature/settings/.../MoodThemeViewModel.kt    MoodThemeUiState, pendingTheme
shared/feature/settings/.../preview/MoodThemePreviews.kt
shared/feature/insight/.../AdvancedStatsTone.kt      가운데 묶음 셋, 범례 자리 셋
shared/feature/plus/.../PlusBenefit.kt, PlusScreen.kt   MOOD_THEME
scripts/lottie/make_onboarding.py                    MOOD_SICK → 자리표시, 그림 다시 만듦
composeResources values*/strings.xml (core/ui, feature/settings, feature/plus, feature/insight) 8개 언어
```

**구조 결정**: 새 모듈은 만들지 않는다. 테마 그림과 기분·테마의 잇기는 `shared/core/ui`(`ShownMoodLook.kt`가 고르고 `MoodWeather.kt`가 그림), 기본 테마 얼굴 경로와 클레이 피부톤 색은 디자인 시스템, 설정 화면은 `shared/feature/settings`다(`/cmp-architecture` 1절). 테마를 고르는 화면만 새로 생긴다.

## 데이터 흐름

```text
SettingsStore(mood_theme…) → SettingsRepository.settings → DiaryApp
PlanRepository.plan ─────────────────────────────────────→ DiaryApp
DiaryApp: look.shownTheme(plan.moodThemes) → LocalMoodLook
MoodFace(mood, size) → LocalMoodLook → ShownMoodLook.art(mood, dark) 또는 clayLayers(mood) → painterResource(webp)
테마 화면 → MoodThemeViewModel → SettingsRepository.updateMoodLook (launchWrite)
PlanRepository.confirmed + settings → PlusLookReset → withoutPlusTheme()
```

상태의 주인: 고른 테마는 설정 저장소, 쓸 수 있는지는 요금제 저장소다. 화면은 둘을 받아 그리기만 한다.

## 위험과 막는 방법

- **옛 앱이 새 기분을 지움**: 형식 4 규칙(research 2절). 사본에서 세는 시험, 휴지통만 새 기분인 시험, 옛 `check`가 4를 막는 시험. `/backup-roundtrip`로 2.2.0 앱과 왕복.
- **옛 9종이 바뀜**: `StoredIdsTest` 그대로 통과, `LegacyEntryMapperTest`·`UpgradeVerificationTest`(1.7.4 번호 7 → 좋음).
- **파일로 내보내기 약속이 깨짐**: 새 기분이 없으면 묶음은 형식 2 그대로인 시험.
- **Plus가 아닌데 Plus 테마가 스침**: 보일 때 거르기(`shownTheme`)로 첫 프레임부터 막고, 저장값은 확정된 요금제로만 고친다(재설치 직후 산 사람의 값을 지우지 않음). 스펙 010 US6과 같은 시험.
- **테마를 바꿀 때 앱 전체 다시 그리기**: 테마 id가 바뀔 때만. 연타해도 저장은 마지막 값(`launchWrite`).
- **목록·달력이 느려짐**: 고른 테마 그림만 읽는다(`painterResource` 캐시). `ScrollBenchmark`·`StartupBenchmark`로 잼(T113, 아직 안 잼). Baseline Profile 다시 만듦.
- **앱 크기**: 그림 약 1.5MB로 예산(+500KB)을 넘는다. 단순한 테마(테두리·도형·도트)를 벡터로 바꾸는 것을 다음 판에서 본다. `apkanalyzer`로 잼(T112, 아직 안 잼).
- **15칸이 폰에서 넘침**: 쓰기 시트 높이를 작은 폰(360dp 폭)·큰 글자에서 캡처로 확인. 넘치면 칸 높이 토큰(`DiaryWriteSize.moodCellHeight`)부터 본다.
- **색만으로 구별**: 테마마다 15종을 흑백 캡처로 나란히 놓고 구별되는지 디자인 점검에서 본다(SC-003).
- **온보딩·결제 그림 색이 옛 값으로 남음**: `MOOD_SICK` 자리표시로 바꾸고 다시 만든 그림을 화면에서 확인.

## 열린 결정

research.md "열린 결정" 표(1 질감 크기, 2 결제 화면 핵심, 3 위젯, 4 설정 자리, 5 다섯 언어 문구 확인, 6 실을 판). 권장대로 가면 tasks.md가 그대로 맞고, 다르게 정하면 표의 "다르게 갈 조건"에 적은 작업이 늘어난다.
