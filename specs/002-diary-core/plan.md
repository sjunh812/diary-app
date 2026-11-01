# Implementation Plan: 일기 핵심 화면

**Branch**: `002-diary-core` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-diary-core/spec.md`

## Summary

쓰기(제한된 HTML 서식, 자동 임시 저장, 사진 여러 장), 목록·상세, 달력, 검색, 기분 통계, 휴지통을 Compose Multiplatform 공유 화면으로
만든다. 화면은 창 폭(600/840dp)에 따라 하단 탭 ↔ 레일, 한 칸 ↔ 두 칸으로 바뀐다. 저장은 스펙 001이 만드는 Room 3 DB 위에 임시 저장본 테이블,
휴지통 표시, 검색·통계 질의를 더한다. 사진은 고르는 즉시 긴 변 약 2,500px JPEG로 줄여 내용 해시 이름으로 앱 저장소에 두고, 일기와 임시 저장본이
가리키지 않는 사진은 정리한다. 차트와 달력은 라이브러리 없이 직접 그린다.

## Technical Context

**Language/Version**: Kotlin 2.4.x, Compose Multiplatform 1.12, material3 1.9(CMP에서 따로 버전), JDK 17. 오발있 `gradle/libs.versions.toml`과 같은 축으로 맞추고 정확한 버전은 스펙 001이 고정한다.

**Primary Dependencies**: Koin 4(DI), Navigation 3(화면 전환, Scene으로 두 칸), Room 3 + `BundledSQLiteDriver`(저장), DataStore(목록 설정),
compose-rich-editor(서식 편집, HTML 입출력), Coil 3(이미지), FileKit(사진 고르기·카메라), kotlinx-datetime(날짜), kotlinx-coroutines,
material3-adaptive(창 크기 등급). 차트·달력·이미지 축소는 직접 만든다(research R7, R8, R5).

**Storage**: 앱 저장소의 Room DB(스펙 001 `Entry`, `Media`에 002가 `Draft`, `DraftMedia`, `Entry.deletedAt` 색인을 더함), 사진 파일 `files/media/<sha256>.jpg`, 목록 설정은 DataStore.

**Testing**: `kotlin.test` + Turbine(commonTest: 서식 정리, 임시 저장, 휴지통 정리, 검색 조건, 통계 집계, ViewModel), Room 시험은 번들 드라이버 메모리 DB로
Android 호스트와 iOS 시뮬레이터 양쪽, Compose UI 시험(쓰기 → 저장, 지우기 → 되돌리기, 창 폭 바꾸기), 에뮬레이터 수동 확인(폰, 폴더블, 태블릿).

**Target Platform**: Android(minSdk 24, D4 2026-11-01, targetSdk 36 이상), iOS 17 이상(R2 출시지만 처음부터 iOS 시뮬레이터에서 돈다).

**Project Type**: 모바일 앱(Compose Multiplatform, 공유 모듈 + 플랫폼 앱 껍데기).

**Performance Goals**: 일기 1만 편·사진 5천 장에서 홈 첫 화면과 검색 결과 1초 안(SC-003), 스크롤 끊김 없음, 임시 저장은 입력이 멈춘 뒤 1초 안에 디스크에 반영(SC-002의 5초보다 여유).

**Constraints**: 오프라인에서 모든 기능 동작(날씨·주소 제안만 빠짐), 일기 내용은 로그·통계·크래시 보고에 없음(헌법 II), 원시 hex 금지(헌법 V), 서버 없음(헌법 IV).

**Scale/Scope**: 사용자 기기당 일기 수천~1만 편, 화면 9개(홈, 상세, 사진 보기, 쓰기, 달력, 검색, 통계, 휴지통, 기간 고르기 시트)와 상태들.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 이 계획에서 지키는 자리 | 판정 |
| --- | --- | --- |
| I. 기록은 잃지 않는다 | 임시 저장본을 DB에 두고 입력 1초 뒤·백그라운드·화면 이탈 때 쓴다. 사진은 고르는 즉시 앱 저장소로 복사(캐시 아님). 지우기는 휴지통(30일), 영구 삭제만 확인. 저장은 한 트랜잭션(반쯤 저장된 일기 없음). 사진 정리는 일기·임시 저장본·휴지통 어디에서도 안 쓰는 파일만, 하루 지난 것만. 스키마 추가는 스펙 001 이전과 같은 DB 버전 계획 안에서 한다 | 통과 |
| II. 일기는 사용자 것이다 | 분석 이벤트는 화면 이름·버튼·개수 구간만(`AnalyticsEvents` 고정 표). 로그에 본문·장소·경로를 찍지 않는 lint 규칙과 시험. 공유는 사용자가 누를 때만 시스템 공유 창 | 통과 |
| III. 지금 무료인 기능은 계속 무료 | 1.7.4 화면 기능 대조표(quickstart)를 시험으로 둔다. 사진 한도는 헌법 III 예외(무료 10·Plus 30, 이전분 보존). 이 스펙 화면에는 광고 없음 | 통과(예외는 헌법에 적힘) |
| IV. 돈이 드는 서버 없음 | 서버 호출 없음. 라이브러리 라이선스: Koin·Room·Coil·Navigation·material3-adaptive Apache-2.0, FileKit MIT, compose-rich-editor Apache-2.0(research R2에서 확인) | 통과 |
| V. 심플하고 맛있게 | 색·글자·간격은 `DiaryTheme` 토큰만. 서식 색도 팔레트 키로 저장해 다크 모드에서 토큰 색으로 바꿔 그린다(R3) | 통과 |
| VI. 어떤 기기에서도 편하다 | 창 폭 600/840dp 규칙, Navigation 3 Scene으로 두 칸, 상태 보존, 키보드 단축키, 글꼴 200%·화면 읽기 시험 | 통과 |
| VII. 근거를 갖고 만든다 | 스펙 근거 표와 research.md 출처. 화면은 프로토타입 → Figma 뒤에 만든다 | 통과 |

Phase 1 설계 뒤 다시 봄: 데이터 모델과 계약에 원칙을 어기는 것이 없다. 사진 정리(R6)와 휴지통 영구 삭제(R9)는 원칙 I의 "지우는 것은 사용자가 고른 것만"과 맞는다(30일 자동 비움은 스펙 FR-035로 사용자에게 알린 규칙).

## Project Structure

### Documentation (this feature)

```text
specs/002-diary-core/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rich-text-html.md    본문 HTML 허용 목록 (백업·내보내기와 같이 씀)
│   ├── navigation.md        화면 키, 뒤로 가기, 창 폭별 칸 배치
│   └── data-access.md       저장소 인터페이스와 질의 규칙
├── prototype.md             (3단계, 프로토타입은 Figma "표지 · 흐름")
├── checklists/
│   ├── requirements.md
│   └── data-safety.md       이전·백업·복원 요구사항 품질 점검
└── tasks.md                 (/speckit-tasks)
```

### Source Code (repository root)

스펙 001이 뼈대를 만들고 002가 기능 모듈과 공유 UI를 더한다. 001 산출물은 "(001)"로 적었다.

```text
build-logic/                         (001) convention plugin
composeApp/                          (001) 안드로이드 앱 껍데기, applicationId org.sjhstudio.diary
shared/
├── app/                             (001 뼈대) 002: 화면 키, Scene 전략(두 칸), 하단 탭/레일, 키보드 단축키
├── core/
│   ├── model/                       002: Entry, Media, Draft, Mood, Weather, SearchFilter, StatsPeriod, ListPreferences
│   ├── database/                    (001 Entry·Media·이전) 002: Draft·DraftMedia 테이블, deletedAt 색인, 검색·통계 DAO
│   ├── data/                        002: EntryRepository, DraftRepository, MediaRepository, TrashPurger, StatsRepository, ListPreferencesRepository
│   ├── designsystem/                (001 토큰·테마) 002: 버튼, 시트, 스낵바, 칩, 빈 상태, 내비게이션 바/레일
│   ├── ui/                          002: MoodIcon, WeatherIcon, EntryRow, DateHeader, RichTextView, PhotoGrid, MoodColor
│   ├── platform/                    002: ImageProcessor(expect/actual), ShareSheet, Haptics, Clock
│   └── testing/                     002: FakeEntryRepository, FakeDraftRepository, FakeImageProcessor, 고정 시계
└── feature/
    ├── home/                        목록 + 상세 + 사진 보기
    ├── write/                       쓰기·고치기, 서식 도구, 임시 저장
    ├── calendar/                    달력
    ├── search/                      검색
    ├── insight/                     기분 통계
    └── trash/                       휴지통(설정에서 진입)
