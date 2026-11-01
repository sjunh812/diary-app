---

description: "스펙 002 일기 핵심 화면 작업 목록"
---

# Tasks: 일기 핵심 화면

**Input**: `specs/002-diary-core/`의 spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md

**Prerequisites**: Phase 1의 프로토타입·Figma 작업(T001~T014)은 지금 할 수 있다. T015부터는 스펙 001(기반과 이전)의 뼈대·DB·디자인 토큰이 있어야 한다(plan.md "스펙 001에 기대는 것").

**Tests**: 사용자가 시험 작업을 요청했다. 헌법 I(기록 보존)·II(내용 비공개)를 지키는 시험을 각 단계 맨 앞에 두고, 먼저 실패하는 것을 본 뒤 구현한다.

**Organization**: 사용자 이야기별로 묶었다. 이야기마다 혼자 시험할 수 있다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 끝나지 않은 작업에 기대지 않아 나란히 할 수 있다
- **[Story]**: US1~US7(spec.md)

## Path Conventions

- `{kt}` = `src/commonMain/kotlin/org/sjhstudio/diary`, `{test}` = `src/commonTest/kotlin/org/sjhstudio/diary`
- `{android}` = `src/androidMain/kotlin/org/sjhstudio/diary`, `{ios}` = `src/iosMain/kotlin/org/sjhstudio/diary`
- 모듈은 plan.md "Source Code" 구조를 따른다. 예: `shared/feature/write/{kt}/feature/write/WriteViewModel.kt`
- Figma는 파일 "1일 1일기"(`HQOqmp9kJv5EEpQH2mXMj9`)의 페이지와 섹션 이름을 경로로 쓴다

---

## Phase 1: Setup (프로토타입, 디자인, 모듈)

**Purpose**: `/diary-flow` 3·4단계와 기능 모듈 생성

### 프로토타입 (3단계, Figma, 지금 가능)

- [x] T001 화면 흐름(이야기별 지나가는 화면, 뒤로 가기)과 상태 목록을 `specs/002-diary-core/prototype.md`에 쓴다(`contracts/navigation.md` 기준)
- [x] T002 레퍼런스 시각 규격(TDS, SEED, Apple HIG, Material 3)을 숫자로 조사하고, 기존 라벤더 계열에서 출발한 디자인 시스템 시안을 Figma "디자인 시스템" 페이지에 그려 사용자가 고르게 한다. 고르면 `docs/design.md`를 확정으로 바꾸고 변수·글자 스타일을 만든다
- [x] T003 "표지 · 흐름" 섹션 "프로토타입 · 일기 핵심"에 폰 프레임 홈(S1), 홈 빈 상태, 상세(S3), 사진 크게(S3-a)를 만든다
- [x] T004 같은 섹션에 쓰기(S2), 기분·날씨 시트, "쓰던 일기가 있어요" 시트, 사진 한도 상태, "임시 저장했어요"·"저장했어요" 상태를 만든다
- [x] T005 같은 섹션에 달력(S4)과 빈 날, 검색(S1-a)과 결과 없음, 돌아보기(S5), 설정(S6), 휴지통(S6-7), "휴지통으로 옮겼어요 [되돌리기]" 상태를 만든다
- [x] T006 같은 섹션에 넓은 화면 프레임(홈 목록|상세, 달력|그날 목록, 쓰기 본문+옆 칸)을 만든다
- [x] T007 프레임을 프로토타입 연결로 잇고 흐름 시작점을 둔 뒤 사용자에게 흐름 확정을 받아 `prototype.md`에 적고 spec.md·tasks.md에 되먹인다

### 디자인과 Figma (4단계, T007 뒤)

