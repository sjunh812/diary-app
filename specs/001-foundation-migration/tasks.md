---

description: "스펙 001 기반과 이전 작업 목록"
---

# Tasks: 기반과 이전

**Input**: `specs/001-foundation-migration/`의 spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md, checklists/data-safety.md

**Prerequisites**: Phase 1의 프로토타입·Figma 작업(T001~T007)은 지금 할 수 있다. 코드는 `docs/renewal-planning`에서 갈라진 `feat/foundation-migration` 브랜치에서 한다.
Play의 레거시 버전은 1.7.4(`94179a4`)다. master에는 내지 않은 1.7.5 코드가 있으니 업그레이드 시험에 쓰지 않는다.

**Tests**: 스펙이 시험을 요구한다(FR-032, FR-034, SC-001~SC-012, 헌법 I·II). 각 이야기의 시험을 먼저 쓰고 실패하는 것을 본 뒤 구현한다.

**Organization**: 사용자 이야기별로 묶었다. US1~US3이 P1이고 셋이 모두 있어야 R1을 낼 수 있다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 끝나지 않은 작업에 기대지 않아 나란히 할 수 있다
- **[Story]**: US1~US5(spec.md)

## Path Conventions

- `{kt}` = `src/commonMain/kotlin/org/sjhstudio/diary`, `{test}` = `src/commonTest/kotlin/org/sjhstudio/diary`
- `{android}` = `src/androidMain/kotlin/org/sjhstudio/diary`, `{ios}` = `src/iosMain/kotlin/org/sjhstudio/diary`
- `{host}` = `src/androidHostTest/kotlin/org/sjhstudio/diary`(AGP 9 KMP 호스트 시험), `{res}` = `src/commonMain/composeResources`
- 모듈은 plan.md "Source Code" 구조를 따른다. 예: `shared/core/migration/{kt}/core/migration/LegacyText.kt`
- Figma는 파일 "1일 1일기"(`HQOqmp9kJv5EEpQH2mXMj9`)의 페이지와 섹션 이름을 경로로 쓴다

---

## Phase 1: 프로토타입과 디자인 (`/diary-flow` 3·4단계, 지금 가능)

- [x] T001 이야기별로 지나가는 화면과 뒤로 가기, 상태(첫 실행, 이전 중, 실패, 공간 부족, 잠금, 생체 취소, 기다림, 폰 잠금 없음)를 `specs/001-foundation-migration/prototype.md`에 쓴다(`contracts/app-shell.md`, `contracts/migration-flow.md`, `contracts/lock.md` 기준)
- [x] T002 [P] "표지 · 흐름" 섹션 "프로토타입 · 기반과 이전"에 잠금 화면(L1), 생체 인증 상태, 틀린 비밀번호, 30초 기다림, 비밀번호를 잊어버렸어요(L3), 폰 잠금 없음 안내, 비밀번호 정하기(L2, 폰 잠금 안내 문구 포함), 잠금 설정(L4) 프레임을 만들고 잇는다
- [x] T003 [P] 같은 섹션에 이전 준비(M0), 이전 진행(M1, 일기 n/N·사진 n/N), 결과(M2), 일부만(M2-b), 실패(M3), 예전 일기 보기(M4), 공간 부족 프레임을 만들고 잇는다
- [x] T004 [P] 같은 섹션에 설정 틀(S6)의 하위 화면 글꼴(G1, 10종 미리보기), 본문 글자 크기(G2, 5단계 미리보기 문장), 화면 모드(G3), 알림(R1, 허락 필요 줄), 기록 이전 결과, 오픈소스와 글꼴 고지 프레임과 "글꼴이 바뀌었어요" 토스트를 만든다(고지 화면 이름은 2026-11-04 "오픈소스 라이선스"로 바꿈)
- [x] T005 흐름 시작점("기반과 이전 · 업데이트한 사용자", "· 잠금 켠 사용자", "· 새 설치")을 두고 캡처를 보내 사용자 확정을 받는다. 확정하면 `prototype.md`에 날짜와 바뀐 점을 쓰고 스펙에 되먹인다(지금 앱과 크게 달라지는 곳은 먼저 묻는다)
- [x] T006 "화면" 페이지에 15줄을 라이트 폰(왼쪽 여백 번호)·설명 패널·다크 폰·태블릿과 상태 섹션으로 그렸다(2026-11-01): 시작 화면 (SP), 잠금 (L1), 비밀번호 정하기 (L2), 비밀번호를 잊었을 때 (L3), 잠금 설정 (L4), 기록 이전 중 (M1), 기록 이전 결과 (M2), 기록 이전 실패 (M3), 예전 일기 보기 (M4), 설정 (S6), 글꼴 (G1, 받기·받는 중·인터넷 없음 상태 포함), 본문 글자 크기 (G2), 화면 모드 (G3), 매일 알림 (R1), 기록 이전 결과(설정) (M5)
- [x] T007 "표지 · 흐름" 페이지에 진행 현황 표를 만들고 001 화면을 더한다(2026-11-01, 섹션 "진행 현황")
- [x] T091 "화면" 페이지의 기록 이전 결과 (M5)를 일부만 옮겨진 경우(읽지 못한 일기, 원본 파일 내보내기, 문의하기)로 바꾸고, S6과 태블릿 왼쪽 설정 목록(L4·G1·G2·G3·R1·M5 태블릿)에서 "기록 이전 결과" 줄을 뺀다(2026-11-01 결정, 프로토타입은 반영함). 2026-11-01 반영: M5 태블릿은 일부만 된 기기라 그 줄을 남겨 고른 줄로 두고, 상태 섹션에 "M5 · 복구 문의" 시트를 더했다. 같은 결정에 맞춰 M2 "다 옮겼어요"의 설정 안내 글을 빼고 M2-b의 "다시 시도"를 "문의하기"로 바꿨다(프로토타입도)
- [x] T093 개발 전 디자인 점검: "디자인 시스템" 페이지와 "화면" 페이지 001 15줄(T091 반영 뒤)을 토스 TDS·당근 SEED·Apple HIG·Material 3와 유명 앱에 대어 보고(흐름과 UX 패턴, 여백·간격, 글자 크기·굵기·줄 높이, 모서리, 터치 영역, 색 대비, 폰·태블릿·다크가 같은 규칙인지, 문구) `specs/001-foundation-migration/design-review.md`에 남긴다. 고칠 것은 `docs/design.md` → Figma 변수·컴포넌트 → 화면 순서로 고친 뒤 디자인 시스템 코드(T025, T026)와 화면 작업에 들어간다(2026-11-01 사용자 요청, 헌법 V). 2026-11-01 점검 18항목, 고친 것 7가지(안내 글 색, 숫자판 글자, 아이콘 4단계, 묶음 머리, 스위치, 옛 문구, 규칙 두 줄)

---

## Phase 2: Setup (프로젝트 뼈대)

