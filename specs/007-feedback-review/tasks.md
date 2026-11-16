# 작업 목록: 의견 보내기 카드와 앱 안 리뷰 요청

**입력**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md)
**선행**: 스펙 002(홈 목록), 003(백업 권하기 카드 `BackupNudgeCard`), 004(개발자의 편지), 006(전면 광고 `AdRepository`)

## 1단계: 바탕

- [x] T001 `research.md`에 스토어 지침과 레퍼런스, R1 라이브러리 표를 정리한다
- [x] T002 [P] `PromptStore`와 `PromptPrefs`를 만든다 `shared/core/datastore/src/commonMain/kotlin/org/sjhstudio/diary/core/datastore/PromptStore.kt`, DataStore 등록 `DataStoreFactory.kt`
- [x] T003 [P] 새로 쓴 일기 수 쿼리 `countWrittenSince(since: Long): Flow<Int>`를 더한다 `shared/core/database/src/commonMain/kotlin/org/sjhstudio/diary/core/database/EntryDao.kt`(스키마 그대로)
- [x] T004 [P] `StoreReview` 인터페이스. 광고 `AdPlayer`처럼 판단 저장소 옆에 둠 `shared/core/data/src/commonMain/kotlin/org/sjhstudio/diary/core/data/PromptRepository.kt`

## 2단계: US1 의견 카드 (P1)

- [x] T005 [US1] 시험 먼저: 3일·3편, 닫음, 설문 연 적 있음, 2일째·2편, 기록 실패 `shared/core/data/src/commonTest/kotlin/org/sjhstudio/diary/core/data/PromptRepositoryTest.kt`
- [x] T006 [US1] `PromptRepository.start()`, `feedbackCard`, `feedbackShown()`, `feedbackClosed()`, `surveyOpened()` `shared/core/data/src/commonMain/kotlin/org/sjhstudio/diary/core/data/PromptRepository.kt`, Koin 등록
- [x] T007 [US1] 시작 때 첫 사용일 적기. 화면 모델 대신 시작 단계 `afterReady`에 둠 `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/di/AppModules.kt`
- [x] T008 [US1] 카드 화면 `FeedbackCardContent`(`DiaryStatusBox`), 세 언어 문자열 `shared/feature/home/src/commonMain/kotlin/org/sjhstudio/diary/feature/home/FeedbackCard.kt`, `composeResources/values*/strings.xml`
- [x] T009 [P] [US1] 프리뷰(한국어 먼저, 라이트·다크) `shared/feature/home/src/commonMain/kotlin/org/sjhstudio/diary/feature/home/preview/FeedbackCardPreview.kt`
- [x] T010 [US1] 홈 `listTop`에서 백업 카드가 없을 때만 의견 카드, 목록 보기 팁 미루기 `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/scaffold/AppScaffold.kt`
- [x] T011 [US1] 편지의 설문 열기도 `surveyOpened()` `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/settings/SettingsEntries.kt`
- [x] T012 [P] [US1] 카드 화면 시험(누르기·닫기) `shared/feature/home/src/iosTest/kotlin/org/sjhstudio/diary/feature/home/FeedbackCardUiTest.kt`

## 3단계: US2 리뷰 창 (P1)

- [x] T013 [US2] 시험 먼저: 7일·5편, 한 번만, 카드 보인 날 제외, 요청일을 먼저 적음 `PromptRepositoryTest.kt`
- [x] T014 [US2] 시험 먼저: 광고 차례면 리뷰 없음, 첫 광고 안내면 없음, 광고 없는 사용자는 리뷰 `shared/app/src/commonTest/kotlin/org/sjhstudio/diary/app/scaffold/AppShellViewModelTest.kt`
- [x] T015 [US2] `AdRepository.showAfterSave()`(전에는 `backToList()`)가 전면 광고 차례였는지 돌려줌. 기존 시험 맞춤 `AdRepository.kt`, `AdRepositoryTest.kt`
- [x] T016 [US2] `PromptRepository.askReviewIfDue()` `PromptRepository.kt`
- [x] T017 [US2] 저장 뒤 차례에 리뷰 잇기 `AppShellViewModel.kt`
- [x] T018 [P] [US2] Android `PlayStoreReview`(`CurrentActivity`), 의존성 `gradle/libs.versions.toml`, `shared/app/build.gradle.kts`, 라이선스 목록 다시 만들기
- [x] T019 [P] [US2] iOS `ReviewBridge.swift`(`AppStore.requestReview(in:)`)와 이를 감싼 `IosStoreReview` `iosApp/iosApp/ReviewBridge.swift`, `shared/app/src/iosMain/kotlin/org/sjhstudio/diary/app/review/IosStoreReview.kt`
- [x] T020 [US2] 플랫폼 모듈 등록 `PlatformModule.android.kt`, iOS 플랫폼 모듈

## 4단계: 화면과 문서

- [x] T029 프로토타입 확정: 백업 카드와 겹칠 때 안 A(사용자 결정 2026-11-15). 개발 뒤에 확인받은 것이라 diary-flow에 확정 멈춤을 넣음(#9)

- [x] T021 Figma 프로토타입 "프로토타입 · 의견과 리뷰"(표지 · 흐름) `prototype.md`
- [x] T022 Figma "화면" 2 일기 묶음 홈 상태에 의견 카드(라이트·다크·태블릿), `/figma-audit`
- [x] T023 디자인 점검 `design-review.md`
- [x] T024 문서: `docs/business.md` 광고 자리에 리뷰 창 한 줄, `docs/DECISIONS.md`, `docs/design.md` 홈 카드 한 장 규칙, `docs/release.md` 시험 방법
- [ ] T025 (미룸: 성공 기준은 설문 응답 수와 콘솔 평점으로 잼. 이벤트가 필요해지면 더함) 통계 이벤트(카드 보임·누름·닫음, 리뷰 요청) `shared/core/platform/.../AnalyticsEvents.kt`

## 5단계: 확인

- [x] T026 CI 묶음(`spotlessCheck designTokenCheck coroutineSafetyCheck stringResourceCheck unusedImportCheck detekt :composeApp:lint allTests assembleDebug`)
- [x] T027 (태블릿 에뮬레이터 한국어로 카드·백업 카드 순서·닫기 확인. 폰·다크·iOS 시뮬레이터 화면과 리뷰 창은 못 봄) 에뮬레이터 한국어·다크·태블릿에서 카드 보기, 리뷰 창 요청 로그 확인(`/emulator-check`), iOS 시뮬레이터 카드 확인
- [x] T028 문구 humanizer·맞춤법, 커밋·푸시·PR·CodeRabbit
