# 구현 계획: 목록 미리보기 줄바꿈과 일기 전체 보기

**브랜치**: `feat/list-full-preview` | **작성**: 2026-11-17 | **명세**: [spec.md](spec.md)

## 요약

목록 미리보기 글을 만드는 곳(`core/data`의 `preview()`)이 줄바꿈을 지키게 고치고, 목록 설정에 미리보기 길이(3줄·전체)와 서식도 보이기(Plus)를 더한다.
줄 수와 서식은 저장소가 정해 목록 줄(`EntryRow`)에 담고, 화면은 받은 대로 그린다. 설정이 바뀌면 저장소가 페이지를 새로 만든다.
옛 1.7.4 설정은 이전 모듈이 한 번 읽는다. 맥락 팁은 "한 번에 하나, 하루에 하나" 규칙을 팁 저장소 한 곳에서 지킨다.
일기 DB 스키마는 그대로다. 백업 설정 칸에 있어도 그만인 값 둘을 더한다(형식 버전 그대로, 사용자 결정 2026-11-17).

## 기술 배경

- 축은 스펙 001과 같다(Kotlin, CMP, Koin, DataStore, Room 3, Paging). 새 라이브러리는 없다.
- 서식 해석은 에디터(compose-rich-editor)를 쓰지 않고 상세 화면이 쓰는 공통 해석기 `parseRichText`(`core/model`)를 쓴다.
- 시험: `commonTest`(줄 규칙, 저장소, 설정, 팁 규칙, 옛 설정 판단), `iosTest`(목록 보기 화면, 홈 줄 화면 시험), 벤치마크(`benchmarks/`).

## 헌법 점검

| 원칙 | 이 계획에서 지키는 자리 |
| --- | --- |
| I 기록은 잃지 않는다 | DB 스키마와 일기 글을 바꾸지 않는다. 옛 `pref.xml`은 읽기만 한다. 백업에는 있어도 그만인 칸만 더해 옛 앱이 무시한다 |
| II 일기는 사용자 것 | 미리보기 글과 서식은 화면을 그리는 데만 쓴다. 팁 기록은 횟수와 날짜뿐이다 |
| III 무료 기능 | 1.7.4의 전체 보기는 무료로 돌려준다. 서식 보기만 Plus(2.0 새 기능, 사용자 결정 2026-11-17). 광고 자리는 그대로다 |
| IV 돈 드는 서버 없음 | 서버 없음 |
| V 심플 | 설정 첫 화면 줄을 늘리지 않는다. 새 부품 없이 기존 설정 줄·시트·팁을 쓴다. 미리보기 상자는 글꼴 화면과 같은 방식이다 |
| VI 어떤 기기에서도 | 줄 수 상한은 줄 수로 정해 폰·태블릿·폴드가 같다. Android·iOS 같음(옛 설정 잇기만 Android) |
| VII 근거 | `research.md`에 에뮬레이터 확인 캡처와 공식 문서 |

## 구조

```text
shared/core/model/.../ListPreferences.kt       PreviewLength, ListPreferences에 previewLength·showFormatting·previewChosen
shared/core/model/.../EntryViews.kt            EntryRow에 previewLines·previewSpans
shared/core/model/.../Welcome.kt               ContextTip.HOME_FULL_PREVIEW, HOME_FULL_PREVIEW_KEPT
shared/core/model/.../richtext/PreviewText.kt  줄 규칙(contracts/preview-text.md), 특수 줄바꿈 글자 정규화
shared/core/datastore/.../SettingsStore.kt     새 키 셋
shared/core/datastore/.../WelcomeStore.kt      마지막 팁 날짜, 잘린 일기 연 횟수
shared/core/database/.../EntryDao.kt           전체 보기용 목록 쿼리(글 길이 상한), 서식용 쿼리(body_html 앞부분)
shared/core/data/.../diary/EntryRows.kt        preview()를 줄 규칙으로, 서식 해석을 쪽마다 IO에서
shared/core/data/.../diary/DefaultEntryRepository.kt  설정·요금제를 보고 PagingSource 고름
shared/core/data/.../WelcomeRepository.kt      팁 규칙(한 번에 하나, 하루 하나), 잘린 일기 연 횟수
shared/core/migration/.../LegacyListPreview.kt 옛 skip_note를 한 번 읽어 판단(이미 이전한 기기 포함)
shared/core/backup/.../ManifestCodec.kt        ManifestSettings에 previewLength, showListFormatting(있어도 그만)
shared/core/ui/.../DiaryEntryLine.kt           rememberPreview: 서식 구간을 토큰 색으로(카드도 같이)
shared/core/ui/.../DiaryEntryCard.kt           홈 카드·격자 칸을 옮겨 목록 보기 미리보기와 함께 씀
shared/feature/home/.../HomeStyles.kt          maxLines를 줄의 PreviewText에서
shared/feature/home/.../HomeScreen.kt          팁 둘(옛 설정 이음, 전체 보기 알림)
shared/feature/home/.../EntryDetailViewModel.kt 잘린 일기를 열면 횟수 올림
shared/feature/settings/.../ListStyleSettingsScreen.kt  미리보기 상자, 길이, 서식 줄
shared/feature/settings/.../SettingsScreen.kt  "목록 보기" 값 "피드 · 전체"
composeApp/src/debug/.../ListPerformanceSample.kt  서식·긴 일기 섞은 견본, 세 경우 바꾸기
benchmarks/.../ScrollBenchmark.kt              세 경우 재기
```