- [x] T008 확정 액센트·기분 색·서식 팔레트 hex를 `docs/design.md`와 `contracts/rich-text-html.md` 팔레트 표에 적고 `docs/DECISIONS.md` D6을 닫는다
- [x] T009 T002에서 만든 변수에 다크 컬렉션 "색 · 다크"를 더하고 확정 액센트로 초안 값을 바꾼다
- [ ] T010 Figma "디자인 시스템" 페이지에 간격·모서리 변수를 더한다(글자 스타일 12종과 색 변수는 T002 최종 점검에서 만들었다)
- [ ] T011 Figma "디자인 시스템" 페이지에 컴포넌트를 만든다: 하단 탭, 레일, 상단 바, 버튼 셋, 목록 줄(글/사진), 날짜 머리, 기분·날씨 칩과 고르기 시트, 사진 격자, 스낵바, 빈 상태, 칩, 달력 칸
- [ ] T012 [P] Figma "화면" 페이지에 섹션 "홈 (S1)", "홈 상태", "일기 상세 (S3)"를 [라이트 | 설명 | 다크 | 태블릿] 줄로 그린다
- [ ] T013 [P] Figma "화면" 페이지에 섹션 "쓰기 (S2)", "쓰기 상태"를 그린다
- [ ] T014 [P] Figma "화면" 페이지에 섹션 "달력 (S4)", "검색 (S1-a)", "돌아보기 (S5)", "휴지통 (S6-7)"과 각 상태를 그리고, "표지 · 흐름"의 화면 흐름 지도와 진행 현황을 고친다

### 모듈 (스펙 001 뒤)

- [ ] T015 `settings.gradle.kts`에 `:shared:feature:home`, `:write`, `:calendar`, `:search`, `:insight`, `:trash`와 `:shared:core:platform`을 더하고 각 `build.gradle.kts`에 001의 convention plugin을 적용한다
- [ ] T016 [P] `gradle/libs.versions.toml`에 compose-rich-editor, FileKit(`filekit-dialogs`), material3-adaptive를 더하고 라이선스(Apache-2.0, MIT)를 주석으로 적는다

---

## Phase 2: Foundational (모든 이야기의 전제)

**⚠️ CRITICAL**: 이 단계가 끝나야 이야기 작업을 시작한다

### 시험 먼저

- [ ] T017 [P] 서식 정리 시험: 허용 목록 밖 태그는 태그만, `script`·`style`·`iframe`은 안의 글까지 버리고, 팔레트 밖 색 `span`은 글만 남기는지 `shared/core/model/{test}/core/model/RichTextSanitizerTest.kt`
- [ ] T018 [P] 평문 시험: "`p` 사이는 빈 줄 하나, `br`과 `li` 사이는 줄바꿈 하나, `li` 앞에는 `• `"와 `toPlainText(fromLegacy(원문)) == 원문`(이모지, 따옴표, `<`, `&`, 빈 줄 여러 개, 10만 자) `shared/core/model/{test}/core/model/PlainTextTest.kt`
- [ ] T019 [P] 내용 비공개 시험: `AnalyticsEvents`의 모든 이벤트 인자가 고정 표의 키·구간 값만 갖는지, 로그 래퍼가 `EntryRow`·`Draft`를 받지 않는지 `shared/core/data/{test}/core/data/PrivacyGuardTest.kt`

### 모델과 저장

