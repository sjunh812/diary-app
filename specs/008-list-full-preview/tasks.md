# 작업 목록: 목록 미리보기 줄바꿈과 일기 전체 보기

**입력**: `specs/008-list-full-preview/`의 spec.md, plan.md, research.md, data-model.md, contracts/preview-text.md, quickstart.md
**시험**: 저장소·규칙은 `commonTest`, 화면은 `iosTest`. 위험한 곳(옛 설정, 백업 칸, 결제 경계)은 실패를 먼저 재현하는 시험을 넣는다(`~/.claude/CLAUDE.md` "일하는 방식").

경로 줄임: `model` = `shared/core/model/src/commonMain/kotlin/org/sjhstudio/diary/core/model`, `data` = `shared/core/data/src/commonMain/kotlin/org/sjhstudio/diary/core/data`, `store` = `shared/core/datastore/src/commonMain/kotlin/org/sjhstudio/diary/core/datastore`, `db` = `shared/core/database/src/commonMain/kotlin/org/sjhstudio/diary/core/database`, `mig` = `shared/core/migration/src/commonMain/kotlin/org/sjhstudio/diary/core/migration`, `home` = `shared/feature/home/src/commonMain/kotlin/org/sjhstudio/diary/feature/home`, `settings` = `shared/feature/settings/src/commonMain/kotlin/org/sjhstudio/diary/feature/settings`.

## 1단계: 프로토타입과 디자인 (`/diary-flow` 3~5단계)

- [x] T001 화면 흐름을 `specs/008-list-full-preview/prototype.md`에 적는다: 설정 > 목록 보기(상자·모양·길이·서식), 홈 3줄/전체/서식, 옛 설정 이음 팁, 전체 보기 팁, 무료가 서식 줄을 누름 → Plus 화면, 사진 격자일 때.
- [ ] T002 (임시 HTML `prototype.html`로 대신해 확정, Figma 옮기기는 사용자가 맡음 2026-11-17) Figma "프로토타입 · 시안" 페이지 섹션 "프로토타입 · 목록 전체 보기"에 폰 390×844·태블릿 1024×720 프레임과 프로토타입 연결을 만든다. 같은 목록 위 자리에 백업 카드·팁이 겹칠 때 순서를 흐름으로 그린다.
- [x] T003 프로토타입 확정을 받고 `prototype.md`에 날짜와 바뀐 점을 적는다(확정 전 T004 이후 금지).
- [ ] T004 Figma "화면" 페이지 "6 설정" 묶음에 "목록 보기 (S?)" 줄(라이트·설명·다크·태블릿)과 상태(3줄, 전체, 사진 격자, 무료 서식 잠김, Plus 서식 켬)를 그린다.
- [ ] T005 Figma "화면" 페이지 "2 일기" 묶음 홈 줄 상태에 3줄·5줄 줄바꿈, 전체(끝까지), 서식 보기, 두 팁을 더한다.
- [x] T006 `/figma-audit`로 규격을 잰다. `specs/008-list-full-preview/design-review.md`에 TDS·SEED·HIG·M3·레퍼런스 앱과 견준 표, 프로토타입과 견준 날짜를 적는다. 고칠 값은 `docs/design.md`부터.

## 2단계: 바탕 (모든 이야기 전에)

- [x] T007 `PreviewLength`(`THREE_LINES` id `three`, `FULL` id `full`, 모르는 id는 `THREE_LINES`)와 `ListPreferences`의 `previewLength`, `showFormatting = false`, `previewChosen = false`를 더하고 주석 "미리보기는 늘 3줄" 문구를 고친다 in `model/ListPreferences.kt`
- [x] T008 `PreviewText(text, spans, maxLines)`를 만들고 `EntryRow.preview`를 `PreviewText`로 바꾼다. `toString`에 글을 담지 않는다 in `model/EntryViews.kt` (실제: `preview`는 String 그대로 두고 `previewLines`·`previewSpans`를 더함. 피드·카드·격자·휴지통·검색이 함께 써서)
- [x] T009 설정 키 `preview_length`, `show_list_formatting`, `preview_chosen`, `legacy_preview_checked`를 읽고 쓴다 in `store/SettingsStore.kt`
- [x] T010 [P] 설정 저장·읽기 왕복 시험(기본값, 모르는 id) in `shared/core/datastore/src/commonTest/kotlin/org/sjhstudio/diary/core/datastore/SettingsStoreTest.kt`

