# 작업 목록: 광고와 Plus

## 1단계: 프로토타입과 디자인

- [x] T001 `specs/006-ads-plus/prototype.md` 화면 흐름(결제 화면, 광고 제거, 사진 한도, 구독 관리, 광고 자리, 고급 통계, 보상형)
- [x] T002 Figma "화면" 섹션 "Plus와 광고 (P1~P7)" 라이트 폰
- [x] T003 `specs/006-ads-plus/design-review.md` TDS, SEED, HIG, M3와 결제 화면 레퍼런스에 대어 보기

## 2단계: 바탕

- [x] T010 [US3] `core/model` `Product`, `PurchaseResult`
- [x] T011 [US3] `core/data` `PurchaseRepository`, 무료 구현, 개발 빌드 `FakePurchases`
- [x] T012 [US1] `core/platform` `AdService`, `AdSwitch`, 빈 구현
- [x] T013 [US1] `core/datastore` `AdStore`, 시험(하루 한 번, 날짜 바뀜, 시각 되돌림)

## 3단계: 결제 화면 (US2, US3, US4)

- [x] T020 `shared/feature/plus` 모듈, 문자열 세 언어, 프리뷰
- [x] T021 `PaywallScreen`, 혜택 로띠, 넓은 화면 두 칸
- [x] T022 `AdFreeSheet`, `PlusManageScreen`
- [x] T023 사진 11장째 `PhotoLimitSheet`을 쓰기 화면에 잇기
- [x] T024 설정 맨 위 Plus 줄, 앱 틀 경로, 화면 키
- [x] T025 구독 끝난 뒤 한도 시험(이전된 사진, 30장 넣은 일기)

## 4단계: 고급 통계 (US5)

- [x] T030 `StatsRepository` 요일별, 시간대별, 날씨별, 장소와 시험
- [x] T031 돌아보기 칸과 무료 미리보기, 프리뷰

## 5단계: 광고 (US1, US6)

- [x] T040 `androidMain` `AdMobService`, 시험 광고 ID, 매니페스트
- [x] T041 `NativeAdSlot`, 홈 목록과 돌아보기 자리
- [x] T042 저장 뒤 목록 복귀 전면, 잠금 뒤에만
- [x] T043 보상형과 24시간 시트
- [x] T044 금지 화면 시험(SC-001), 유료 첫 프레임 시험(SC-002)

## 6단계: 실제 연결 (사용자 준비 뒤)

- [ ] T050 `androidMain` `RevenueCatPurchases`, 키 없으면 무료 구현
- [ ] T051 Play 상품과 시험 계정으로 구매, 복원, 해지
- [ ] T052 AdMob 실제 ID, Firebase Remote Config 스위치(스펙 005)
- [ ] T053 결제 화면에 이용약관·개인정보처리방침 링크(스펙 005 주소가 나오면). 출시 필수다
- [x] T054 다크, 태블릿 Figma 프레임과 결제 화면 넓은 화면 두 칸 2026-11-05: T021에서 체크했지만 코드는 한 칸이어서 두 칸으로 만들었다. 태블릿에서 레일이 결제 화면의 닫기 버튼을 가리던 버그도 고쳤다

## 마무리

- [ ] T060 영어·일본어 문구 사용자 검토(한국어 맞춤법 검사기는 2026-11-05 통과, 걸린 것은 "스토어" 순화 권고뿐)