## 데이터 모델

자세한 것은 [data-model.md](data-model.md).

- `ListPreferences`: `listStyle`(있음), `previewLength: PreviewLength = THREE_LINES`, `showFormatting: Boolean = false`, `previewChosen: Boolean = false`.
- 설정 저장 키: `preview_length`, `show_list_formatting`, `preview_chosen`, `legacy_preview_checked`.
- 목록 쿼리는 그대로 두고, 전체 보기·서식 보기면 쪽(60편)마다 `previewTexts`·`previewHtmls`로 한 번 더 읽는다(사진을 쪽마다 한 번에 읽는 방식과 같음).
- 팁 기록(`WelcomeStore`): `last_tip_day`(팁이 마지막으로 보인 날), `clipped_opens`(3줄에서 잘린 일기를 상세로 연 횟수).
- `EntryRow`: `preview`(줄 규칙을 거친 글, 타입 그대로), `previewLines`(3 또는 상한), `previewSpans`(서식 보기일 때만).
- 백업 `ManifestSettings`: `previewLength: String?`, `showListFormatting: Boolean?`. 없으면 복원 기기 값을 그대로 둔다.

## 흐름

1. 홈은 `EntryRepository.timeline()`을 본다. 저장소는 `ListPreferences`와 요금제(`PlanRepository`)를 묶어 `PreviewMode`(3줄 / 전체 / 전체+서식 / 3줄+서식)를 만들고, 바뀌면 그 모드의 `PagingSource`로 새 페이지를 만든다.
2. 쪽(60편)을 읽을 때 IO 스레드에서 줄 규칙을 적용하고, 서식 모드면 HTML 앞부분을 `parseRichText`로 해석해 구간을 담는다.
3. 화면은 `DiaryPreviewText`가 `remember(preview, colors)`로 서식에 토큰 색을 입혀 그린다.
4. 옛 설정: 앱 시작 때 `legacy_preview_checked`가 없으면 이전 모듈이 `pref.xml`을 읽어 판단하고, `skip_note=false`면 길이를 전체로 바꾸고 `HOME_FULL_PREVIEW_KEPT` 팁을 연다. 어느 경우든 checked를 적는다.
5. 팁: `ContextTips.isOpen(tip)`이 "같은 날 다른 팁이 이미 보였으면 거짓"까지 판단한다. 보이기 시작한 순간 `last_tip_day`를 적는다.

## 측정 계획(SC-003, SC-004)

`research.md` R4와 [quickstart.md](quickstart.md). 상한 줄 수는 측정 뒤 `docs/design.md`에 적는다.

## 열린 결정

- 전체 보기 상한 줄 수(측정으로 정함, `research.md` R4).
- 실기기 Android 17에서 같은 글이 일찍 줄바뀐 원인(`research.md` R5). 특수 줄바꿈 글자면 줄 규칙에서 고친다.

## 다시 본 헌법 점검(설계 뒤)

위 표 그대로 통과한다. 백업 칸을 더하는 것은 형식 버전을 올리지 않는 덧붙임이고, 사용자 확인을 받았다(2026-11-17). 옛 앱은 `ignoreUnknownKeys`로 무시하고, 새 앱이 옛 백업을 읽으면 null이라 기기 값을 그대로 둔다.