## 3단계: US1 미리보기가 줄바꿈을 지킨다 (P1)

**독립 시험**: 줄마다 쓴 일기·빈 줄 일기·체크리스트를 넣고 3줄 홈을 본다.

- [x] T011 [P] [US1] `contracts/preview-text.md`의 "시험 예" 표와 특수 줄바꿈 글자(U+2028·2029·0085·000B·000C, `\r\n`) 시험을 먼저 쓴다 in `shared/core/model/src/commonTest/kotlin/org/sjhstudio/diary/core/model/richtext/PreviewTextTest.kt`
- [x] T012 [US1] 줄 규칙(정규화, 3줄은 빈 줄 없앰, 전체는 빈 줄 하나, 이모지 안 자름)을 만든다 in `model/richtext/PreviewText.kt`
- [x] T013 [US1] `preview()`가 줄 규칙을 쓰게 바꾸고, 검색일 때는 지금 방식(찾은 글자 앞 12자, 240자)에 줄바꿈 정규화만 더한다 in `data/diary/EntryRows.kt`
- [x] T014 [US1] 피드·카드 미리보기가 `PreviewText.maxLines`를 쓰게 하고 `PREVIEW_LINES` 상수를 정리한다 in `home/HomeStyles.kt`
- [x] T015 [P] [US1] 한국어 3줄·줄바꿈 미리보기(라이트·다크)를 더한다 in `home/preview/HomePreviews.kt`
- [ ] T016 [US1] 실기기 Android 17 일찍 줄바뀐 일기의 원문을 읽어 원인을 `research.md` R5에 적는다(특수 글자면 T011에 그 글자 시험을 더한다)

## 4단계: US2 목록에서 일기 전체 보기 (P1)

**독립 시험**: 목록 보기에서 "전체"를 고르고 피드·카드를 본다.

- [x] T017 [US2] 전체 보기용 목록 쿼리(글 길이 상한까지 `substr`)를 더한다. 3줄 쿼리(`LIST_TEXT_CHARS = 520`)는 그대로 in `db/EntryDao.kt` (실제: 목록 쿼리는 그대로, 쪽마다 `previewTexts`·`previewHtmls`로 더 읽음)
- [x] T018 [US2] `ListPreferences`를 보고 모드별 `PagingSource`를 고르고, 설정이 바뀌면 새 페이지를 만든다. 보던 자리를 지킨다 in `data/diary/DefaultEntryRepository.kt`
- [x] T019 [P] [US2] 모드가 바뀌면 줄 글·`maxLines`가 바뀌는 저장소 시험 in `shared/core/data/src/commonTest/kotlin/org/sjhstudio/diary/core/data/diary/EntryPreviewModeTest.kt`
- [x] T020 [US2] 목록 보기 화면에 맨 위 미리보기 상자(견본 일기 문자열 리소스), 모양, 본문 길이(3줄·5줄·전체)를 둔다. 사진 격자면 길이를 숨긴다. 길이를 고르면 `previewChosen = true` in `settings/ListStyleSettingsScreen.kt`
- [x] T021 [US2] 설정 첫 화면 "목록 보기" 값을 "피드 · 전체"(사진 격자면 모양만)로 in `settings/SettingsScreen.kt`
- [x] T022 [P] [US2] 문자열(값 문구, 길이 이름, 미리보기 상자 견본 글)을 `values`·`values-ko`·`values-ja`에 in `shared/feature/settings/src/commonMain/composeResources/values*/strings.xml`
- [x] T023 [P] [US2] 목록 보기 화면 프리뷰(라이트·다크·사진 격자) in `settings/preview/` 와 화면 시험 in `shared/feature/settings/src/iosTest/kotlin/org/sjhstudio/diary/feature/settings/ListStyleSettingsScreenTest.kt` (프리뷰만 함 `settings/preview/ListStylePreviews.kt`. iOS 화면 시험은 남음) (iOS 화면 시험 `ListStyleSettingsUiTest` 더함)
- [x] T024 [US2] 백업 `ManifestSettings`에 `previewLength: String?`, `showListFormatting: Boolean?`을 더하고 백업·복원에 잇는다. null이면 기기 값 유지 in `shared/core/backup/src/commonMain/kotlin/org/sjhstudio/diary/core/backup/ManifestCodec.kt`, `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/backup/BackupSettingsBridge.kt`
- [x] T025 [P] [US2] 옛 백업(칸 없음) 복원, 새 백업을 옛 형식으로 읽기 시험 in `shared/core/backup/src/commonTest/kotlin/org/sjhstudio/diary/core/backup/ManifestCodecTest.kt` (옛 백업 읽기·새 백업 왕복·형식 버전 그대로를 `list_preview_settings_are_optional`이 봄)

