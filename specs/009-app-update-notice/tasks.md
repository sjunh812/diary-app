# 작업 목록: 새 버전 안내

**입력**: [spec.md](spec.md), [plan.md](plan.md), [prototype.md](prototype.md)

## 1단계: 디자인

- [x] T001 Figma "프로토타입 · 시안"의 고르지 않은 U1-B를 "보관 · 지난 시안 (쓰지 않음)"으로 옮김
- [x] T002 Figma "화면" 1 시작 묶음에 "새 버전 카드 (S11)" 줄(라이트·설명·다크·태블릿)과 "새 버전 카드 상태" 섹션(받는 중, 다시 시작하기, 받기 실패, 꼭 필요)
- [x] T003 Figma "화면" 6 설정 묶음 "설정 상태"에 앱 버전 줄 세 상태(업데이트, 받는 중, 다시 시작하기)와 설정 위 Play 확인 창
- [x] T004 Figma "화면" 7 백업 묶음에 복원 NEEDS_UPDATE [스토어에서 업데이트]
- [x] T005 `docs/design.md` 홈 맨 위 카드 차례, 새 버전 카드, 앱 버전 줄 규칙
- [x] T006 디자인 점검 `specs/009-app-update-notice/design-review.md`(TDS·SEED·HIG·M3, 프로토타입과 견주기)

## 2단계: 바탕(US1·US2 공통)

- [x] T010 `shared/core/model/.../AppUpdate.kt`: StoreCheck, UpdateNotice, ReleaseNote, AppVersion 비교 + `commonTest` 버전 비교 시험
- [x] T011 `shared/core/datastore/.../PromptStore.kt`: updateDismissed, updateKnown, updateCheckedAt
- [x] T012 `shared/core/data/.../AppUpdateRepository.kt`: StoreUpdates·UpdateConfig 인터페이스, 상태 판단, refresh·start·restart·dismiss·openStorePage
- [x] T013 `shared/core/data/src/commonTest/.../AppUpdateRepositoryTest.kt`: 닫은 버전, 더 새 버전, 꼭 필요, 받는 중·받음은 닫아도 보임, 실패 문구, 확인 실패는 None·마지막 값, iOS 24시간 간격, 소식 버전이 낮으면 안 씀
- [x] T014 `shared/core/testing/.../FakeStoreUpdates.kt`, `FakeUpdateConfig`

## 3단계: US1 홈 카드 (P1)

- [x] T020 `shared/feature/home/.../UpdateCard.kt` + `preview/UpdateCardPreview.kt`(한국어 먼저, 라이트·다크, 상태마다)
- [x] T021 문자열 `shared/feature/home/src/commonMain/composeResources/values{,-ko,-ja}/strings.xml`
- [x] T022 `shared/app/.../scaffold/FeedbackCardViewModel.kt` HomeTop.Update + `HomeTopStateTest` 차례 시험
- [x] T023 `shared/app/.../scaffold/UpdateCardViewModel.kt`, `AppScaffold.kt` listTop 연결, Koin 모듈
- [x] T024 `AppRootViewModel.resumed()`에서 refresh
- [x] T025 Android `PlayStoreUpdates.kt`, `FirebaseRemote.kt`(광고 스위치 합침), 의존성 `app-update-ktx`, 플랫폼 모듈 바인딩
- [x] T026 iOS `IosStoreUpdates.kt`(조회·해석 + 시험), `AppUpdateBridge.swift`, `FirebaseReporter.remoteString`, `IosStartup` 연결, `xcodegen`
- [x] T027 `shared/feature/home/src/iosTest/.../UpdateCardUiTest.kt`: 상태별 버튼, [나중에]·[업데이트]·[다시 시작하기] 눌림

## 4단계: US2 설정 줄 (P2)

- [x] T030 `DiarySettingsRow` 값 뒤 브랜드색 글자(valueAction) + 디자인 시스템 프리뷰
- [x] T031 `SettingsScreen.kt` 앱 버전 줄, `SettingsEntries.kt` 연결, 문자열

## 5단계: US3 복원 (P3)

- [x] T040 `RestoreProgressScreens.kt` NEEDS_UPDATE 버튼 [스토어에서 업데이트] → openStorePage, 문자열

## 6단계: 개발 빌드 흉내와 확인

- [x] T050 `composeApp/src/debug/.../DevStoreUpdates.kt` + 개발자 모드 "새 버전 흉내"(없음·있음·꼭 필요·받기 실패)
- [x] T051 iOS `-debugUpdate available|required` 인자
- [x] T052 CI 검사 전체, 에뮬레이터·시뮬레이터 한국어 캡처(홈 카드 상태, 설정 줄, 다크, 태블릿, 큰 글자)

## 7단계: 문서와 마무리

- [x] T060 `docs/release.md`·`/release-prep`에 Remote Config 단계, `docs/DECISIONS.md` 결정 기록
- [x] T061 문구 korean-humanizer·맞춤법 검사, 셀프 리뷰, 커밋·푸시·PR