- [ ] T020 [P] `Mood`(소문자 ID `angry` … `tired`, 레거시 0~8), `Weather`(소문자 ID `sunny` … `snow`, 레거시 0~6), `MoodColor` 토큰 키를 `shared/core/model/{kt}/core/model/Mood.kt`, `Weather.kt`에 만든다
- [ ] T021 [P] `SearchFilter`, `StatsPeriod`(이전·다음, "오늘이 든 기간 뒤로는 가지 않는다"), `ListPreferences`(`listStyle` 기본 `FEED`, `favoritesOnly`)를 `shared/core/model/{kt}/core/model/`에 만든다
- [ ] T022 `contracts/rich-text-html.md`대로 `RichTextSanitizer`, `toPlainText`, `fromLegacy`를 `shared/core/model/{kt}/core/model/RichText.kt`에 만들어 T017·T018을 통과시킨다
- [ ] T023 001 DB에 `Draft`(키 `"new"` 또는 `Entry.id`, `weatherAutoFilled`), `DraftMedia`, `Entry.deletedAt`과 색인 `(deletedAt, date DESC, time DESC)`, `(deletedAt, isFavorite)`를 001 첫 DB 버전 안에 더한다 `shared/core/database/{kt}/core/database/`
- [ ] T024 [P] `ImageProcessor` expect와 Android actual(표본 축소, EXIF 방향, "긴 변 2,560px을 넘을 때만" 축소, JPEG 85, SHA-256)을 `shared/core/platform/{kt}/core/platform/ImageProcessor.kt`, `shared/core/platform/{android}/core/platform/ImageProcessor.android.kt`에 만든다
- [ ] T025 [P] `ImageProcessor` iOS actual(`UIGraphicsImageRenderer`)을 `shared/core/platform/{ios}/core/platform/ImageProcessor.ios.kt`에 만든다
- [ ] T026 [P] 시스템 공유 창, 햅틱, 고정 가능한 시계를 `shared/core/platform/{kt}/core/platform/ShareSheet.kt`, `Haptics.kt`, `Clock.kt`(+ android/ios actual)에 만든다
- [ ] T027 [P] 대역 `FakeEntryRepository`, `FakeDraftRepository`, `FakeImageProcessor`, `FixedClock`을 `shared/core/testing/{kt}/core/testing/`에 만든다

### 공유 UI와 뼈대

- [ ] T028 [P] 디자인 시스템 컴포넌트(버튼 셋, 시트, 스낵바(누르고 있으면 멈춤), 칩, 빈 상태, 내비게이션 바·레일)를 Figma T011과 같은 이름으로 `shared/core/designsystem/{kt}/core/designsystem/component/`에 만든다
- [ ] T029 [P] `MoodIcon`, `WeatherIcon`(아이콘 + 이름으로 구별), `EntryRow`(글/사진 보기), `DateHeader`, `MonthHeader`, `RichTextView`(팔레트 hex → 테마 토큰, 001 설정의 본문 글꼴과 글자 크기 단계를 따름, FR-014), `PhotoGrid`를 `shared/core/ui/{kt}/core/ui/`에 만든다
- [ ] T030 `contracts/navigation.md`의 화면 키, 하단 탭(600dp 미만)·레일(600dp 이상), 늘 보이는 쓰기 버튼, 두 칸 Scene 전략(840dp 이상, 직접 만든 `SceneStrategy`, 스펙 001 research R25)을 `shared/app/{kt}/app/DiaryNavigation.kt`, `DiaryScaffold.kt`, `ListDetailScene.kt`에 만든다
- [ ] T031 하드웨어 키보드 단축키(Ctrl/⌘ + N, S, F)를 `shared/app/{kt}/app/KeyboardShortcuts.kt`에 만든다

**Checkpoint**: 빈 화면 넷을 탭·레일로 오가고 창 폭에 따라 칸이 바뀐다

---

## Phase 3: User Story 1 - 오늘 일기 쓰기 (Priority: P1) 🎯 MVP

**Goal**: 어디서든 한 번에 쓰기, 기분 선택 사항, 사진 여러 장, 서식, 자동 임시 저장

**Independent Test**: 빈 앱에서 한 편을 써서 저장하고, 쓰는 중 강제 종료 뒤 다시 열어 내용이 남았는지 본다

### Tests for User Story 1