## 5단계: US3 옛 설정 이어받기 (P2, Android)

**독립 시험**: 1.7.4에서 3줄 보기를 끄고 덮어 깐다. 2.0.1 이전 마친 기기에도.

- [x] T026 [P] [US3] 판단 시험을 먼저 쓴다: `skip_note=false`→전체+팁, true·없음·깨짐→그대로, checked면 다시 안 읽음, `previewChosen`이면 안 읽음 in `shared/core/migration/src/commonTest/kotlin/org/sjhstudio/diary/core/migration/LegacyListPreviewTest.kt`
- [x] T027 [US3] `pref.xml`을 `LegacyPrefs.parse`로 읽기만 하는 판단을 만든다 in `mig/LegacyListPreview.kt`
- [x] T028 [US3] 시작 때 이전이 끝난 뒤 한 번 부른다(이전한 기기 포함, iOS는 checked) in `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/Startup.kt`, `shared/app/src/androidMain/kotlin/org/sjhstudio/diary/app/di/PlatformModule.android.kt`
- [x] T029 [US3] 팁 `HOME_FULL_PREVIEW_KEPT`("예전처럼 목록에서 일기 전체를 보여드려요." [3줄로 보기])를 목록 위에 in `model/Welcome.kt`, `home/HomeScreen.kt`, `shared/feature/home/src/commonMain/composeResources/values*/strings.xml`
- [x] T030 [US3] `docs/migration.md` 설정 표와 시험 결과를 맞춘다(T026 결과) (문서 반영은 기획 커밋에서 함)

## 6단계: US4 잘린 일기를 자주 여는 사람에게 알리기 (P2)

**독립 시험**: 3줄에서 잘린 일기를 상세로 3번 열고 홈으로.

- [x] T031 [P] [US4] 팁 규칙 시험: 한 번에 하나, 같은 날 두 번째 팁 없음, 다음 날 됨, 백업 카드 우선, 횟수 3 in `shared/core/data/src/commonTest/kotlin/org/sjhstudio/diary/core/data/ContextTipsRuleTest.kt`
- [x] T032 [US4] `last_tip_day`, `clipped_opens`, `opened_tips`를 저장한다 in `store/WelcomeStore.kt`
- [x] T033 [US4] `ContextTips.isOpen`이 하루 하나 규칙과 순서를 지키게 한다. 보이기 시작할 때 날짜를 적는다 in `data/WelcomeRepository.kt`, `shared/core/ui/src/commonMain/kotlin/org/sjhstudio/diary/core/ui/ContextTipBar.kt`
- [x] T034 [US4] 홈 줄이 글 배치 결과로 잘렸는지 알아 상세 열기에 넘기고, 상세가 잘린 일기를 열면 횟수를 올린다 in `home/HomeStyles.kt`, `home/EntryDetailViewModel.kt`
- [x] T035 [US4] 팁 `HOME_FULL_PREVIEW`("목록에서도 일기 전체를 볼 수 있어요." [전체로 보기]→바로 전체) in `model/Welcome.kt`, `home/HomeScreen.kt`, 문자열 리소스
- [x] T036 [P] [US4] 홈 팁 화면 시험 in `shared/feature/home/src/iosTest/kotlin/org/sjhstudio/diary/feature/home/HomeTipsTest.kt` (남음: 홈 팁 iOS 화면 시험) (`HomeTipsUiTest` 더함)

