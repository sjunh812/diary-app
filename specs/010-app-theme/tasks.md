# 작업 목록: 앱 테마 색

**입력**: [spec.md](spec.md), [plan.md](plan.md), [prototype.md](prototype.md)

## 1단계: 디자인

- [x] T001 안 쓰는 시안을 "시안 · 테마 색 (고르지 않음/지난 판)"으로 옮김
- [x] T002 Figma "프로토타입 · 테마 색"(흐름 시작 5개)
- [x] T003 프로토타입 B2 [이 색으로 하기] 버튼을 주 버튼 변형으로(글자 흰색)
- [x] T004 Figma "화면" 6 설정 묶음: 설정 줄 "테마", "테마 (G3)" 라이트·설명·다크·태블릿, "테마 상태"(무료 칩, Plus 끝남, 직접 고르기 시트, Plus 안내 시트)
- [x] T005 Figma "화면" 3 Plus 묶음: 결제·관리·모든 혜택 시트에 핵심 넷
- [x] T006 `docs/design.md` 테마 색·테마 화면·설정 묶음·결제 화면, 헌법 V, `CLAUDE.md`, `docs/business.md`, `docs/DECISIONS.md`
- [x] T007 디자인 점검 `design-review.md`

## 2단계: 바탕

- [x] T010 `OkLch.kt`, `DiaryBrand.kt`(정한 7색 고정 값, custom 계산) + 시험(라벤더 = 지금 값, 정한 색 대비, 색상환 전체·경계 색 대비)
- [x] T011 `DiaryColors.withBrand`, `DiaryTheme(brand)`
- [x] T012 `ThemeColor`, `AppSettings`, `Plan.customTheme`, `SettingsStore`, `SettingsRepository` + 시험(저장·모르는 값 지키기)
- [x] T013 `ThemeBrand.kt`(쓸 색 정하기) + 시험(Plus 아님·깨진 값·모르는 이름)
- [x] T014 `DiaryApp`에서 brand 넘기기

## 3단계: US1·US2 테마 화면

- [x] T020 디자인 시스템 `DiaryThemeModeOption.kt`(작은 홈 그림), `DiaryThemeSwatch.kt`(견본·Plus 칩), `ThemeSettingsScreen.kt` 다시 짬 + `preview/ThemePreviews.kt`
- [x] T021 설정 첫 화면 "테마" 줄 값, 문자열 3개 언어
- [x] T022 `SettingsViewModel`·`SettingsEntries` 연결, 통계 id

## 4단계: US3·US4 직접 고르기와 Plus 안내

- [x] T030 디자인 시스템 `DiaryColorPicker.kt`(판·띠·hex, semantics)와 직접 고르기 시트
- [x] T031 Plus 안내 시트, 결제 화면으로
- [x] T032 `iosTest` 테마 화면 시험(견본 고름, 무료 칩, 시트)

## 5단계: US5 결제 화면

- [x] T040 `PlusBenefit.THEME`, 아이콘 `Palette`, 문자열, `PlusBenefitTest`
- [x] T041 로띠 `plus()` 다시 짜고 `plus_benefits.json` 다시 만듦

## 6단계: US6·US7

- [x] T050 `ManifestSettings`·`BackupSettingsBridge` + 왕복 시험(옛 백업, 새 백업)

## 7단계: 마무리

- [x] T060 전체 검사(spotless, designToken, coroutineSafety, stringResource, unusedImport, detekt, lint, allTests, assembleDebug), iOS 빌드
- [x] T061 에뮬레이터: 한국어 먼저, 라이트·다크, 무료·Plus·Plus 끝남, 결제 화면. 큰 글자·태블릿·업그레이드는 에뮬레이터가 꺼져 못 봄(라벤더 그대로는 시험으로 확인)
- [x] T062 셀프 리뷰 `/self-review`
- [ ] T063 커밋, `git push gitlab feat/app-theme`, PR