- [ ] T032 [P] [US1] 임시 저장 시험: 입력 1초 뒤 `Draft` 반영, 재시작 뒤 복원, `commit` 실패 시 `Draft` 유지, `commit` 성공 시 `Entry`·`Media`가 한 트랜잭션으로 생기고 `Draft`가 사라짐 `shared/core/data/{test}/core/data/DraftRepositoryTest.kt`
- [ ] T033 [P] [US1] 사진 시험: "무료 10, Plus 30", 이미 한도를 넘은 일기는 더하기만 막힘(`AddPhotosResult.LimitReached`), 같은 사진 두 번이면 파일 하나, 공간 부족이면 `Draft` 변화 없음 `shared/core/data/{test}/core/data/PhotoAttachTest.kt`
- [ ] T034 [P] [US1] 쓰기 ViewModel 시험: 저장 조건("plainText가 비어 있지 않거나, 사진이 1장 이상이거나, mood가 있다"), 같은 기분 다시 누르면 해제, 날짜를 오늘이 아닌 날로 바꾸면 `weatherAutoFilled`인 날씨만 지움 `shared/feature/write/{test}/feature/write/WriteViewModelTest.kt`

### Implementation for User Story 1

- [ ] T035 [US1] `DraftRepository`(`contracts/data-access.md`) 구현을 `shared/core/data/{kt}/core/data/DraftRepositoryImpl.kt`에 만든다
- [ ] T036 [US1] `WriteViewModel`(1초 디바운스 저장, `ON_STOP`·이탈 즉시 저장, 새/고치기 대상, 오늘 일기만 날씨·동네 제안(D7 전까지 비워 둠))을 `shared/feature/write/{kt}/feature/write/WriteViewModel.kt`에 만든다
- [ ] T037 [US1] 쓰기 화면(날짜·시각 줄, 본문 편집기, 첨부 칩 기분·날씨·장소(30자 안팎), 사진 격자 길게 눌러 순서 바꾸기)을 `shared/feature/write/{kt}/feature/write/WriteScreen.kt`에 만든다
- [ ] T038 [US1] 키보드 위 서식 도구(굵게, 밑줄, 형광펜 3, 글자색 5, 글머리 목록, 사진, 기분, 날씨, 장소)를 `shared/feature/write/{kt}/feature/write/FormatToolbar.kt`에 만들고 지금 쓰는 줄이 키보드·도구에 가리지 않게 한다(FR-013)
- [ ] T039 [US1] 기분·날씨 고르기 시트를 `shared/feature/write/{kt}/feature/write/MoodWeatherSheet.kt`에 만든다
- [ ] T040 [US1] FileKit 여러 장 고르기(`maxItems = 남은 장수`)와 카메라를 쓰기 화면 루트에서 기억하는 런처로 `shared/feature/write/{kt}/feature/write/PhotoPicker.kt`에 붙인다(research R4)
- [ ] T041 [US1] "쓰던 일기가 있어요" [이어 쓰기]/[새로 쓰기], 뒤로 가면 "임시 저장했어요", [버리기] 확인, 저장 뒤 "저장했어요"와 상세 이동을 `shared/feature/write/{kt}/feature/write/WriteRoute.kt`에 만든다
- [ ] T042 [US1] 넓은 화면(840dp 이상)에서 본문 가운데(최대 약 680dp) + 옆 칸 배치를 `WriteScreen.kt`에 더한다
- [ ] T043 [US1] 쓰기 화면 문구(해요체, 버튼·제목에 마침표 없음)를 `shared/feature/write/src/commonMain/composeResources/values/strings.xml`에 둔다

**Checkpoint**: US1만으로 쓰기·저장·임시 저장이 된다

---

## Phase 4: User Story 2 - 목록과 상세 (Priority: P1)

**Goal**: 날짜별로 훑고, 읽고, 고치고, 즐겨찾기, 공유

**Independent Test**: 일기 30편에서 훑기, 즐겨찾기만, 다른 달, 한 편 열어 고치기

### Tests for User Story 2

- [ ] T044 [P] [US2] 타임라인 질의 시험: 휴지통 제외, 일기 날짜 최신순, 같은 날 묶음, 즐겨찾기만, 달 이동 위치 `shared/core/data/{test}/core/data/TimelineQueryTest.kt`

### Implementation for User Story 2

