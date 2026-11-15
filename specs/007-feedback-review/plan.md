# 구현 계획: 의견 보내기 카드와 앱 안 리뷰 요청

**브랜치**: `feat/feedback-review-prompt` | **작성**: 2026-11-15 | **명세**: [spec.md](spec.md)

## 요약

홈 목록 맨 위에 의견 카드를 한 번 띄우고, 저장 뒤 목록으로 돌아온 순간 전면 광고 차례가 아니면 스토어 리뷰 창을 한 번 요청한다.
판단은 공통 코드의 `PromptRepository` 하나가 맡고, 기록은 기기에만 두는 새 DataStore 파일에 둔다. 일기 DB 스키마와 백업 형식은 바꾸지 않는다.

## 기술 배경

- 축은 스펙 001과 같다(Kotlin, CMP, Koin, DataStore, Room 3). 새로 고르는 것은 스토어 리뷰 창뿐이다.
- Android: Play 앱 안 리뷰 라이브러리(`com.google.android.play:review-ktx`, 무료). 버전은 `research.md` R1 표.
- iOS: StoreKit `AppStore.requestReview(in:)`(iOS 16+, Swift 전용)를 Swift 다리(`ReviewBridge.swift`)로 부른다. 옛 `SKStoreReviewController`는 iOS 18에서 지원 중단이라 쓰지 않는다. 다리는 광고(`GoogleAdsBridge`)와 같은 방식으로 Kotlin에 넘긴다.
- 시험: `commonTest`(저장소·화면 모델), `iosTest`(카드 화면 시험), Android는 `FakeReviewManager`를 쓰지 않고 공통 인터페이스를 가짜로 바꿔 시험한다.

## 헌법 점검

| 원칙 | 이 계획에서 지키는 자리 |
| --- | --- |
| I 기록은 잃지 않는다 | 저장 형식을 건드리지 않는다. 백업 권하기 카드가 의견 카드보다 먼저다(FR-014) |
| II 일기는 사용자 것 | 판단에 일기 수만 쓰고 내용은 읽지 않는다. 통계에는 카드 보임·누름·닫음, 리뷰 요청만 남긴다 |
| III 무료 기능·광고 규칙 | 전면 광고 규칙을 바꾸지 않는다. 리뷰 창은 광고 차례가 아닐 때만. 카드는 홈 목록에만 있고 광고가 아니다 |
| IV 돈 드는 서버 없음 | 서버 없음. 리뷰 라이브러리와 StoreKit은 무료 |
| V 심플 | 새 부품 없이 `DiaryStatusBox`를 쓴다. 한 번에 카드 한 장 |
| VI 어떤 기기에서도 | 폰·태블릿 두 칸 같은 규칙. Android·iOS 기능 같음 |
| VII 근거 | `research.md`에 스토어 지침과 레퍼런스 |

## 구조

```text
shared/core/datastore/.../PromptStore.kt          새 파일 prompt.preferences_pb. 기기 것, 백업 제외
shared/core/database/.../EntryDao.kt              새로 쓴 일기 수 세기 쿼리(스키마 그대로)
shared/core/data/.../PromptRepository.kt          카드·리뷰 판단과 기록, StoreReview 인터페이스
shared/core/data/.../AdRepository.kt              backToList()가 전면 광고 차례였는지 돌려줌
shared/app/src/androidMain/.../review/PlayStoreReview.kt
shared/app/src/iosMain/.../review/IosStoreReview.kt  Swift 다리를 감쌈
iosApp/iosApp/ReviewBridge.swift
shared/app/.../scaffold/AppShellViewModel.kt       저장 뒤 차례: 광고 차례가 아니면 리뷰
shared/app/.../scaffold/AppScaffold.kt            listTop: 백업 카드가 없으면 의견 카드
shared/app/.../settings/SettingsEntries.kt        편지의 설문 열기도 기록
shared/app/.../scaffold/FeedbackCardViewModel.kt  카드 상태와 홈 카드 연결
shared/feature/home/.../FeedbackCard.kt           카드 화면(+ preview/)
```

## 데이터 모델

`PromptPrefs`(기기에만, `prompt.preferences_pb`)

| 값 | 뜻 |
| --- | --- |
| `firstUseAt: Long?` | 이 기능이 든 버전을 처음 연 순간. 없으면 앱 시작 때 지금으로 적음 |
| `feedbackDone: Boolean` | 카드를 닫았거나 설문을 연 적 있음 |
| `feedbackShownDay: String?` | 카드가 마지막으로 보인 날(같은 날 리뷰 막기) |
| `reviewAskedDay: String?` | 리뷰 창을 요청한 날. 있으면 다시 요청하지 않음 |

새로 쓴 편 수는 따로 세지 않고 일기 DB에서 센다: `created_at >= firstUseAt`이고 `legacy_id`·`legacy_create_date`가 비어 있는 줄. 휴지통 줄도 센다.
- 영구 삭제하거나 휴지통에서 30일이 지나 지워진 줄은 빠진다. 새로 쓴 일기를 굳이 영구 삭제한 경우뿐이라 따로 세는 칸을 두지 않는다. 이미 보인 카드나 요청한 리뷰는 되돌리지 않는다.
- 복원한 일기는 원래 `created_at`을 지녀 대개 첫 사용일보다 앞이라 빠진다. 첫 사용일 뒤 다른 기기에서 이 앱으로 쓴 일기를 복원하면 들어갈 수 있다. 그 사람도 새 앱으로 일기를 쓴 사람이라 그대로 둔다(CodeRabbit 지적 검토, 2026-11-15).

## 흐름

1. 시작 단계(`Startup.afterReady`)에서 `PromptRepository.start()`가 첫 사용일을 적는다.
2. 홈 `listTop`: 백업 카드가 있으면 그것만, 없으면 `PromptRepository.feedbackCard`가 참일 때 의견 카드. 카드가 그려지면 그날을 `feedbackShownDay`로 적는다.
3. 저장 뒤 목록으로 돌아옴(`AppShellViewModel.takeAdTurn`): 첫 광고 안내 차례면 안내, 아니면 `ads.backToList()`. 전면 광고 차례가 아니었으면(`false`) `PromptRepository.askReviewIfDue()`.
4. `askReviewIfDue()`: 7일·5편·오늘 카드 안 보임·요청한 적 없음이면 그날을 먼저 적고 `StoreReview.request()`. 적은 뒤 부르므로 앱이 꺼져도 두 번 묻지 않는다.
5. 기록을 읽거나 쓰지 못하면 카드와 리뷰 모두 거짓(덜 묻는 쪽). 광고와 같은 `orElseOnFailure`.

## 열린 결정

없음(Clarifications 2026-11-15에서 닫음).
