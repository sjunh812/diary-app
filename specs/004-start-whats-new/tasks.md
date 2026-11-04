# 작업 목록: 시작과 새 소식

선행: 스펙 001 이전 관문, 스펙 003 연결 진입점. 시험을 먼저 쓴다.

## 공통

- [ ] T001 `shared/core/model/Welcome.kt`: `WelcomeStep`, `ContextTip`, `WhatsNewItem`
- [ ] T002 `shared/core/datastore/WelcomeStore.kt`, `DataStoreFactory.welcome()`
- [ ] T003 `shared/core/data/WelcomeRepository.kt`와 시험 `WelcomeRepositoryTest`: 단계 계산(이전함/새 설치/끝남), 세션당 팁 하나
- [ ] T004 `shared/feature/welcome` 모듈과 문자열(ko, en, ja)
- [ ] T005 Figma "시작 (W1~W5)" 섹션과 디자인 점검 `specs/004-start-whats-new/design-review.md`

## US1, US2

- [ ] T006 `LetterScreen`(짧은 편지, 전체 편지)
- [ ] T007 `OnboardingScreen`(장 넘기기, 건너뛰기, 자동 백업 켜기)
- [ ] T008 `shared/app/gate/WelcomeGate`, `DiaryApp` 잇기, 끈 뒤 잇기 시험

## US3

- [ ] T009 `WhatsNewScreen`, 설정의 "새 소식" 줄과 점, `WhatsNewKey`

## US4

- [ ] T010 `DiaryTip`과 쓰기·달력 팁

## 마무리

- [ ] T011 프리뷰, 태블릿, 글자 200%, 에뮬레이터·시뮬레이터 확인, 문서 동기화