## 7단계: US5 Plus는 목록에서 서식까지 (P3)

**독립 시험**: 개발자 모드 Plus로 서식 일기를 쓰고 켠다. 무료로 돌린다.

- [x] T037 [P] [US5] 서식 구간 옮기기 시험(정규화·빈 줄 제거 뒤 위치, 잘린 태그 버림) in `shared/core/model/src/commonTest/kotlin/org/sjhstudio/diary/core/model/richtext/PreviewTextTest.kt`
- [x] T038 [US5] HTML 앞부분 해석과 구간 옮기기 in `model/richtext/PreviewText.kt`
- [x] T039 [US5] 서식 쿼리(`body_html` 앞부분)와 `showFormatting && Plus`일 때만 그 모드 in `db/EntryDao.kt`, `data/diary/DefaultEntryRepository.kt`, `data/diary/EntryRows.kt`
- [x] T040 [P] [US5] 결제 경계 시험: 무료면 구간 없음, Plus 끝나면 구간 없음·값 유지 in `shared/core/data/src/commonTest/kotlin/org/sjhstudio/diary/core/data/diary/EntryPreviewModeTest.kt`
- [x] T041 [US5] `PreviewText`를 토큰 색으로 그리는 부품 in `shared/core/ui/src/commonMain/kotlin/org/sjhstudio/diary/core/ui/DiaryPreviewText.kt`, 홈 줄에서 쓰기 in `home/HomeStyles.kt` (실제: 새 부품 대신 `rememberPreview`가 서식을 토큰 색으로 입히고 피드·카드가 씀)
- [x] T042 [US5] 목록 보기 화면 "서식 그대로 보기" 줄(무료는 Plus 표시, 누르면 Plus 화면), 미리보기 상자에 서식 in `settings/ListStyleSettingsScreen.kt`, 문자열 리소스

## 8단계: 측정과 상한 (SC-003, SC-004)

- [x] T043 견본 DB에 서식 일기 20%, 5천 자 이상 긴 일기 5%를 섞고 세 모드를 바꾸게 한다 in `composeApp/src/debug/kotlin/org/sjhstudio/diary/debug/ListPerformanceSample.kt`
- [x] T044 [P] 줄 하나 해석 시간 측정 시험 in `shared/core/model/src/commonTest/kotlin/org/sjhstudio/diary/core/model/richtext/PreviewTextTimingTest.kt` (Mac JVM 쪽 하나 3.7ms)
- [x] T045 세 모드로 재도록 한다 in `benchmarks/src/main/kotlin/org/sjhstudio/diary/benchmarks/ScrollBenchmark.kt`, `StartupBenchmark.kt` (S931N 출시형 빌드로 방식마다 3회, SC-003 통과. 첫 화면 측정은 건너뜀)
- [x] T046 에뮬레이터와 실기기에서 재고 상한 줄 수를 정해 `docs/design.md` "일기 줄(피드)"·`contracts/preview-text.md`·`design-review.md`에 적는다 (상한 없음으로 정함, research R4)

## 9단계: 마무리