```

**Structure Decision**: 오발있과 같은 `shared/core/*` + `shared/feature/*` 구조를 쓴다. 기능 모듈은 ViewModel을 받아 그리는 진입 컴포저블까지만 내놓고,
어디서 어디로 가는지와 두 칸 배치는 `shared/app`이 정한다. 레거시 `app/` 모듈은 건드리지 않는다.

## 스펙 001에 기대는 것

002 개발은 001이 아래를 만든 뒤에 시작한다. 002의 3단계(프로토타입)와 4단계(Figma)는 001과 상관없이 먼저 할 수 있다.

| 001 산출물 | 002가 쓰는 곳 |
| --- | --- |
| `build-logic`, `composeApp`, `shared/app` 뼈대, Koin 시작 | 모든 기능 모듈 |
| Room DB와 `Entry`, `Media` 엔티티, 레거시 이전, Paging 3(001 research R4로 O2 닫음), 줄임 사진(`ThumbnailRepository`) | 모든 저장·질의. 002의 테이블 추가는 001 DB 버전 1에 넣는다 |
| `DiaryTheme` 토큰(색 · 글자 · 간격 · 모서리), 본문 글꼴 10종과 본문 글자 크기 5단계 | 모든 화면, 본문 글꼴 |
| DataStore 설정 저장소(목록 모양, 즐겨찾기만 보기). 레거시 `skip_note`(3줄 보기)는 잇지 않는다 | 목록 설정 |
| Firebase 붙이기와 `AnalyticsEvents` 고정 표 | 화면 이름·버튼 이벤트 |

## Complexity Tracking

헌법 위반 없음. 사진 한도는 헌법 III에 적힌 예외다.
