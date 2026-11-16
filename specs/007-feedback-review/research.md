# 조사: 의견 카드와 앱 안 리뷰 요청

조사일 2026-11-15. 공식 문서에서 확인한 것만 적고, 확인하지 못한 것은 맨 아래에 따로 둔다.

## R1 라이브러리

| 플랫폼 | 고른 것 | 까닭 | 출처 |
| --- | --- | --- | --- |
| Android | `com.google.android.play:review-ktx:2.0.2`(2024-10, 최신) | Play 공식 앱 안 리뷰. 요금 없음, Play Core SDK 약관만 | <https://developer.android.com/guide/playcore/in-app-review/kotlin-java>, <https://developer.android.com/reference/com/google/android/play/core/release-notes-in_app_reviews> |
| iOS | StoreKit `AppStore.requestReview(in: UIWindowScene)`(iOS 16+)를 Swift 다리로 | `SKStoreReviewController.requestReview(in:)`는 iOS 18에서 지원 중단. 새 API는 Swift 전용이라 광고처럼 Swift 다리를 둔다 | <https://developer.apple.com/documentation/storekit/appstore/requestreview(in:)-1q8qs>, <https://developer.apple.com/documentation/storekit/skstorereviewcontroller/requestreview(in:)> |

## R2 스토어 규칙과 이 스펙의 판단

| 규칙 | 출처 | 이 스펙 |
| --- | --- | --- |
| 리뷰 창 앞이나 도중에 "앱이 마음에 드나요?", "별 5개를 주시겠어요?" 같은 질문 금지 | Play 앱 안 리뷰 지침 <https://developer.android.com/guide/playcore/in-app-review> | 의견 카드와 리뷰 창을 한 흐름으로 묶지 않음(FR-008) |
| 버튼으로 API를 부르지 말 것. 버튼이 필요하면 스토어로 보냄 | Play 지침, Apple `RequestReviewAction` | 설정 "리뷰 남기기"는 지금처럼 스토어 링크 |
| 앱은 사용자가 리뷰했는지, 창이 떴는지조차 모름. 결과와 상관없이 흐름을 이음 | Play kotlin-java 예제 주석 | 결과로 광고를 바꾸는 안은 불가(Clarifications) |
| 카드 위·둘레에 아무것도 덮지 않음, 맨 위 층 | Play 지침 | 전면 광고 차례에는 요청하지 않음(FR-007) |
| 쿼터 비공개, 짧은 기간 여러 번 부르면 안 뜰 수 있음 | Play 지침 | 한 번만 요청 |
| 365일에 최대 3번, 표시 여부는 App Store가 정함. 개발 빌드는 늘 뜨고 TestFlight는 안 뜸 | <https://developer.apple.com/documentation/storekit/requestreviewaction> | iOS 확인은 개발 빌드로 |
| 몰입한 뒤, 중요한 일을 마친 자연스러운 쉼에서. 첫 실행·온보딩·일하는 중 금지 | HIG <https://developer.apple.com/design/human-interface-guidelines/ratings-and-reviews> | 저장해 작성 화면이 닫힌 순간(전면 광고 차례가 아닐 때) |
| 앱을 켜자마자 묻지 말 것. 예제는 같은 버전에서 한 번, 과정 4번 완료 뒤(4는 임의 숫자) | <https://developer.apple.com/documentation/storekit/requesting-app-store-reviews> | 5편·7일은 우리가 정한 기준 |
| 직접 만든 리뷰 창 금지, 리뷰 조건부 기능·보상·걸러 받기 금지 | App Store 심사 지침 5.6.1, 3.2.2(x), 3장 머리말 <https://developer.apple.com/app-store/review/guidelines/> | 보상·조건 없음 |
| 평점·리뷰에 보상 금지, 속이는 팝업 금지 | Play 정책 <https://support.google.com/googleplay/android-developer/answer/9898684> | 같음 |

며칠·몇 번 뒤에 물으라는 숫자는 Google도 Apple도 주지 않는다. 3편·3일, 5편·7일은 사용자 결정(2026-11-15)이다.

## R3 의견 카드

| 근거 | 출처 | 이 스펙 |
| --- | --- | --- |
| 닫을 수 있는 안내(Dismissible Callout)는 한 번만 알려도 되는 정보에만 쓰고, 닫은 뒤 같은 메시지를 다시 띄우지 않음 | 당근 SEED Callout <https://seed-design.io/components/callout> | 닫으면 다시 안 뜸(FR-003) |
| 페이지 맨 위 배너는 한 화면에 하나 | SEED Page Banner <https://seed-design.io/components/page-banner> | 홈 맨 위 카드는 한 장(FR-014) |
| Callout 규격: 안쪽 14, 요소 사이 12, 모서리 10, 닫기 버튼 40 | SEED 위 주소, Spacing·Radius 토큰 | 새 값을 만들지 않고 백업 카드와 같은 `DiaryStatusBox` 규격(디자인 점검에서 대어 봄) |
| 누르는 영역 최소 48dp / 44pt | M3 <https://m3.material.io/components/icon-buttons/accessibility>, HIG <https://developer.apple.com/design/human-interface-guidelines/buttons> | 버튼 높이 점검 |

## 확인하지 못한 것

- 토스·당근·배민·Daylio·Bear·Things가 앱 안에서 의견을 받는 실제 화면(공식 자료 없음, 앱을 받아 보지 않음).
- 토스 TDS 공개 문서에는 배너·안내 카드 규격이 없다.
- 리뷰 창과 광고를 겹치지 말라는 문장을 직접 적은 공식 문서는 없다. 겹치지 않게 하는 것은 우리 판단이다.
- Play 정책 페이지는 요약으로 읽었다.
- Day One은 사용자 포럼 증언(2025-09)으로 "마음에 드세요?" 뒤 의견 페이지로 보낸 적이 있다고 하나 공식 자료가 아니고, Play 규칙과 부딪혀 따르지 않는다.