- [x] T047 `/emulator-check`: 한국어 먼저, 다크, 글자 최대, 태블릿 두 칸, 영어·일본어, 1.7.4 업그레이드(quickstart 3) (한국어·다크·라이트·글자 2배·영어·일본어 확인. 태블릿 남음. 1.7.4 업그레이드는 2026-11-19 Android 10 에뮬레이터에서 통과: 일기 1,004편 모두 이전, 사진 336장, 원본 파일 그대로, `skip_note=false` → `previewLength=full`)
- [ ] T048 `/backup-roundtrip`(quickstart 4)
- [x] T049 CI와 같은 검사 전부(quickstart 1), 새 문구 `korean-humanizer`·맞춤법
- [ ] T050 Baseline Profile에 새 경로가 필요한지 `/release-prep` 기준으로 본다

## 10단계: US6 Plus 화면 혜택 (P3)

**독립 시험**: 무료로 결제 화면·시트·"서식 그대로 보기" 진입, 개발자 모드 Plus로 관리 화면.

- [x] T051 [US6] Figma 프로토타입과 디자인(라이트·다크·태블릿) in Figma "프로토타입 · 시안" 섹션 "프로토타입 · Plus 혜택"
- [x] T052 [US6] 결제 화면 규칙(핵심·다른 혜택, [모두 보기], 약관 자리, 간격)을 `docs/design.md` "결제 화면"·"구독 관리"와 `docs/DECISIONS.md`에 적는다
- [x] T053 [P] [US6] 새 문구(다른 혜택, 모두 보기, Plus 혜택, 일기에 체크리스트 넣기, 목록에서도 서식 그대로 보기와 설명) 3개 언어 in `shared/feature/plus/src/commonMain/composeResources/values*/strings.xml`
- [x] T054 [US6] 혜택 목록 모델(핵심·다른 혜택, 들어온 혜택 먼저, 3줄 상한) in `shared/feature/plus/src/commonMain/kotlin/org/sjhstudio/diary/feature/plus/PlusBenefit.kt`, 시험 in `src/commonTest`
- [x] T055 [US6] 결제 화면·관리 화면을 새 묶음으로, [모두 보기] 시트, 약관 자리 in `PlusScreen.kt`
- [x] T056 [US6] 들어온 곳을 `PlusKey`에 싣고 "서식 그대로 보기"에서 넘기기 in `shared/app/src/commonMain/kotlin/org/sjhstudio/diary/app/navigation/ScreenKeys.kt`, `SettingsEntries.kt`
- [x] T057 [P] [US6] 프리뷰(한국어 먼저, 라이트·다크, 시트) in `shared/feature/plus/src/commonMain/kotlin/org/sjhstudio/diary/feature/plus/preview/PlusPreviews.kt`, iOS 화면 시험 in `src/iosTest`
- [ ] T058 [US6] 아이폰 15 시뮬레이터와 411×914 에뮬레이터에서 스크롤이 없는지 재고(SC-007) 다크·태블릿·큰 글자·영어·일본어를 본다

## 순서

- 1단계(프로토타입 확정)가 끝나야 화면 작업(T014, T020~T023, T029, T035, T041, T042)을 한다. 화면 없는 T007~T013, T017~T019, T024~T027, T031~T033, T037~T040은 먼저 해도 된다.
- US1 → US2(같은 줄 규칙 위) → US3·US4(US2의 설정 위) → US5. US3과 US4는 서로 기다리지 않는다.
- 측정(8단계)은 US2 뒤부터. 전체는 상한 없음(T046).

## 함께 할 수 있는 것

- T010, T011, T019, T025, T026, T031, T037, T044는 서로 다른 시험 파일이라 함께 쓴다.
- T022·T023(설정 문자열·프리뷰)과 T029·T035(홈 문자열)는 파일이 달라 함께 한다.

## 전략

- MVP는 US1 + US2(줄바꿈과 무료 전체 보기). 메일의 불편이 여기서 풀린다.
- 그다음 US3(메일 보낸 사람 같은 옛 사용자), US4(알리기), US5(Plus) 순서로 더한다.