- [ ] T045 [US2] `EntryRepository`의 `timeline`, `entry`, `setFavorite`를 `shared/core/data/{kt}/core/data/EntryRepositoryImpl.kt`에 만든다(Paging 3, 스펙 001 research R4. 줄임 사진은 001 `ThumbnailRepository`)
- [ ] T046 [P] [US2] `ListPreferencesRepository`(레거시 `skip_note`를 001이 이은 값)를 `shared/core/data/{kt}/core/data/ListPreferencesRepository.kt`에 만든다
- [ ] T047 [US2] 홈 ViewModel과 화면(달 머리, 날짜 머리, 3줄 미리보기, 즐겨찾기만, 목록 모양, 연·월 고르기 시트, 빈 상태 "아직 쓴 일기가 없어요" [오늘 일기 쓰기]와 "즐겨찾기한 일기가 없어요")을 `shared/feature/home/{kt}/feature/home/HomeViewModel.kt`, `HomeScreen.kt`에 만든다
- [ ] T048 [US2] 상세 화면(서식 본문, 사진, 고치기, 지우기, 즐겨찾기, 공유 = 서식 없는 글)을 `shared/feature/home/{kt}/feature/home/EntryDetailScreen.kt`에 만든다
- [ ] T049 [P] [US2] 사진 크게 보기(넘기기, 확대, MISSING은 "찾을 수 없는 사진")를 `shared/feature/home/{kt}/feature/home/PhotoViewerScreen.kt`에 만든다
- [ ] T050 [US2] 두 칸(840dp 이상)에서 고른 줄 강조, 고른 게 없을 때 안내, 폭이 줄면 상세가 한 칸으로 쌓이는 동작을 `shared/app/{kt}/app/ListDetailScene.kt`와 홈에 잇는다
- [ ] T051 [P] [US2] 홈·상세 문구를 `shared/feature/home/src/commonMain/composeResources/values/strings.xml`에 둔다

**Checkpoint**: US1 + US2로 R1의 최소 일기 앱이 된다

---

## Phase 5: User Story 3 - 달력 (Priority: P2)

**Independent Test**: 하루 2편인 날이 있는 한 달에서 일기 있는 날·빈 날 누르기

- [ ] T052 [P] [US3] 달력 질의 시험: 칸 기분 = "일기 시각이 가장 늦은 것, 같으면 만든 시각이 늦은 것", 편수, 기분 없는 날 중립, 휴지통 제외 `shared/core/data/{test}/core/data/MonthQueryTest.kt`
- [ ] T053 [US3] `EntryRepository.month`, `day`를 `EntryRepositoryImpl.kt`에 더한다
- [ ] T054 [US3] 달력 화면(한 장 한 달 넘기기, 7열 격자, 오늘·주말 구별, 연·월 고르기, [오늘], 그날 목록, 빈 날 "이날 쓴 일기가 없어요" [이날 일기 쓰기])을 `shared/feature/calendar/{kt}/feature/calendar/CalendarViewModel.kt`, `CalendarScreen.kt`에 만든다
- [ ] T055 [US3] 840dp 이상에서 달력 + 그날 목록 두 칸을 `CalendarScreen.kt`에 더하고 문구를 `shared/feature/calendar/src/commonMain/composeResources/values/strings.xml`에 둔다

---

## Phase 6: User Story 4 - 찾기 (Priority: P2)

**Independent Test**: 1,000편에서 한 글자 한국어, 장소, 기분·기간 조건

- [ ] T056 [P] [US4] 검색 시험: 한 글자 한국어, `%`·`_`·`\` 이스케이프, 본문+장소, 조건 조합, 조건만 검색, 휴지통 제외 `shared/core/data/{test}/core/data/SearchQueryTest.kt`
- [ ] T057 [US4] `EntryRepository.search`(`LIKE … ESCAPE '\'`, research R10)를 `EntryRepositoryImpl.kt`에 더한다
- [ ] T058 [US4] 검색 화면(입력, 칩: 기분·날씨·사진 있음·즐겨찾기·기간, 결과 강조, "찾는 일기가 없어요", 돌아왔을 때 입력·조건·스크롤 유지)을 `shared/feature/search/{kt}/feature/search/SearchViewModel.kt`, `SearchScreen.kt`에 만들고 문구를 `strings.xml`에 둔다

