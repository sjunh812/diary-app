# 구현 계획: 광고와 Plus

**스펙**: `specs/006-ads-plus/spec.md`

## 기술 맥락

| 항목 | 선택 | 까닭 |
| --- | --- | --- |
| 결제 | RevenueCat KMP 4.0.0 (`purchases-kmp-core`) | 서버 없이 구매 상태를 다룬다. 월 추적 매출 2,500달러까지 무료(`docs/business.md`). 사용자 결정 2026-10-31 |
| 광고 | Google Mobile Ads 25.5.0 (`play-services-ads`) | 네이티브, 전면, 보상형을 한 SDK로. 무료 |
| 원격 스위치 | `AdSwitch` 인터페이스. 지금은 켜짐 고정, Firebase Remote Config는 스펙 005에서 붙인다 | Firebase가 아직 프로젝트에 없다 |
| 혜택 그림 | Compottie, `scripts/lottie` | 온보딩과 같다 |

R1은 안드로이드만 낸다. RevenueCat과 AdMob은 `androidMain`에만 넣고 공통 코드는 인터페이스만 본다. iOS는 무료 구현으로 빌드가 그대로 돌고, R2에 같은 인터페이스로 붙인다.

## 구조

- `shared/core/model`: `Plan`(있음), `Product`(`AD_FREE`, `PLUS_YEARLY`, 스토어 가격 문자열), `PurchaseResult`.
- `shared/core/data`: `PlanRepository`(있음), `PurchaseRepository`(상품, 구매, 복원). 화면은 이 둘만 본다.
- `shared/core/platform`: `AdService`(전면, 보상형), `AdSwitch`. `androidMain`에 `AdMobService`, `RevenueCatPurchases`.
- `shared/core/datastore`: `AdStore`(`ads.preferences_pb`). 오늘 전면을 본 날짜, 보상 끝나는 시각, 마지막으로 확인한 요금제.
- `shared/feature/plus`(새 모듈): `PaywallScreen`, `AdFreeSheet`, `PlusManageScreen`, `PhotoLimitSheet`, `RewardSheet`.
- `shared/core/ui`: `NativeAdSlot`(expect/actual). 무료일 때만 그린다.
- `shared/feature/insight`: 고급 통계 칸, 무료 미리보기. 계산은 `core/data` `StatsRepository`에 더한다.
- `composeApp/src/debug`: `DevPlanRepository`(있음), `FakePurchases`. 구매 버튼이 요금제만 바꾼다.

키는 저장소에 두지 않는다. RevenueCat 키와 AdMob ID는 `local.properties`에서 빌드 값으로 넣고, 없으면 무료 구현과 시험 광고 ID를 쓴다. 개발 빌드는 늘 시험 광고만 받는다.

## 헌법 점검

| 원칙 | 지키는 자리 |
| --- | --- |
| I 기록 | 요금제는 표시와 새로 붙이는 한도만 바꾼다. 기록을 가리거나 지우는 길이 없다(US4) |
| II 사용자 것 | 광고 요청에 일기 내용을 넣지 않는다. 계정을 만들지 않고 RevenueCat 익명 ID를 쓴다. 처리방침과 데이터 보안에 AdMob을 적는다(스펙 005) |
| III 무료·광고 | 금지 화면은 `NativeAdSlot`을 두지 않고, 전면은 저장 뒤 목록 복귀 한 곳에서만 부른다. 기존 통계는 그대로 |
| IV 서버 없음 | 서버 없음. RevenueCat은 무료 구간, 넘으면 1%(사용자 결정) |
| V 심플 | 결제 화면도 토큰과 부품만 쓴다 |
| VI 어떤 기기 | 결제 화면 넓은 화면 한 칸 |
| VII 근거 | `docs/business.md` 근거 |

## 저장 형식

DB와 백업 형식은 바뀌지 않는다. 고급 통계는 있는 열로 계산한다. 새 DataStore 파일 하나를 더하고 백업에 넣지 않는다.

## 사용자가 준비할 것

| 무엇 | 언제 필요 |
| --- | --- |
| 사업자등록번호, 통신판매업 신고번호 | Play에 상품 만들기 전 |
| RevenueCat 프로젝트와 Play 서비스 계정 연결 | 실제 결제 시험 |
| Play 인앱 상품 2개 | 실제 결제 시험 |
| AdMob 계정과 앱, 광고 단위 | 출시 전. 그 전에는 시험 ID |

이 넷이 없어도 개발자 모드로 화면과 흐름은 끝까지 만든다.
