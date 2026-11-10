# 작업 목록: 시작

선행: 스펙 001 이전 관문, 스펙 003 연결 진입점. 시험을 먼저 쓴다.

## 공통

- [x] T001 `shared/core/model/Welcome.kt`: `WelcomeStep`, `ContextTip`
- [x] T002 `shared/core/datastore/WelcomeStore.kt`, `DataStoreFactory.welcome()`
- [x] T003 `shared/core/data/WelcomeRepository.kt`와 시험 `WelcomeRepositoryTest`: 단계 계산(이전함/새 설치/끝남), 세션당 팁 하나
- [x] T004 `shared/feature/welcome` 모듈과 문자열(ko, en, ja)
- [x] T005 Figma "시작 (W1~W5)" 섹션과 디자인 점검 `specs/004-start-whats-new/design-review.md`

## US1, US2

- [x] T006 `LetterScreen`(짧은 편지, 전체 편지)
- [x] T007 `OnboardingScreen`(장 넘기기, 건너뛰기, 자동 백업 켜기)
- [x] T008 `shared/app/gate/WelcomeGate`, `DiaryApp` 잇기, 끈 뒤 잇기 시험

## US3

- [x] T009 설정 > 정보의 "개발자의 편지", "처음 소개 다시 보기" 줄(새 소식 화면은 2026-11-06에 뺐다)

## US4

- [x] T010 `DiaryTip`과 쓰기·달력 팁

## 마무리

- [x] T011 프리뷰, 태블릿, 글자 200%, 에뮬레이터·시뮬레이터 확인, 문서 동기화

## 온보딩 다시 짜기 (2026-11-10)

- [x] T012 로띠 넷 더하기(꾸미기, 찾기·돌아보기, 휴지통, 태블릿) `scripts/lottie/make_onboarding.py`
- [x] T013 장 구성 바꾸기, 백업 버튼과 목록 장 빼기 `shared/feature/welcome/.../OnboardingScreen.kt`, `WelcomeViewModel.kt`, `WelcomeGate.kt`
- [x] T014 문구 세 언어, 프리뷰, 화면 시험, 개발자 모드 화면 견본
- [ ] T015 Figma W2·W3 고치기