- [x] T008 `feat/foundation-migration` 브랜치를 만들고 루트 `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, Gradle 래퍼를 새로 쓴다. 레거시 `app/`은 `include`에서 빼고 폴더는 남긴다(plan.md Structure Decision)
- [x] T009 `gradle/libs.versions.toml`에 research R2 표의 버전을 그대로 고정한다(알파·베타 없음)
- [x] T010 [P] `build-logic/settings.gradle.kts`, `build-logic/convention/build.gradle.kts`와 `build-logic/convention/src/main/kotlin/`의 `AndroidApplicationConventionPlugin.kt`, `KmpLibraryConventionPlugin.kt`(android + iosArm64 + iosSimulatorArm64, JDK 17), `KmpFeatureConventionPlugin.kt`, `RoomConventionPlugin.kt`(KSP + 스키마 `shared/core/database/schemas`)를 만든다
- [x] T011 [P] 루트 `compose_compiler_config.conf`에 `org.sjhstudio.diary.core.model.*`, `kotlinx.datetime.*`를 적고 `KmpLibraryConventionPlugin`이 읽게 한다(research R10)
- [x] T012 plan.md의 모듈마다 `build.gradle.kts`와 빈 소스 폴더를 만든다: `shared/app`, `shared/core/{model,database,datastore,data,migration,designsystem,ui,platform,testing}`, `shared/feature/{lock,migration,settings}`, `composeApp`, `benchmarks`
- [x] T013 [P] 루트 `build.gradle.kts`에 Spotless + ktlint, `.editorconfig`를 둔다(research R20)
- [x] T014 [P] `.githooks/commit-msg`(정규식은 research R19 그대로, `Merge `·`Revert "` 통과)와 루트 `installGitHooks` 작업(`git config core.hooksPath .githooks`, `preBuild`에 묶음), 훅 시험 `scripts/test-commit-msg.sh`를 만든다(FR-031)
- [x] T015 [P] `build-logic/convention/src/main/kotlin/DesignTokenCheckTask.kt`와 루트 등록: `shared/feature/**`, `shared/app/**`의 Kotlin에서 `Color(0x`, 숫자 `.dp`·`.sp`, `RoundedCornerShape(숫자`, `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `Text("`를 찾으면 실패, `check`에 묶음(FR-028, FR-030, `contracts/app-shell.md` 빌드 검사)
- [x] T016 [P] `.github/workflows/ci.yml`: Ubuntu `spotlessCheck designTokenCheck allTests assembleDebug`, macOS `iosSimulatorArm64Test`와 `xcodebuild -scheme iosApp -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build`, 업그레이드 시험은 `workflow_dispatch`
- [x] T017 `composeApp/src/main/AndroidManifest.xml`(applicationId `org.sjhstudio.diary`, minSdk 24, targetSdk 36, compileSdk 37(research R2), `versionCode` 37 이상), `composeApp/src/main/kotlin/org/sjhstudio/diary/DiaryApplication.kt`, `MainActivity.kt`(빈 `setContent`)
- [x] T018 [P] `iosApp/iosApp.xcodeproj`, `iosApp/iosApp/iOSApp.swift`, `ContentView.swift`(`ComposeUIViewController`), `shared/app`을 static framework `Shared`로 내보내기, 최소 iOS 17(research R22)
- [x] T019 [P] `shared/core/testing/{kt}/core/testing/TestClock.kt`(벽시계·단조 시계를 함께 움직임)와 모듈 뼈대

---

## Phase 3: Foundational (모든 이야기가 기대는 것)

**⚠️ 이 단계가 끝나야 이야기 작업을 시작한다**

- [x] T020 [P] `shared/core/model/{kt}/core/model/`에 `Entry.kt`, `Media.kt`(status NORMAL/MISSING), `Mood.kt`·`Weather.kt`(소문자 ID와 레거시 숫자), `DiaryFont.kt`(ID 11개), `BodyTextSize.kt`(15/24, 17/28, 19/30, 21/34, 24/38), `ThemeMode.kt`, `AppSettings.kt`, `LockSettings.kt`, `RelockAfter.kt`, `MigrationState.kt`, `MigrationReport.kt`, `LegacyBackupLink.kt`를 data-model.md 그대로 만든다
- [x] T021 [P] `shared/core/platform/{kt}/core/platform/AppFiles.kt` expect와 `{android}`·`{ios}` actual: data-model.md "파일 자리" 표의 경로(Android `databases/diary.db`, `files/media`, `no_backup/thumbs`, `files/datastore`, `files/migration`, `files/legacy-snapshot`; iOS `Application Support/…`, `thumbs` 백업 제외 표시)
- [x] T022 `shared/core/database/{test}/core/database/DiaryDatabaseTest.kt`: 번들 드라이버 메모리 DB로 `media` 외래 키 연쇄 삭제, `(entry_id, sort_order)` 유일, `legacy_id` 유일(null 여럿 허용), `missing` 줄의 파일 열 null 허용, 목록 색인 사용(`EXPLAIN QUERY PLAN`)을 본다(Android 호스트와 iOS 시뮬레이터)
- [x] T023 `shared/core/database/{kt}/core/database/`에 `DiaryDatabase.kt`(Room 3, 버전 1, `BundledSQLiteDriver`, `Dispatchers.IO`), `EntryEntity.kt`(열 이름·타입·널은 data-model.md `entry` 표 그대로, 색인 `(deleted_at, date DESC, time DESC, created_at DESC)`, `(deleted_at, is_favorite)`, `legacy_id` 유일), `MediaEntity.kt`(`media` 표 그대로), `Converters.kt`, `EntryDao.kt`(`PagingSource`, 개수, 일괄 넣기), `MediaDao.kt`, `DatabaseFactory.kt` expect/actual. 스키마 JSON을 `shared/core/database/schemas/`에 커밋
- [x] T024 [P] `shared/core/datastore/{kt}/core/datastore/SettingsStore.kt`, `LockStore.kt`(키 이름·타입·기본값은 data-model.md 표 그대로), `DataStoreFactory.kt`(폴더는 `AppFiles`가 플랫폼마다 정하므로 공통 코드 하나로 둠)
- [x] T025 [P] `shared/core/designsystem/{kt}/core/designsystem/theme/`에 `DiaryColors.kt`(라이트·다크, `docs/design.md` 색 표 그대로), `DiaryTypography.kt`(13단계, 자간 0, sp), `DiarySpacing.kt`, `DiaryRadius.kt`, `DiaryMotion.kt`, `DiaryTheme.kt`와 Pretendard 4굵기 `{res}/font/`
- [x] T026 [P] `shared/core/designsystem/{kt}/core/designsystem/component/`에 버튼(56/48/38/32), 상단 바(56, 오른쪽 글자 버튼), 시트(떠 있는 시트, 폭 480 상한), 토스트(5초, 누르는 동안 멈춤), 대화상자(312, "닫기" 왼쪽), 설정 줄(최소 56), 칩, 빈 상태를 `docs/design.md` 컴포넌트 규격대로 만든다. 2026-11-01: 디자인 점검(T093)에서 정한 스위치, 진행 막대, 고르기 목록 줄도 같이 만들었다. 숫자판은 잠금 화면(US2)과 함께 만든다
- [x] T027 [P] `shared/core/platform/{test}/core/platform/TelemetryTest.kt`: 고정 표에 없는 이벤트·키는 개발 빌드에서 예외, 출시 빌드에서 버려지는지 본다
- [x] T028 `shared/core/platform/{kt}/core/platform/Telemetry.kt`, `AnalyticsEvents.kt`(`contracts/app-shell.md` 고정 표와 구간), `{android}` Firebase Analytics·Crashlytics 구현, `{ios}` Swift 쪽 구현을 받는 다리, `iosApp`에 Firebase SPM(research R21). 2026-11-01: `Telemetry.kt`, `AnalyticsEvents.kt`, 보내지 않는 `NoTelemetrySink`까지 했다. Firebase 프로젝트(설정 파일)를 만든 뒤 두 플랫폼 구현을 잇는다
- [x] T029 `shared/app/{kt}/app/Startup.kt`(설정·잠금 저장소와 `state.json` 읽기, `ready`), `DiaryApp.kt`(`DiaryTheme` → 관문 자리 → 빈 틀), `di/AppModules.kt`(Koin), `{ios}/app/MainViewController.kt`. 2026-11-01: `Startup`(설정·잠금 읽기, 폴더 만들기, DB는 열지 않음), `di/AppModules.kt`·`KoinInit.kt`, Android `DiaryApplication`과 iOS `iOSApp.init`(`startApp`) 연결까지 했다. `DiaryApp`에 `DiaryTheme`(설정의 화면 모드, 시작 단계 값으로 첫 프레임부터)까지 씌웠다. 잠금·이전 관문은 US2·US1, 앱 틀(탭·레일)은 스펙 002에서 잇는다 2026-11-04: 잠금·이전 관문(US2·US1)과 앱 틀(하단 탭·레일, 설정 탭)까지 이어졌다. 오늘 바꾼 뒤 끊겼다 다시(KILL_AT=Copying) 업그레이드 시험이 두 번째 시도에서 끝나고 원본이 그대로인 것을 다시 봤다
- [x] T030 `shared/core/data/{kt}/core/data/SettingsRepository.kt`와 `shared/core/testing/{kt}/core/testing/FakeSettingsRepository.kt`
- [x] T094 세 언어(2026-11-01 사용자 요청, `docs/design.md` 여러 언어): 모든 `composeResources`를 `values`(영어)·`values-ko`·`values-ja`로 나누고 개수 문구는 `plurals`로, `composeApp/src/main/res`의 앱 이름과 `generateLocaleConfig`(Android 앱별 언어), `iosApp/iosApp/*.lproj/InfoPlist.strings`와 `CFBundleLocalizations`, 날짜·시각은 `shared/core/ui/{kt}/core/ui/DiaryDateFormat.kt` expect/actual(플랫폼 형식, 기기 12·24시간), 줄바꿈 언어를 화면 언어로(`DiaryTypography.kt`), 일본어 화면은 시스템 글꼴(D13 임시), 루트 `stringResourceCheck`(R18). 영어·일본어 문구는 초안이고 원어민 검수는 `docs/release.md` 출시 전 확인. 2026-11-01: Android 16 에뮬레이터에서 앱별 언어를 en·ja·ko로 바꿔 업그레이드 결과 화면(M2)을 봤고, iOS 시뮬레이터 시험이 세 언어의 달·요일·시각 형식을 확인한다. Android의 날짜 형식은 예전 일기 보기(실패 화면)에서 아직 눈으로 보지 못했다

**Checkpoint**: 빈 앱이 Android·iOS에서 뜨고 `./gradlew spotlessCheck designTokenCheck allTests`가 통과한다

---

## Phase 4: User Story 1 - 업데이트해도 일기와 사진이 그대로 있다 (Priority: P1) 🎯 MVP

**Goal**: 1.7.4 원본을 바꾸지 않고 일기·사진을 옮기고 검증한다

**Independent Test**: quickstart.md §2·§3 견본 자료 조합을 업그레이드 시험으로 돌려 일기 값·사진 순서가 같고 원본 해시가 그대로인지 본다

### Tests for User Story 1 ⚠️

- [x] T031 [P] [US1] `shared/core/testing/{android}/core/testing/legacy/LegacyFixtures.kt`: 버전 1로 만들고 2로 올린 DB(`star` NULL), 빈 DB, 테이블만 있는 DB, 1만 줄 DB, WAL에만 있는 줄(연결을 닫지 않고 복사), 깨진 페이지 DB, `pref.xml`·`pref2.xml`(이메일 키, 잘못된 비밀번호 모양 포함), 캐시 사진(있음, 없음, 같은 파일 두 번, 캐시 밖 경로, 공백 경로, 확장자 없음)을 만든다. 견본 파일은 `shared/core/migration/src/androidHostTest/resources/legacy/`에 둔다. 2026-11-01: 파일 대신 `shared/core/migration/src/commonTest/.../LegacyFixtures.kt`가 시험마다 같은 자료를 만들게 해 Android 호스트와 iOS 시뮬레이터에서 함께 돈다(버전 1→2, 빈 DB, 표만, 1만 줄, WAL에만 있는 줄, 깨진 페이지, 숫자 열의 글). 사진 견본 자료는 사진 옮기기(T041)와 함께 만든다
- [x] T032 [P] [US1] `shared/core/migration/{test}/core/migration/LegacyTextTest.kt`: `contracts/legacy-mapping.md` 본문 변환 표의 11가지 값, 10만 자 글, 덩어리 경계의 서로게이트 쌍, 무작위 글 왕복 `toPlainText(legacyToHtml(c)) == c`
- [x] T033 [P] [US1] `shared/core/migration/{test}/core/migration/LegacyEntryMapperTest.kt`: 날짜 앞 10글자, `modify_date` 대체, 이전한 날 대체와 `dateUnparsed`, `HH:mm`과 `00:00` 대체, `2024-03-01 23:59:457`의 `dateDiffers`, 기분 0~8·날씨 0~6과 범위 밖 null, 장소 공백, `star` NULL, `legacy_location`
- [x] T034 [P] [US1] `shared/core/migration/{test}/core/migration/PhotoPathsTest.kt`: 빈 값, 끝 쉼표, 빈 조각, 앞뒤 공백 다시 찾기, 확장자 소문자, 머리 바이트(JPEG, PNG, WebP, HEIF), 모르면 `bin`
- [x] T035 [P] [US1] `shared/core/migration/{test}/core/migration/FingerprintTest.kt`: 같은 일기의 `legacyCanonical`과 `restoredCanonical`이 같고, 글 한 글자·사진 순서·해시·기분 하나만 바꿔도 다르다
- [x] T036 [P] [US1] `shared/core/migration/{host}/core/migration/LegacyPrefsReaderTest.kt`: `pref.xml`·`pref2.xml` 읽기, 사본에서 `password` 줄 빠짐, 이메일 키와 `"<계정>'s id"`
- [x] T037 [US1] `shared/core/migration/{host}/core/migration/MigrationRunnerTest.kt`: `docs/migration.md` "시험"의 모든 경우, 원본 SHA-256 전후 같음(SC-002), 단계마다 끊고 다시(`Snapshotting`·`Copying`·`Verifying`·`Committing`), `Partial` 뒤 업데이트하면 한 번만 다시 읽고 그 사이 쓴 일기를 지킴, 같은 버전에서는 다시 읽지 않음, 찾을 수 없는 사진의 원래 경로에 파일이 생기면 하루 한 번 다시 찾아 옮김, 공간 부족이면 시작 안 함, 일기 0편이면 결과 화면 없이 `Done`, 실패해도 `media/` 사본 남음, `photos.json`·`snapshot.json` 내용

### Implementation for User Story 1

- [x] T038 [P] [US1] `shared/core/migration/{kt}/core/migration/LegacyText.kt`(`legacyToHtml`)와 `shared/core/data/{kt}/core/data/richtext/PlainText.kt`(`toPlainText`, 스펙 002와 같이 쓰는 함수)
- [x] T039 [P] [US1] `shared/core/migration/{kt}/core/migration/LegacyEntryMapper.kt`, `PhotoPaths.kt`, `Fingerprint.kt`(`contracts/legacy-mapping.md` 그대로)
- [x] T040 [US1] `shared/core/migration/{android}/core/migration/legacy/LegacyProbe.kt`(원본 있음 판정), `LegacySnapshotter.kt`(스트림 복사 + SHA-256, `pref.xml` 비밀번호 줄 빼기, `snapshot.json`), `LegacyDbReader.kt`(번들 드라이버로 사본 열기, `PRAGMA table_info`, `_id` 순 읽기, `integrity_check` 실패 시 반씩 나눠 건지기), `LegacyPrefsReader.kt`(XML 직접 읽기). 2026-11-01: 원본 위치만 Android가 알면 되어서 사본 만들기(`LegacySnapshotter`), 사본 읽기(`LegacyDbReader`), 설정 읽기(`LegacyPrefs`)는 okio와 번들 드라이버로 공통 코드에 두었다. Android 원본 위치(`LegacyFiles`)는 이전 실행기(T043)와 함께 잇는다
- [x] T041 [US1] `shared/core/migration/{android}/core/migration/legacy/MediaImporter.kt`: `media/<sha256>.<ext>` 복사(임시 이름 → 이름 바꾸기), 같은 해시 건너뜀, 복사본 해시 다시 재기, EXIF 방향 반영 가로세로, `photos.json`
- [x] T042 [P] [US1] `shared/core/platform/{kt}/core/platform/ImageDecoder.kt` expect와 `{android}`(다운샘플링 + 정확한 크기, EXIF)·`{ios}`(`CGImageSource` 썸네일) actual, `shared/core/data/{kt}/core/data/ThumbnailRepository.kt`(`thumbs/<sha256>.jpg`, 긴 변 480px, JPEG 80)와 Coil 3 Fetcher `ThumbnailFetcher.kt`(없으면 만들어 저장). 2026-11-01: `ThumbnailRepository`는 `shared/core/data/{kt}/core/data/photo/`(사진 폴더 밖 파일은 맡지 않음, 임시 파일 뒤 이름 바꾸기), Coil 쪽은 `shared/core/ui/{kt}/core/ui/ThumbnailFetcher.kt`(`Thumbnail` 모델, 앱 뿌리의 `ProvideDiaryImageLoader`), 이전은 `MigrationRunner`가 사진을 옮긴 덩어리마다 최대 3개씩 만들고 실패는 `thumbnailsFailed`로 센다. Android EXIF는 프레임워크 `ExifInterface`(새 의존성 없음). 시험: iOS 디코더 3, 저장소 4, 이전 1(Android·iOS). 에뮬레이터에서 방향 6 견본이 288×480으로 세워졌고, 지운 썸네일이 사진 칸을 열 때 다시 만들어졌다. `seed.sh` 견본 사진을 진짜 그림으로 바꿨다(`scripts/legacy/fixtures`)
- [x] T043 [US1] `shared/core/migration/{kt}/core/migration/MigrationRunner.kt`: `contracts/migration-flow.md` 단계, `state.json`·`report.json` 임시 파일 + 이름 바꾸기, `diary-new.db`(저널 TRUNCATE)에 200줄씩, 썸네일 최대 3개씩 나란히, 검증, `diary.db`로 이름 바꾸기, `Committing` 재검증, `Partial`은 앱 업데이트 뒤 첫 실행에 한 번만 자동으로 다시 읽어 없는 `legacy_id`만 더함(`retriedVersionCode`), 찾을 수 없는 사진 하루 한 번 다시 찾기(`photoRecheckAt`, `contracts/migration-flow.md`). `{ios}`는 늘 "없음". 2026-11-01: 실행기는 공통 코드(`MigrationRunner`, `MigrationStateStore`, `MediaImporter`)로 두고 두 플랫폼 시험이 돈다. 썸네일 만들기(T042)와 Android 연결(원본 위치, 사진 크기, 남은 공간, Koin)은 아직이다
- [x] T044 [US1] `shared/core/migration/{kt}/core/migration/MigrationTelemetry.kt`: `migration_started`·`finished`·`failed`와 구간, 예외를 `MigrationError(kind)`로 바꿔 보냄(FR-013)
- [x] T045 [P] [US1] `shared/feature/migration/{kt}/feature/migration/MigrationViewModel.kt`, `MigrationProgressScreen.kt`(일기 n/N·사진 n/N, 0.25초 갱신, 사본 만드는 동안 진행 표시, 화면 꺼짐 막기), `MigrationResultScreen.kt`, `MigrationFailedScreen.kt`(다시 시도, 옛 일기 읽기, 원본 내보내기, 문의하기), 문구는 `{res}/values/strings.xml`. 2026-11-01: 준비 중·진행·결과·일부만·실패·공간 부족 화면과 화면 모델까지 했다(`MigrationScreen.kt` 하나에 모음). 실패 화면의 예전 일기 보기·원본 내보내기·문의하기 줄은 T046을 이은 뒤 보인다
- [x] T046 [P] [US1] `shared/feature/migration/{kt}/feature/migration/LegacyReaderScreen.kt`(사본 읽기 전용, `_id` 역순 60줄씩), `LegacyExport.kt`(FileKit 저장 창으로 `1일1일기-원본-yyyyMMdd.zip`: 사본, `media/` 이전 사진, `README.txt`), `ContactMail.kt`(`sjunh812@naver.com`, 단계·종류·구간만). 2026-11-01: 저장 창은 FileKit 대신 Android SAF(문서 만들기)를 바로 쓴다(옛 앱이 있는 곳은 Android뿐). 예전 일기는 `_id` 역순이 아니라 일기 날짜 최근 순이다(contracts/migration-flow.md를 고침). 에뮬레이터에서 실패 → 예전 일기 보기 → zip 저장 → Gmail 작성 화면 → 다시 시도까지 확인했다
- [x] T047 [US1] `shared/app/{kt}/app/gate/MigrationGate.kt`: `contracts/migration-flow.md` 화면 상태 표대로, `Failed` 동안 앱 틀로 가지 않음, 결과는 한 번만
- [x] T095 [US1] 이전 뒤 후속 작업(FR-011): `shared/core/migration/{kt}/core/migration/MigrationFollowUp.kt`(이전이 끝난 기기에서만 일기 DB를 열고 `retryPartial`·`recheckMissingPhotos`), `MigrationRunner`가 처음 옮긴 `versionCode`를 `retriedVersionCode`로 적음, `MigrationViewModel`이 관문을 지난 뒤 한 번 돌리고 `MigrationGate`가 토스트로 알림(세 언어). 2026-11-01: 시험(이전 실행기 11, 화면 상태 3)이 Android·iOS에서 통과했고, 에뮬레이터에서 지워졌던 사진을 되돌려 놓자 "찾을 수 없던 사진 1장을 찾았어요"가 뜨는 것을 봤다
- [x] T048 [US1] `shared/feature/settings/{kt}/feature/settings/MigrationReportScreen.kt`: `Partial`인 기기에서만 설정에 보임. 결과, 원본 파일 내보내기, 문의하기, "앱을 업데이트하면 한 번 더 읽어 봐요" 안내. 다시 시도 버튼은 없음(FR-010, FR-011). 2026-11-01: `MigrationReportScreen`, `MigrationReportViewModel`(시간대 날짜), `showsMigrationReport`(일부만일 때만 설정 줄), 세 언어 문구, 값 줄 `DiaryValueRow`. 시험(Android·iOS 3개) 통과, 에뮬레이터 화면 모음(`ScreenCatalogActivity`)으로 세 언어·다크를 Figma M5와 견줬다. 설정 목록의 줄과 이동은 설정 틀(US3)에서, 문의하기의 시트는 T092에서 잇는다. Figma M5·B6의 문의하기 아이콘을 M3와 같은 메일로 바꿨다
- [x] T092 [US1] 복구 문의(FR-010a): `shared/feature/settings/{kt}/feature/settings/SupportAttachSheet.kt`(첨부해서·첨부 없이, 무엇이 들어 있는지와 지우는 때), `shared/core/migration/{android}/core/migration/SupportBundle.kt`(문의용 zip, 공유 뒤·하루 뒤 지움), 개발 쪽 `scripts/recover/recover-note-db.sh`(`sqlite3 .recover` → 정상 `note.db` → zip)와 복구 절차 문서 `scripts/recover/README.md`(받은 파일 지우는 때 포함). Figma "표지 · 흐름" M5 · 복구 문의. 2026-11-01: 시트는 `shared/feature/migration/{kt}/feature/migration/SupportAttachSheet.kt`(앱 틀이 일부만 결과 화면과 설정 결과 화면 양쪽에서 띄운다, 기능 모듈끼리 서로 모르게), 묶음은 `SupportBundle`(Android) + `SupportFiles`(공통, 만든 시각을 적고 하루 뒤 이전 후속 작업이 지움), 메일은 `ACTION_SEND` + mailto 선택자와 FileProvider(`composeApp` `support_paths.xml`). 시험(SupportFiles 3, SupportBundle 2) 통과, 깨뜨린 견본 DB로 복구 스크립트가 400편 중 388편을 살렸다. 에뮬레이터 화면 모음으로 시트 세 언어를 봤다. Gmail이 사용자 계정이라 실제 첨부 메일 작성은 열지 않았다
- [x] T049 [P] [US1] 개발 빌드만: `composeApp/src/debug/kotlin/org/sjhstudio/diary/debug/KillAtHook.kt`(`files/debug/kill_at`의 단계에서 프로세스 종료). 2026-11-01: `KillAtHook`은 `MigrationEvents.stepStarted`(상태를 적은 직후)에서 끊고 파일을 지운다. 개발 빌드만 `DebugApplication`이 Koin의 `MigrationEvents`를 감싸 덮어쓴다
- [x] T050 [US1] `scripts/legacy/build-legacy-apk.sh`(1.7.4 `94179a4`를 임시 worktree로 꺼내 빌드, JBR 17), `scripts/legacy/seed.sh`(`run-as`로 견본 자료 넣기), `scripts/legacy/upgrade-test.sh`와 `composeApp/src/androidTest/kotlin/org/sjhstudio/diary/UpgradeVerificationTest.kt`. 2026-11-01: 세 스크립트를 만들어 API 36 에뮬레이터에서 돌렸다(`test-report.md`). 계측 시험은 아직이다. 2026-11-01: `UpgradeVerificationTest`(일기 값, 사진 순서·상태, 찾을 수 없는 사진, 썸네일, 큰 견본 수)와 스크립트 옵션 `COUNT`, `KILL_AT`, `INSTRUMENT`를 더했다
- [x] T051 [US1] quickstart.md §2·§3 조합을 API 24·29·35·36에서 1.7.4로 돌리고 결과와 이전 시간(1,000편·사진 1,000장)을 `specs/001-foundation-migration/test-report.md`에 남긴다(FR-032, SC-001~SC-003). 이전 결과로 왕복 시험용 견본 자료(`diary.db`, `media/`)를 `shared/core/testing/src/androidHostTest/resources/migrated/`에 남겨 스펙 003이 쓰게 한다(FR-034). 2026-11-01: API 36 폰과 API 35 태블릿에서 기본·강제 종료 네 단계·1,000편+사진 1,000장을 돌렸다(이전 17.3초·2.6초, test-report.md). 견본 자료는 `shared/core/testing/src/androidHostTest/resources/migrated/`. 2026-11-02: API 24·29 이미지를 받아 같은 조합을 돌렸다(API 24 큰 견본 2.3초, API 29 2.2초). API 24에서 앱이 켜자마자 꺼지는 결함(java.time, desugaring 꺼짐)을 찾아 고쳤다. quickstart §2의 나머지 견본(wal-only 등)은 seed에 없고 공통 시험이 본다

**Checkpoint**: 이전만으로 업데이트한 기기의 기록이 모두 보인다

---

## Phase 5: User Story 2 - 잠금이 그대로 지켜진다 (Priority: P1)

**Goal**: 옛 비밀번호·지문으로 잠긴 채 열리고, 다시 잠기고, 최근 앱에 내용이 비치지 않고, 잊어도 기기 안에서 다시 정한다

**Independent Test**: quickstart.md §4

### Tests for User Story 2 ⚠️

- [x] T052 [P] [US2] `shared/core/data/{test}/core/data/lock/LockRepositoryTest.kt`: 확인값 형식(`HMAC-SHA256(key, salt ‖ PIN)`, Base64), 맞음·틀림, 5번째에 30초 `Waiting`과 다시 켜도 이어짐, `KeyMissing`, `disable`은 맞을 때만. 2026-11-02: 기기 시각을 뒤로 돌려도 30초보다 오래 묶이지 않는 것, 확인할 수 있으면 비밀번호 없이 끌 수 없는 것, 쓰고 난 PIN을 지우는 것도 본다
- [x] T053 [P] [US2] `shared/app/{test}/app/lock/LockControllerTest.kt`: `immediately`·`one_minute`·`five_minutes`·`cold_start`, 벽시계를 바꿔도 단조 시계 기준, 프로세스 새로 시작은 늘 잠금
- [x] T054 [P] [US2] `shared/core/migration/{test}/core/migration/LegacyLockBootstrapTest.kt`: `use_pw` + 4자리 → 켜짐·확인값, 잘못된 모양 → 켜짐·확인값 없음, `finger_print`는 잠금이 켜질 때만, 저장소에 비밀번호 없음. 2026-11-02: 이전을 시작한 뒤(`NotStarted`가 아니면)에는 옛 파일로 잠금을 다시 켜지 않는다. 사용자가 새 앱에서 끈 잠금이 되살아나지 않게 하려고 새 표시값 없이 이전 단계로 가린다. 공통 코드라 `{test}`에 둔다
- [x] T055 [P] [US2] `shared/app/{host}/app/lock/LockGateUiTest.kt`: 잠긴 동안 안쪽 화면을 그리지 않음, 열면 보던 화면 키로 돌아감, 생체 취소 → 숫자판. 2026-11-02: 화면 상태 시험(`shared/feature/lock/{test}/.../LockViewModelTest.kt`: 틀림·남은 횟수, 30초 기다림, 두 번 입력, 뒤로)과 업그레이드 스크립트의 `LOCK=0427`(잠긴 동안 이전 시작 안 함)까지 했다. 2026-11-03: Compose UI 시험 `shared/app/src/iosTest/kotlin/org/sjhstudio/diary/app/gate/LockGateUiTest.kt`(잠긴 동안 안쪽 화면 없음, 다시 열면 저장 상태 그대로, 생체 취소 → 숫자판, 생체 성공 → 열림)를 iOS 시뮬레이터에서 돌린다. Android 호스트 JVM에서 그리려면 Robolectric이 있어야 해서 오발있처럼 iosTest에 두었다

### Implementation for User Story 2

- [x] T056 [P] [US2] `shared/core/platform/{kt}/core/platform/PinKeyStore.kt` expect, `{android}` Keystore HMAC-SHA256(별칭 `diary.pin.v1`, 사용자 인증 요구 없음), `{ios}` 키체인 256비트 키(`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) + `CCHmac`, 각 플랫폼 계측·시뮬레이터 시험. 2026-11-02: Android는 `composeApp/src/androidTest/.../PinKeyStoreTest.kt`로 API 36·24 에뮬레이터에서 통과. iOS 시험 실행기는 앱 묶음 없이 돌아 키체인을 쓸 수 없어서(errSecNotAvailable) HMAC 계산만 RFC 4231 시험값으로 보고, 키체인 저장은 앱을 올려 봤다: iPhone 17 시뮬레이터에서 잠금을 켜고 앱을 새로 연 뒤 같은 비밀번호로 열렸다(2026-11-02)
- [x] T057 [US2] `shared/core/data/{kt}/core/data/lock/LockRepository.kt`(`contracts/lock.md` 인터페이스, 상수 시간 비교, PIN 배열 지우기)와 `shared/core/testing/{kt}/core/testing/FakeLockRepository.kt`. 2026-11-02: 시험은 대역 저장소 대신 실제 `DefaultLockRepository`에 `FakePinKeyStore`(키를 잃는 흉내 포함)와 메모리 DataStore를 끼워 쓴다
- [x] T058 [P] [US2] `shared/core/platform/{kt}/core/platform/DeviceAuthenticator.kt` expect, `{android}` `androidx.biometric` 1.1.0(열기 `BIOMETRIC_WEAK`, 분실 `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`, `isDeviceSecure`), `{ios}` `LAContext`, `iosApp/iosApp/Info.plist`에 `NSFaceIDUsageDescription`, `FakeDeviceAuthenticator.kt`. 2026-11-02: 시스템 창 글은 부르는 쪽이 문자열 리소스에서 넘긴다(`AuthPrompt`). Face ID 글은 `project.yml`과 언어별 `InfoPlist.strings`. 대역은 창을 띄울 수 없는 자리에 쓰는 `NoDeviceAuthenticator`다. `MainActivity`를 `FragmentActivity`로 바꿨다
- [x] T059 [US2] `shared/core/migration/{android}/core/migration/legacy/LegacyLockBootstrap.kt`와 `Startup`에서 스플래시 동안 부르기(research R16). 2026-11-02: 원본 자리만 Android가 알면 되어 공통 코드(`core/migration/LegacyLockBootstrap.kt`)에 두었다. 잇기가 실패하면 시작 단계가 잠근 채 새 비밀번호를 정하게 한다
- [x] T060 [US2] `shared/core/platform/{kt}/core/platform/AppForeground.kt`(Android `ProcessLifecycleOwner`, iOS 앱 상태 알림, 단조 시계)와 `shared/app/{kt}/app/lock/LockController.kt`, `shared/app/{kt}/app/gate/LockGate.kt`. 2026-11-02: 잠긴 동안 안쪽 화면은 `SaveableStateHolder`로 저장 상태만 들고 있다. 폰 잠금 확인으로 다녀오는 동안은 다시 잠그지 않는다(`withoutRelock`)
- [x] T061 [P] [US2] `shared/feature/lock/{kt}/feature/lock/`에 `LockScreen.kt`(4칸 점, 숫자판, 생체 버튼, 기다림 남은 시간), `PinSetupScreen.kt`(두 번 입력, 폰 잠금 안내), `ForgotPinFlow.kt`(폰 잠금 확인 → 새 비밀번호, 없으면 백업 안내), `LockSettingsScreen.kt`(켜기·끄기, 바꾸기, 생체, 다시 잠기는 시간, 생체 못 씀 안내), 문구는 `{res}/values/strings.xml`. 2026-11-02: 잠금 화면(L1), 비밀번호 정하기(L2), 잊어버렸을 때 시트(L3)와 이를 잇는 `LockFlow`, 디자인 시스템 숫자판(`DiaryNumberPad`, `DiaryPinDots`)과 프리뷰를 만들었다. 하드웨어 키보드로도 숫자를 받는다. L3 본문의 "(PIN, 패턴, 지문)"은 iOS와 맞지 않아 뺐다(Figma 화면·프로토타입도 같이 고침). 백업 안내 버튼은 스펙 003 뒤에 붙인다. 잠금 설정(L4)도 만들었다(`LockSettingsRoute`: 켜기는 새 비밀번호 두 번, 끄기는 지금 비밀번호, 바꾸기는 지금 → 새 비밀번호). 설정 목록(S6)에서 들어가는 길은 설정 화면(US3)과 함께 잇고, 그때까지는 개발 빌드 화면 모음(`lock_settings`)으로 본다. 생체 인증 줄 이름은 iOS(Face ID)와 맞추려고 Figma의 "지문으로 열기" 대신 "생체 인증으로 열기"로 했다(Figma 화면·프로토타입도 같이 고침) 2026-11-05: 나머지를 끝냈다. 잠긴 동안에는 백업 화면으로 갈 수 없어 "백업 안내" 버튼은 두지 않고 시트 문구로 안내한다(Figma도 고침)
- [x] T062 [US2] 최근 앱 가림: `composeApp/src/main/kotlin/org/sjhstudio/diary/RecentsGuard.kt`(API 33+ `setRecentsScreenshotEnabled(false)`, 24~32 `onPause` `FLAG_SECURE`·`onResume` 끔), `iosApp/iosApp/PrivacyCover.swift`(비활성 때 가림 창). 잠금이 켜졌을 때만. 2026-11-02: Android 쪽을 만들었다. API 36·29는 가려지고 API 24는 가려지지 않았다. 사용자가 캡처를 막지 않는 지금 방식을 골랐다(O3 닫음, 2026-11-02). iOS는 SwiftUI `scenePhase`가 활성이 아닐 때 덮는 `PrivacyCover`로 만들었고, 시뮬레이터 앱 전환기에서 앱 카드가 덮이는 것을 봤다
- [x] T063 [P] [US2] `composeApp/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml`: 잠금 저장소와 `no_backup/thumbs`, `files/migration`, `files/legacy-snapshot`을 클라우드 백업·기기 간 이전에서 뺀다(research R11, O5). 2026-11-02: O5 결정대로 다섯 영역(root, file, database, sharedpref, external)을 모두 뺐다. `allowBackup="false"`여도 Android 12부터 기기 간 이전이 돌아서 규칙 파일을 같이 둔다
- [ ] T064 [US2] quickstart.md §4를 API 24·29·31·35와 iOS 시뮬레이터에서 하고 research O3(Android 7~12 최근 앱 가림) 결과를 `test-report.md`에 쓴다. 가려지지 않으면 사용자에게 묻는다. 2026-11-02: API 36·29·24에서 잠금 업데이트, 최근 앱 가림(O3 닫음), API 36에서 비밀번호를 잊어버렸을 때 흐름을 했다. 30초 기다림이 앱을 다시 켜도 이어지는 것도 봤다. iOS 시뮬레이터에서 잠금 켜기와 다시 열기도 봤다. iOS 앱 전환기 가림 창도 봤다. 2026-11-04: API 31에서 잠금 업데이트와 최근 앱 가림을 했다(홈을 거치면 가려지고, 앱에서 바로 최근 앱 버튼이면 보임). API 36에서 생체 인증으로 열기(업데이트 직후, 다시 켠 뒤)도 했다. API 35는 폰 이미지가 없어 남았다

**Checkpoint**: 잠금 켠 기기가 업데이트 뒤 잠금 화면부터 보고, 이전 화면도 잠금 뒤에 있다

---

## Phase 6: User Story 3 - 설정이 그대로다 (Priority: P1)

**Goal**: 글꼴, 본문 글자 크기, 화면 모드, 위치 안내, 매일 알림이 이어진다

**Independent Test**: quickstart.md §2 `fonts`·`locked` 조합과 다음 알림 시각

### Tests for User Story 3 ⚠️

- [x] T065 [P] [US3] `shared/core/migration/{test}/core/migration/LegacySettingsMapperTest.kt`: `font_key` 전체 표(-1·0·100·1~13·그 밖, -1·0·1·3·100이 아니면 `font_replaced_from`, data-model.md DiaryFont 표 그대로), `font_size` 0·1 `small`·2와 없음 `medium`·3 `large`·4 `xlarge`, `mode_key`, `ask_location` 없음 → true, 알림 시각 범위 밖 → 22:00
- [x] T066 [P] [US3] `shared/core/data/{test}/core/data/reminder/ReminderRepositoryTest.kt`: 다음 알림 시각(오늘 지났으면 내일), 시간대 바뀜, 꺼짐

### Implementation for User Story 3

- [x] T067 [US3] `shared/core/migration/{kt}/core/migration/LegacySettingsMapper.kt`와 스플래시 동안 `SettingsStore`에 쓰기(이미 했으면 건너뜀), `LegacyBackupLink` 담기(FR-012). 옮긴 글꼴이 받아야 하는 글꼴이면 이전 직후 `FontInstaller`로 받기 시작(받을 때까지 Pretendard). 2026-11-02: 대응표(`LegacySettingsMapper`)와 스플래시 동안 쓰기(`LegacySettingsBootstrap`, 잠금 잇기처럼 이전을 시작하기 전에만)를 했다. 바뀐 글꼴의 옛 이름은 1.7.4 글꼴 화면 이름을 남긴다. API 36 업그레이드로 확인했다. 2026-11-04: 고른 글꼴이 받는 글꼴인데 없으면 앱을 연 뒤 받기 시작한다(`shared/app/.../font/ChosenFontDownload.kt`, 이전 직후 포함, 받을 때까지 Pretendard). 결과의 `settingsMigrated` 목록은 이전을 마칠 때 사본의 옛 설정 파일(비밀번호를 뺀 것)에서 실제로 있던 설정만 센다(`LegacySettingsMapper.migrated`, 2026-11-04)
- [x] T068 [P] [US3] 본문 글꼴 가운데 앱에 넣는 5종(Pretendard, 국립공원 꼬미, 나눔손글씨 펜, 카페24 써라운드 OTF, 마루 부리 OTF, 2026-11-03 고침)을 `shared/core/designsystem/{res}/font/`에 원본 그대로 넣고, 나머지 5종은 `fonts/<id>/` 필요할 때 설치 모듈(자산만)로 두고 `shared/core/platform/{kt}/core/platform/FontInstaller.kt` expect/actual(받기, 진행, 실패, `no_backup/fonts/<id>.ttf`로 복사)과 글꼴마다 미리보기 그림을 넣고 `shared/core/designsystem/{kt}/core/designsystem/theme/DiaryFonts.kt`(ID → FontFamily, 고른 것만 읽음), 본문 글자 크기를 `DiaryTypography`의 본문 스타일에 거는 `BodyTextStyle.kt`(기기 글자 크기 위에 곱함, research R17) 2026-11-03: 공식 배포처에서 받은 글꼴로 했다. 앱에 넣음 다섯 종(Pretendard, 꼬미, 나눔손글씨 펜, 카페24 써라운드 OTF, 마루 부리 OTF), 고를 때 받음 다섯 종은 `fonts/<id>`(Play 필요할 때 설치 모듈, iOS는 같은 파일을 On-Demand Resources 태그로). `FontInstaller`(Android `AndroidFontInstaller`, iOS `IosFontInstaller`)가 받아 `AppFiles.fontDirectory`에 복사하고, `core/ui` `diaryBodyTextStyle`이 고른 글꼴과 본문 글자 크기를 합친다. 받기 전 글꼴은 이름 그림(그 글꼴로 그린 한국어·로마자 이름, 64KB). API 36 에뮬레이터에서 bundletool 로컬 시험 모드로 감자꽃 받기 → 복사 → 바로 바뀜을 봤다. iPhone 17 시뮬레이터에서도 감자꽃 받기 → 복사 → 바로 바뀜을 봤다
- [x] T069 [P] [US3] `shared/feature/settings/{kt}/feature/settings/`에 `SettingsScreen.kt`(틀), `FontSettingsScreen.kt`(10종 + 시스템, 미리보기 그림, "받기 · 크기"와 받는 중 진행, 인터넷 없음 안내), `TextSizeSettingsScreen.kt`(5단계, 미리보기 문장), `ThemeSettingsScreen.kt`, `ReminderSettingsScreen.kt`(허락 필요 줄), `LicensesScreen.kt`(OFL 전문, 네이버 글꼴 라이선스, 카페24 약관, 공공누리 출처), "글꼴이 바뀌었어요" 토스트 한 번 2026-11-02: 설정 목록(S6, 스펙 001 줄만), 본문 글자 크기(G2), 화면 모드(G3)와 이를 잇는 `shared/app/{kt}/app/settings/SettingsGraph.kt`(잠금 설정 L4, 기록 이전 결과 M5 포함)를 만들었다. 개발 빌드 화면 모음 `settings`로 에뮬레이터에서 봤다. G2 견본 날짜는 플랫폼 형식(`fullDate`)이라 Figma의 "10월 31일 토요일"과 꼴이 다르다. 고지는 남았다. 2026-11-03: 매일 알림(R1)을 만들었다(`ReminderSettingsScreen.kt`: 켜기·끄기, 켰을 때만 알림 시각 줄, 시각은 바텀 시트의 디자인 시스템 휠 `DiaryTimeWheel`로 고르고 저장, 켰는데 알림이 허용되지 않았으면 상태 상자 `DiaryStatusBox`와 허용하기). 켤 때 아직 허용되지 않았으면 시스템 창으로 묻고, 휴대폰 설정에서 돌아오면 다시 읽는다. API 36 에뮬레이터에서 켜기 → 권한 창 → 허용 → 시각 9:02로 저장 → 다음 날 21:02 예약까지 봤다 글꼴(G1)도 만들었다(`FontSettingsScreen.kt`: 미리보기 상자, 줄 이름을 그 글꼴로, 받기 · 크기, 받는 중 %, 인터넷 없음·실패 토스트) 2026-11-04: 오픈소스 라이선스(I1·I2, T087)와 "쓰던 글꼴 대신 ○○ 글꼴로 보여 드려요 · 바꾸기" 토스트(`FontReplacedNotice.kt`, 앱 틀이 처음 뜰 때 한 번)를 했다. API 36 업그레이드 시험(옛 글꼴 미니콩다방체 → 꼬미)으로 토스트, 다시 열면 안 뜸, 바꾸기 → 글꼴 화면을 봤다
- [x] T070 [US3] `shared/core/platform/{kt}/core/platform/ReminderScheduler.kt` expect, `{android}` `AlarmManager.setWindow(RTC_WAKEUP, 시각, 10분)`과 알림 채널, `{ios}` `UNCalendarNotificationTrigger(repeats: true)`, `shared/core/data/{kt}/core/data/reminder/ReminderRepository.kt`. 2026-11-02: Android와 `ReminderRepository`(앱을 열 때 다시 예약)를 했다. 옛 앱과 같은 수신기 이름·요청 번호로 예약해 옛 예약을 덮어쓰고, 채널 ID `alarm_id`를 이어 써서 사용자의 채널 설정이 남는다. 2026-11-03: iOS `IosReminderScheduler`(그 시각에 날마다 되풀이하는 달력 알림 하나, 같은 이름으로 다시 걸면 바뀜, 글은 iosApp `Localizable.strings` 세 언어)와 알림 허용 읽기·묻기를 붙였다. iPhone 17 시뮬레이터에서 오후 11:54로 맞춰 알림이 오는 것을 봤다(`test-report.md`)
- [x] T071 [US3] `composeApp/src/main/kotlin/org/sjhstudio/diary/helper/AlarmReceiver.kt`(옛 이름 그대로, 알림 띄우고 다음 예약), `BOOT_COMPLETED`·`MY_PACKAGE_REPLACED` 받기, 이전 전이면 옛 `pref2.xml`을 읽기만 해서 시각 얻기(research R15). 2026-11-02: 수신기도 시작 단계(옛 설정 잇기)가 끝나기를 기다린 뒤 새 설정으로 예약한다. 시각·시간대 바뀜(`TIME_SET`, `TIMEZONE_CHANGED`)에도 다시 예약한다
- [x] T072 [US3] 화면 모드를 `DiaryTheme`에 잇고(시스템·라이트·다크) iOS도 같게 한다. 2026-11-02: 공통 `DiaryApp`이 설정의 화면 모드로 `DiaryTheme`을 고르고 iOS도 같은 코드를 쓴다. 시작 단계에서 읽은 값으로 첫 프레임부터 맞는다
- [x] T073 [US3] `locked` 조합으로 업데이트한 뒤 앱을 열지 않고 다음 알림이 오는지, 앱이 멈추지 않는지 본다(FR-022, `test-report.md`). 2026-11-03: API 29에서 잠금(0427)과 알림(21:30)을 켠 1.7.4를 새 앱으로 덮어 깔고 열지 않았다. 옛 수신기 이름으로 다음 21:30 예약이 걸리고, 알림이 뜨고(비공개 알림), 누르면 잠금 화면이 먼저 뜬다. 잠긴 동안 기록 이전은 시작하지 않았고 앱은 멈추지 않았다

**Checkpoint**: 옛 설정 여덟 가지가 이어진다(SC-006)

---

## Phase 7: User Story 4 - 새 아이콘으로, 빠르게 열린다 (Priority: P2)

**Goal**: 새 아이콘, 시스템 스플래시 하나, 고정 대기 없음, 옛 바로가기 유지

**Independent Test**: quickstart.md §5

- [x] T074 [P] [US4] Figma 섹션 "앱 아이콘 확정 · A2"에서 내보내기: `composeApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`(전경·배경·단색), `mipmap-*dpi/` PNG(API 24·25용 둥근·사각), `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`(1024 라이트·다크·틴트), 스토어 512·1024는 `docs/store/`(research R24). 2026-11-02: Android를 했다. Figma A2의 배경·전경·단색 겹을 SVG로 뽑아 벡터 그대로 옮겨, 화면 밀도별 PNG 없이 `mipmap-anydpi-v26`(적응형, 단색 포함)과 Android 7용 `mipmap/ic_launcher`·`ic_launcher_round` 벡터로 두었다. API 36·24 런처에서 봤다. iOS 라이트 아이콘 1024(그라데이션 바탕 + 연필, 투명 채널 없음)도 넣었다. 2026-11-04: iOS 18 다크·틴트 1024(투명 채널 없음, `Contents.json`의 luminosity dark·tinted)와 스토어 그림(`docs/store/play-512.png`, `app-store-1024.png`)을 A2에서 뽑았다. iPhone 17 시뮬레이터 홈 화면을 다크·틴트로 바꿔 봤다
- [x] T075 [US4] 스플래시: `composeApp/src/main/res/values/themes.xml`·`values-night/themes.xml`의 `Theme.Diary.Starting`(바탕 `canvas`), `drawable/splash_pencil.xml`(움직임 없음), `MainActivity`의 `installSplashScreen()`과 `setKeepOnScreenCondition { !startup.ready }`, iOS `Info.plist` `UILaunchScreen`과 `LaunchBackground`·`LaunchPencil` 에셋(research R14). 2026-11-02: Android를 했다. 연필은 Figma "SP 시작 화면"의 splash-pencil을 SVG로 뽑아 그대로 옮겼다. iOS도 했다: `LaunchBackground` 색(라이트 흰색, 다크 #16171B)과 `LaunchPencil`(같은 경로의 SVG, 벡터 유지) 2026-11-04: API 29·36 라이트·다크(test-report 스플래시 표)와 iOS 시뮬레이터(실행할 때 연필 화면)에서 봤다
- [x] T076 [US4] `composeApp/src/main/AndroidManifest.xml`에 `org.sjhstudio.diary.SplashActivity` → `MainActivity` `activity-alias`(런처)를 두고, 1.7.4 홈 화면 바로가기가 업데이트 뒤 열리는지 본다. 2026-11-02: 런처를 옛 이름 하나만 두고 `MainActivity`는 내보내지 않는다. API 36·24에서 런처가 `.SplashActivity`로 잡히고 그 이름으로 열리는 것을 봤다(`cmd package resolve-activity`, `am start -n .../.SplashActivity`)
- [ ] T077 [P] [US4] `benchmarks/src/main/kotlin/org/sjhstudio/diary/benchmarks/StartupBenchmark.kt`, `BaselineProfileGenerator.kt`, 만든 profile을 `composeApp/src/main/baseline-prof.txt`로 커밋(SC-007, research R10). 2026-11-03: 생성기(켜서 탭까지는 시작 프로필, 설정 화면 돌기는 일반 프로필)와 프로필 없음·있음을 나란히 재는 시작 측정을 만들고, API 36 에뮬레이터에서 만든 프로필을 Baseline Profile 플러그인 자리 `composeApp/src/main/generated/baselineProfiles/`(baseline-prof.txt, startup-prof.txt)에 넣었다. 출시 축소 빌드 APK에 `assets/dexopt/baseline.prof`가 들어간다. 에뮬레이터 콜드 스타트 중앙값은 프로필 없이 0.97·1.00초, 있으면 1.04·0.99초로 차이가 잡음 안이라, 중급 실기기에서 잰 값은 남았다. 2026-11-11 실기기(갤럭시 SM-S948N, API 37) 10번 중앙값: 홈 목록까지 다 그림(reportFullyDrawn) 165ms → 프로필로 148ms. 상위 기기라 중급 기기 값은 아직 없다
- [x] T078 [US4] Android 12 미만·이상, 라이트·다크에서 스플래시가 한 번만 뜨고 바탕색이 맞는지, 돌아올 때 스플래시가 없는지 캡처로 본다. 2026-11-03: API 29(Android 10)·API 36(Android 16) 런처에서 콜드 스타트를 녹화·연속 캡처로 보았다. 두 버전 모두 라이트는 흰 바탕, 다크는 #16171B 바탕에 연필 하나가 한 번만 뜨고 앱으로 넘어간다. 홈으로 갔다 돌아오면 스플래시 없이 바로 앱이다(`test-report.md`)

---

## Phase 8: User Story 5 - 어떤 기기와 모드에서도 같은 모습의 앱 틀 (Priority: P2)

**Goal**: 디자인 시스템 그대로의 앱 틀과 개발용 견본

**Independent Test**: quickstart.md §6

- [x] T079 [P] [US5] `build-logic/convention/src/test/kotlin/DesignTokenCheckTaskTest.kt`: 금지 패턴이 든 견본 파일이 실패하고 디자인 시스템·미리보기 파일은 통과한다
- [x] T080 [US5] `shared/app/{kt}/app/navigation/`에 화면 키(`contracts/app-shell.md` 표), `NavDisplay`와 표준 장식자(`rememberSaveableStateHolderNavEntryDecorator`, `rememberViewModelStoreNavEntryDecorator`), `screen_view` 이벤트 2026-11-02: 화면 키(`navigation/ScreenKeys.kt`, sealed로 묶어 직렬화기 등록), `NavDisplay`와 표준 장식자 둘을 붙였다(`scaffold/AppScaffold.kt`, 설정 탭 화면은 `settings/SettingsEntries.kt`). `screen_view`는 보는 화면이 바뀔 때 화면 키 이름만 보낸다. 002·글꼴·알림·고지 키는 남았다 2026-11-04: 글꼴·알림·오픈소스 라이선스(목록·전문) 키를 더했다. 쓰기 같은 스펙 002 화면 키는 그 화면과 함께 더한다
- [x] T081 [US5] `shared/app/{kt}/app/scaffold/AppScaffold.kt`: material3-adaptive로 폭 600dp 미만 하단 탭(64 + 안전 영역), 이상 레일(80), 탭 홈·달력·돌아보기·설정, 쓰기 버튼 자리, 홈·달력·돌아보기는 빈 상태 컴포넌트(스펙 002가 채움) 2026-11-02: 폭 600dp 미만 하단 탭(64 + 안전 영역, 탭의 맨 위 화면에서만), 이상 레일(80)을 디자인 시스템 `DiaryNavigationBar`·`DiaryNavigationRail`로 만들었다. 탭 아이콘은 Figma S6 경로 그대로, 고른 탭은 굵은 선. 폭은 material3-adaptive 대신 `BoxWithConstraints`로 잰다. 쓰기 버튼과 840dp 두 칸은 스펙 002가 붙인다. API 36 에뮬레이터에서 잠금을 연 뒤 설정 탭까지 봤다 2026-11-05: 쓰기 버튼과 두 칸은 스펙 002에서 붙었다. 폭이 바뀌어도 화면 칸이 같은 자리에 남게 합쳤다
- [x] T082 [P] [US5] `shared/core/designsystem/{kt}/core/designsystem/catalog/DesignCatalogScreen.kt`를 개발 빌드에서만 설정에 연결하고 출시 빌드에서 빠지는지 본다(FR-029). 2026-11-02: 견본 화면(색 토큰 전부와 값, 글자 13단계, 간격·모서리, 컴포넌트)을 만들고 프리뷰가 같은 묶음을 쓰게 했다. 설정의 "개발 > 디자인 시스템 견본" 줄과 화면 키 `DesignCatalogKey`는 Koin에 `DeveloperEntries`가 있을 때만 생기고, 그 등록은 Android `DebugApplication`(src/debug)과 iOS 디버그 바이너리만 한다. 출시 APK의 R8 mapping에 견본 화면 클래스가 없는 것을 봤다(`test-report.md`) 2026-11-04: 설정의 "개발" 묶음을 지웠다. 견본은 Android 개발 빌드의 `ScreenCatalogActivity`에서 `design_catalog`로만 연다(사용자 요청)
- [ ] T083 [P] [US5] 성능 견본: 개발 빌드 견본에 "목록 성능" 화면(Paging 3 60줄씩, 일기 ID 키, 불변 줄 모델, 썸네일 칸)과 `benchmarks/src/main/kotlin/org/sjhstudio/diary/benchmarks/ScrollBenchmark.kt`(1만 편·사진 5천 장 견본 자료). 스펙 002 홈이 같은 방식을 쓴다(FR-035~FR-038, SC-010, SC-011) 2026-11-04: 개발 빌드 견본 `list_performance`(견본 DB 1만 편·사진 5천 장, 60편씩, ID 키, 불변 줄 모델, 사진 칸)를 만들고 에뮬레이터에서 프레임을 쟀다(test-report). `ScrollBenchmark`와 실기기 측정은 출시형 빌드와 스펙 002 홈으로 남겼다
- [x] T084 [US5] Compose UI 시험 `shared/app/{host}/app/scaffold/AppScaffoldUiTest.kt`: 폭 400·700·1000dp, 글자 배율 2.0, 라이트·다크에서 잘림·겹침 없음, 600dp에서 탭 ↔ 레일 2026-11-03: `shared/app/src/iosTest/kotlin/org/sjhstudio/diary/app/scaffold/AppScaffoldUiTest.kt`(400·599 하단 탭, 600·700·1000 레일, 글자 배율 2.0 라이트·다크에서 탭 겹침·잘림 없음, 탭을 누르면 고른 탭과 `screen_view` 화면 키만)를 iOS 시뮬레이터에서 돌린다. 글자 배율 2.0에서 탭 이름 넷이 모두 잘리는 것을 찾아, 탭·레일 이름만 1.3배까지 커지게 고쳤다(`docs/design.md` 하단 탭, DECISIONS 2026-11-03, 세 언어 이름 시험 `DiaryNavigationTest`)

---

## Phase 9: Polish & Cross-Cutting

- [x] T085 `CLAUDE.md` "빌드와 시험"에 실제 명령(검사, 시험, iOS, 업그레이드 시험, 벤치마크)을 옮긴다 2026-11-04: 검사·시험·빌드, 형식, 빌드 규칙 시험, 커밋 훅 시험, 에뮬레이터 설치, 화면 모음, 업그레이드 시험, Baseline Profile·시작 시간, 받는 글꼴 시험, 라이선스 목록 다시 만들기, iOS 빌드가 들어 있다
- [ ] T086 [P] SC-009 점검: Firebase DebugView로 한 바퀴 돈 이벤트가 고정 표 안에 있고 값에 글·장소·경로·기분이 없음을 `test-report.md`에 남긴다. 로그에 일기 값을 찍는 곳이 없는지 `grep`으로 본다 2026-11-04: grep 부분을 했다(로그 호출 없음, 값이 든 예외 메시지는 알림 시각 하나). DebugView는 T028 뒤
- [x] T087 [P] 오픈소스·글꼴 고지 문구(OFL 전문, 네이버 글꼴, 카페24 약관, 공공누리 출처)를 `docs/release.md` 글꼴 항목과 맞추고, 카페24 써라운드 약관 원문과 파일 크기를 확인한다. 교보문고가 서면으로 허락하면 `kyobo_2019`를 더한다
  (2026-11-04: 설정 > 정보 > 오픈소스 라이선스(I1·I2), 라이브러리 목록은 AboutLibraries. 네이버 전문 페이지 직접 확인은 `docs/release.md`에 남김. 교보는 허락 전이라 넣지 않음)
- [ ] T088 [P] research 열린 결정 O1(썸네일 크기), O2(Pretendard 서브셋)를 측정해 닫고 research.md·`docs/DECISIONS.md`에 남긴다
- [x] T089 문서 동기화: 바뀐 규칙이 `docs/migration.md`, `docs/backup.md`, `docs/design.md`, 헌법, 스펙 001·002, `docs/DECISIONS.md`에 같은 내용으로 있는지, Figma 진행 현황이 맞는지 본다 2026-11-04: 점검에서 나온 12건(문의 첨부 범위, 받은 글꼴 자리, 백업 문서와 003 계약, 편지 자리, 광고 없는 곳, 묶음 머리 여백, 글꼴 크기, T068 글, 화면 이름, 탭 이름 1.3배, `canvasGrouped` 쓰임, 낱말)을 고치고 Figma 진행 현황 표를 11-04 기준으로 고쳤다
- [ ] T090 quickstart.md 전체를 처음부터 돌려 통과를 확인하고 `checklists/data-safety.md`를 검토자에게 넘긴다

---

## Dependencies & Execution Order

- Phase 1(프로토타입·디자인)은 코드와 상관없이 지금 할 수 있다. 화면 코드(T045, T061, T069, T081)는 T005(흐름 확정)와 T006(Figma) 뒤에 한다.
- Phase 2 → Phase 3 → 이야기들. Phase 3이 끝나면 US1·US2·US3은 나란히 할 수 있지만 서로 닿는 곳이 있다:
  - US2의 T059(잠금 잇기)와 US3의 T067(설정 잇기)은 US1의 T040(`LegacyPrefsReader`)에 기댄다.
  - US1의 T047(`MigrationGate`)은 US2의 T060(`LockGate`) 안쪽에 들어간다. 잠금 없이도 US1을 시험할 수 있다.
- US4·US5는 Phase 3 뒤 언제든. T083(성능 견본)은 T042(썸네일)와 T023(Paging DAO)에 기댄다.
- 스펙 002 개발은 Phase 3과 T042, T080, T081이 끝난 뒤 시작할 수 있다.

## Parallel Example: User Story 1

```text
T032 LegacyTextTest, T033 LegacyEntryMapperTest, T034 PhotoPathsTest, T035 FingerprintTest, T036 LegacyPrefsReaderTest  (시험 먼저, 나란히)
T038 LegacyText, T039 LegacyEntryMapper·PhotoPaths·Fingerprint, T042 ImageDecoder·ThumbnailRepository  (구현, 나란히)
T045 이전 화면들, T046 옛 일기 읽기·내보내기·문의  (T043 뒤 나란히)
```

## Implementation Strategy

1. **MVP**: Phase 2·3 + US1. 견본 자료 업그레이드 시험이 통과하면 기록 이전은 믿을 수 있다.
2. **R1에 꼭 필요**: US2·US3(P1). 잠금과 설정이 안 이어지면 업데이트를 낼 수 없다.
3. **그다음**: US4(아이콘·스플래시·시작 시간), US5(앱 틀·견본·성능 견본). 스펙 002가 이 틀 위에 화면을 얹는다.
4. 각 단계 끝에 `./gradlew spotlessCheck designTokenCheck allTests`가 통과하고, 커밋은 `CLAUDE.md` 커밋 규칙대로 작업 브랜치에 바로 푸시한다.