---

## Phase 7: User Story 5 - 기분 통계 (Priority: P2)

**Independent Test**: 1년치로 기간 바꾸기, 개수·비율 확인, 기분 눌러 목록 확인

- [ ] T059 [P] [US5] 통계 시험: 주 시작 요일, 월말, 윤년 경계, 기분 없음 개수, 동점, 미래 기간 막힘, 기분 → 검색 결과가 같은 조건 검색과 같음 `shared/core/data/{test}/core/data/StatsRepositoryTest.kt`
- [ ] T060 [US5] `StatsRepository`(`moodCounts`, `moodFlow`, `yearPixels`)를 `shared/core/data/{kt}/core/data/StatsRepositoryImpl.kt`에 만든다
- [ ] T061 [US5] 돌아보기 화면(주·월·년·전체, 이전·다음, 기분 막대와 비율, 가장 많은 기분, 흐름, 연간 지도, "이 기간에 쓴 일기가 없어요", 기분을 누르면 `Search(SearchFilter(moods, period))`)을 Canvas로 `shared/feature/insight/{kt}/feature/insight/InsightViewModel.kt`, `InsightScreen.kt`, `charts/`에 만든다. 차트마다 화면 읽기용 설명을 붙인다

---

## Phase 8: User Story 6 - 지우기, 되돌리기, 휴지통 (Priority: P2)

**Independent Test**: 지운 직후 되돌리기, 휴지통 되살리기·완전히 지우기, 30일 지난 항목 비움

- [ ] T062 [P] [US6] 휴지통 시험: 지우면 모든 질의에서 빠짐, 되돌리기, 30일 지난 것만 비움(고정 시계), 다른 일기가 쓰는 같은 해시 사진은 남김, 사진 정리는 "만든 지 하루가 지난" 미사용 파일만 `shared/core/data/{test}/core/data/TrashRepositoryTest.kt`, `MediaCleanupTest.kt`
- [ ] T063 [US6] `EntryRepository.moveToTrash`·`restore`, `TrashRepository`, `MediaRepository.cleanUnused`를 `shared/core/data/{kt}/core/data/`에 만들고 앱 시작 때 비움 → 정리 순서로 부른다 `shared/app/{kt}/app/StartupJobs.kt`
- [ ] T064 [US6] 지우기 → "휴지통으로 옮겼어요 [되돌리기]" 5초(누르고 있으면 멈춤, research O5 결과 반영)를 상세·목록 줄 메뉴·두 칸 상세에 붙인다 `shared/feature/home/{kt}/feature/home/`
- [ ] T065 [US6] 휴지통 화면(지운 날짜순, "N일 뒤 완전히 지워져요", 되살리기, 완전히 지우기·비우기 확인에 편수)을 `shared/feature/trash/{kt}/feature/trash/TrashViewModel.kt`, `TrashScreen.kt`에 만들고 문구를 `strings.xml`에 둔다

---

## Phase 9: User Story 7 - 넓은 화면 (Priority: P2)

**Independent Test**: 1280dp 태블릿과 폴더블에서 홈·달력·쓰기, 창 크기 바꾸기, 단축키

- [ ] T066 [P] [US7] 창 폭 360/600/840/1280dp와 폭 변경 중 상태 유지(쓰던 내용, 고른 일기, 스크롤)를 Compose UI 시험으로 `shared/app/src/commonTest/kotlin/org/sjhstudio/diary/app/AdaptiveLayoutTest.kt`
- [ ] T067 [US7] 폴더블 접힘선 피하기와 iPad Split View·Stage Manager 크기 변경을 에뮬레이터·시뮬레이터에서 확인하고 어긋난 곳을 `shared/app/{kt}/app/`에서 고친다
- [ ] T068 [US7] 단축키(T031)가 홈·쓰기·검색에서 동작하는지 하드웨어 키보드로 확인하고 결과를 `specs/002-diary-core/quickstart.md` 손으로 확인 표에 적는다

---

## Phase 10: Polish & Cross-Cutting

- [ ] T069 [P] 다크 모드, 시스템 글꼴 200%, TalkBack·VoiceOver 순서를 모든 화면에서 확인하고 고친다(SC-005, SC-008)
- [ ] T070 [P] 001 고정 DB(1만 편, 사진 5천 장)로 홈 첫 화면·검색 1초를 재고, 넘으면 research R10의 FTS5 trigram 또는 O2 페이징으로 고친다(SC-003)
- [ ] T071 [P] 강제 종료 100회 시험 스크립트로 SC-002를 확인한다 `scripts/qa/kill-while-writing.sh`
- [ ] T072 quickstart.md "레거시 기능 대조표"(SC-006)와 spec.md 근거 표의 리뷰 요청 9개(SC-007)를 하나씩 확인하고 결과를 quickstart.md에 적는다
- [ ] T073 에뮬레이터 스크린샷을 Figma "화면" 페이지와 나란히 보고 어긋난 곳을 고친다
- [ ] T074 [P] 처음 보는 참가자 5명 이상에게 "기분·사진 한 장 넣은 오늘 일기 저장"을 시키고 1분 안 완료 비율을 잰다(SC-001). 결과를 `specs/002-diary-core/quickstart.md`에 적는다
- [ ] T075 문서 동기화: 바뀐 규칙이 spec, `docs/design.md`, `contracts/`, `docs/DECISIONS.md`, Figma "표지 · 흐름" 진행 현황에 같게 있는지 본다

---

## Dependencies & Execution Order

- Phase 1 Figma 프로토타입(T001~T007) → 디자인(T008~T014): 001과 상관없이 지금 시작
- 스펙 001 완료 → T015~T016 → Phase 2(T017~T031) → 이야기들
- US1(쓰기)과 US2(목록·상세)는 Phase 2 뒤 나란히 가능. US2의 상세 → 고치기는 US1의 `Write(Edit)`에 기댄다
- US3, US4, US5는 Phase 2와 `EntryRepositoryImpl`(T045) 뒤 서로 나란히 가능. US5의 기분 → 일기는 US4 검색 화면에 기댄다
- US6은 US2(상세·목록 줄)에 기댄다
- US7은 각 화면이 만들어진 뒤 확인·보정
- Polish는 모든 이야기 뒤

## Parallel Examples

```text
# Phase 2 시험 먼저 (나란히)
T017 RichTextSanitizerTest · T018 PlainTextTest · T019 PrivacyGuardTest

# Phase 2 플랫폼과 UI (나란히)
T024 ImageProcessor android · T025 ImageProcessor ios · T028 designsystem 컴포넌트 · T029 core/ui

# Figma 화면 줄 (T011 뒤 나란히)
T012 홈·상세 · T013 쓰기 · T014 달력·검색·돌아보기·휴지통

# US3·US4·US5 (T045 뒤 나란히)
T052~T055 달력 · T056~T058 검색 · T059~T061 통계
```

## Implementation Strategy

1. 지금: Figma 프로토타입(T001~T007)으로 흐름과 액센트를 확정하고 화면 디자인(T008~T014)을 그린다.
2. 스펙 001 뒤 Phase 2 → US1 → US2까지가 MVP다. 여기서 내부 시험(실제 1.7.4 데이터 이전 + 쓰기·읽기)을 한다.
3. US6(휴지통)은 헌법 I 때문에 R1 출시 전에 꼭 넣는다. US3~US5, US7을 더해 R1 범위를 채운다.
4. 이야기마다 Checkpoint에서 멈추고 에뮬레이터로 확인한다.
