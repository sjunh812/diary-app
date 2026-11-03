# Implementation Plan: 기반과 이전

**Branch**: `docs/renewal-planning`(문서), 개발은 `feat/foundation-migration` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-foundation-migration/spec.md`

## Summary

새 Compose Multiplatform 프로젝트의 뼈대를 세우고, 1.7.4 사용자의 일기·사진·설정·잠금을 잃지 않고 옮긴다.

- 뼈대: 오발있과 같은 축(Kotlin 2.4, CMP 1.12, AGP 9.3, Koin 4, Navigation 3)에 Now in Android의 모듈 나누기와 성능 관행을 더한다
  (research R1~R3).
- 저장: Room 3 + 번들 SQLite 하나에 스펙 002·003의 표까지 담아 R1 출시 때 DB 버전 1로 낸다(R4).
- 이전: 원본은 바이트 복사만 하고, 사본을 날 SQL로 읽어 임시 DB에 쓰고, 옛 값으로 되돌린 지문을 견준 뒤 이름 바꾸기로 한 번에 바꾼다.
  사진은 `media/<sha256>.<ext>`로 한 번만 복사하고 실패해도 남긴다(R5~R9).
- 잠금: 4자리 비밀번호는 기기 보안 저장소의 HMAC 키로 만든 확인값만 둔다. 잠금은 화면 하나가 아니라 앱 전체를 덮는 관문이다(R11~R13).
- 시작: 시스템 스플래시 하나, 작은 값만 읽는 동안만 붙잡는다. 옛 시작 화면 이름과 옛 알림 수신기 이름은 그대로 남겨 업데이트 직후에도 깨지지 않게 한다(R14, R15).
- 성능: 나눠 읽기(Paging 3), 미리 만든 썸네일, 안정된 키와 불변 화면 모델로 바뀐 줄만 다시 그리기, baseline profile(R9, R10).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Compose Multiplatform 1.12.2(material3 1.9.0), AGP 9.3.3, KSP 2.3.12, JDK 17(빌드 툴체인).
오발있 `gradle/libs.versions.toml`과 같은 축이고 최신 패치로 맞춘다. 정확한 버전은 research R2 표(2026-11-01 Maven 메타데이터 확인)다.

**Primary Dependencies**: Koin BOM 4.2.2, Navigation 3(androidx navigation3-runtime 1.2.0 + JetBrains navigation3-ui 1.2.0), JetBrains lifecycle 2.11.0,
Room 3.0.3 + `sqlite-bundled` 2.7.1, Paging 3.5.3(KMP) + `room3-paging`, DataStore 1.3.0, kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0,
kotlinx-serialization 1.11.0, Coil 3.6.3, FileKit 0.17.0(원본 내보내기 저장 창), material3-adaptive 1.3.0(창 크기), Android 전용
`core-splashscreen` 1.2.0과 `androidx.biometric` 1.1.0, iOS `LocalAuthentication`·`Security`(키체인), Firebase Analytics·Crashlytics(헌법 IV 예외).

**Storage**: 앱 전용 저장소만 쓴다(캐시 아님). Room DB `diary.db`, 사진 `media/<sha256>.<ext>`, 썸네일 `thumbs/<sha256>.jpg`(백업 제외),
설정과 잠금은 DataStore 두 파일, 이전 상태·결과·원본 사본은 `migration/`과 `legacy-snapshot/`. 자세한 자리는 [data-model.md](data-model.md) "파일 자리".

**Testing**: `kotlin.test` + Turbine(commonTest: 매핑, 본문 변환 왕복, 지문, 상태 기계, 잠금 규칙, ViewModel), Room 시험은 번들 드라이버로
Android 호스트와 iOS 시뮬레이터 양쪽, 레거시 DB 견본 파일로 이전 시험(Android 호스트, `docs/migration.md` 시험 전부),
실제 1.7.4 APK → 새 APK 업그레이드 계측 시험(에뮬레이터 API 24, 29, 35, 36), Compose UI 시험(잠금, 이전 화면, 앱 틀 폭 바꾸기),
Macrobenchmark(시작 시간, 목록 넘기기). 대역(fake)만 쓰고 mock 라이브러리는 쓰지 않는다(NIA 관행).

**Target Platform**: Android minSdk 24(D4), targetSdk 36, compileSdk 37(최신 androidx가 요구, research R2). iOS 17 이상(R2 출시지만 처음부터 시뮬레이터에서 돈다).

**Project Type**: 모바일 앱(Compose Multiplatform 공유 모듈 + Android 앱 껍데기 + Xcode iOS 앱).

**Performance Goals**: 이전 1,000편·사진 1,000장 2분 안(SC-003), 이전 없는 날 시작 1.5초 안(SC-007), 1만 편·사진 5천 장 목록 넘기기 늦은 프레임
1% 미만, 사진 0.3초 안(SC-010), 10분 넘기기 메모리 부족 0건(SC-011).

**Constraints**: 원본 파일 바이트 불변(FR-002), 잠금 해제 전 일기 내용 노출 0(SC-005), 일기 내용이 통계·오류 보고·로그에 0(SC-009), 서버 없음,
화면 코드에 원시 hex·숫자 크기 없음(FR-028), 화면 문구는 문자열 리소스만(FR-030).

**Scale/Scope**: 기기당 일기 1만 편·사진 5천 장까지 시험한다. 001이 만드는 화면은 잠금(열기, 비밀번호 정하기·바꾸기, 잊었을 때), 이전(진행, 결과,
실패, 옛 일기 읽기), 설정 틀(글꼴, 글자 크기, 화면 모드, 잠금, 알림, 이전 결과, 고지), 앱 틀(하단 탭·레일, 빈 탭 자리), 디자인 견본(개발용)이다.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 이 계획에서 지키는 자리 | 판정 |
| --- | --- | --- |
| I. 기록은 잃지 않는다 | 원본은 스트림으로 복사만 하고 연결을 열지 않는다(R5). 사본에서 읽어 임시 DB에 쓰고 지문 검증 뒤 이름 바꾸기로 교체(R6, R7). 끊기면 처음부터. 사진은 앱 저장소로 복사하고 실패해도 남긴다(R8). 못 찾은 사진은 자리와 원래 경로를 남긴다. 깨진 DB는 읽히는 줄만 옮기고 "일부만"으로 알린다. 1.7.4 실제 업그레이드 시험 | 통과 |
| II. 일기는 사용자 것이다 | 잠금 확인값은 기기에 묶인 키의 HMAC(R11). 잠금 관문이 모든 화면 위(R13), 최근 앱 미리보기 가림. 원본 사본의 설정 파일에서 비밀번호 줄을 빼고 복사. 통계·오류 보고는 고정 표의 이벤트와 개수 구간만, 이전 오류는 종류 코드로 바꿔 보낸다(R21). 비밀번호 분실은 폰 화면 잠금으로, 서버·계정 없음 | 통과 |
| III. 지금 무료인 기능은 계속 무료 | 잠금, 생체 인증, 알림, 글꼴, 글자 크기, 화면 모드를 그대로 무료로 잇는다. 이 스펙 화면(잠금, 이전, 설정 틀)에 광고 없음 | 통과 |
| IV. 돈이 드는 서버 없음 | 서버 호출은 Firebase(Analytics·Crashlytics)뿐이고 헌법의 예외다. CI는 공개 저장소의 무료 GitHub Actions. 라이브러리는 Apache-2.0(AndroidX, Koin, Coil), MIT(FileKit), 글꼴은 `docs/design.md`의 확인된 10종 | 통과 |
| V. 심플하고 맛있게 | `DiaryTheme` 토큰을 `docs/design.md` 확정값 그대로 코드로 옮기고, 기능 모듈에서 원시 hex·숫자 크기·`MaterialTheme.colorScheme`을 쓰면 빌드 검사에서 막는다(R18). 스플래시 애니메이션 없음(사용자 결정) | 통과 |
| VI. 어떤 기기에서도 편하다 | 앱 틀이 폭 600dp에서 하단 탭 ↔ 레일로 바뀐다(R25). 글자 크기 200%와 다크 모드 시험. iOS도 같은 공유 화면이고 잠금·알림은 각 플랫폼 방식(R12, R15) | 통과 |
| VII. 근거를 갖고 만든다 | 구조는 NIA·KotlinConf·FeedFlow·오발있 비교(R1), 라이브러리 버전은 Maven 메타데이터, 잠금·스플래시·최근 앱은 플랫폼 문서. 확인 못 한 것은 research "열린 결정"에 남긴다 | 통과 |

Phase 1 설계 뒤 다시 봄: 데이터 모델과 계약에 원칙을 어기는 것이 없다. 원본 사본과 사진 사본을 지우는 시점은 이 스펙에서 정하지 않는다(헌법 I,
`docs/migration.md` 원칙 1). 썸네일은 원본에서 다시 만들 수 있는 파생 파일이라 백업에서 빼지만 캐시 폴더에 두지 않는다(R9).

## Project Structure

### Documentation (this feature)

```text
specs/001-foundation-migration/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── prototype.md             (3단계, Figma "표지 · 흐름" 섹션 "프로토타입 · 기반과 이전")
├── test-report.md           (개발 중 업그레이드·잠금·통계 점검 결과)
├── contracts/
│   ├── legacy-mapping.md    레거시 값 → 새 모델, 본문 변환, 검증 지문
│   ├── migration-flow.md    이전 단계와 파일, 다시 시작, 화면 상태, 보내도 되는 값
│   ├── lock.md              잠금 저장소 인터페이스, 확인값 형식, 다시 잠그기, 분실
│   └── app-shell.md         시작 순서와 관문, 화면 키, 앱 틀, 사용 통계 고정 표, 빌드 검사
├── checklists/
│   ├── requirements.md
│   └── data-safety.md       이전·백업·복원 요구사항 품질 점검
└── tasks.md                 (/speckit-tasks)
```

### Source Code (repository root)

```text
build-logic/convention/              convention plugin: diary.android.application, diary.android.test, diary.kmp.library,
                                     diary.kmp.feature, diary.room, diary.root(디자인 토큰 검사·git 훅 설치), 루트의 spotless
gradle/libs.versions.toml            버전 표(research R2)
composeApp/                          Android 앱 껍데기 org.sjhstudio.diary
│   src/main/AndroidManifest.xml     MainActivity, activity-alias .SplashActivity(옛 런처 이름), helper.AlarmReceiver(옛 알림 이름)
│   src/main/kotlin/…                DiaryApplication(Koin 시작, 이전 시작), MainActivity(installSplashScreen),
│                                    LegacyAlarmReceiver, 최근 앱 가림
│   src/main/res/                    적응형 아이콘(전경·배경·단색), Theme.Diary.Starting, 자동 백업 규칙
iosApp/                              Xcode 프로젝트: iOSApp.swift, 앱 아이콘(라이트·다크·틴트), UILaunchScreen, 가림 창, Firebase 연결
shared/
├── app/                             시작 관문(Startup → Lock → Migration), 루트 NavDisplay, 하단 탭·레일 틀, Koin 모듈 모음,
│                                    iOS MainViewController
├── core/
│   ├── model/                       Entry, Media, Mood, Weather, DiaryFont, BodyTextSize, ThemeMode, AppSettings, LockSettings,
│   │                                RelockAfter, MigrationState, MigrationReport, LegacyBackupLink
│   ├── database/                    DiaryDatabase(Room 3), EntryEntity, MediaEntity, DAO, 변환기, schemas/
│   │                                (002가 Draft 표를, 003이 백업 표를 같은 버전 1에 더함)
│   ├── datastore/                   SettingsStore, LockStore(DataStore 파일 둘), 파일 자리 expect/actual
│   ├── data/                        SettingsRepository, LockRepository, EntryRepository 뼈대(Paging), MediaStore,
│   │                                ThumbnailRepository, ReminderRepository
│   ├── migration/                   commonMain: 매핑, 본문 변환, 지문, 상태 기계, 결과. androidMain: 원본 찾기·복사,
│   │                                레거시 DB 읽기, 설정 XML 읽기. iosMain: 옮길 것 없음
│   ├── designsystem/                DiaryTheme(색·글자·간격·모서리·움직임), 본문 글꼴 10종, 컴포넌트(버튼, 상단 바, 하단 탭,
│   │                                레일, 시트, 토스트, 대화상자, 설정 줄, 칩), 디자인 견본 화면(개발 빌드만)
│   ├── ui/                          창 폭 등급, 공통 상태 화면, 미리보기 도구
│   ├── platform/                    expect/actual: PinKeyStore(HMAC 키), DeviceAuthenticator, AppForeground(단조 시계),
│   │                                ReminderScheduler, ImageDecoder(썸네일), Telemetry, RecentsGuard, AppFiles
│   └── testing/                     FakeSettingsRepository, FakeLockRepository, FakeDeviceAuthenticator, TestClock,
│                                    LegacyFixtures(레거시 DB·설정 XML·사진 견본 파일 만들기)
└── feature/
    ├── lock/                        잠금 화면, 비밀번호 정하기·바꾸기, 잊었을 때, 잠금 설정
    ├── migration/                   이전 진행, 결과, 실패(다시 시도·옛 일기 읽기·원본 내보내기·문의)
    └── settings/                    설정 틀(글꼴, 글자 크기, 화면 모드, 잠금, 알림, 이전 결과, 오픈소스 라이선스)
benchmarks/                          Macrobenchmark(시작, 목록 넘기기)와 baseline profile 만들기
.githooks/commit-msg                 커밋 형식 검사(FR-031)
.github/workflows/ci.yml             Android 검사·시험·빌드, iOS 시험·xcodebuild
scripts/legacy/                      1.7.x APK 만들기와 견본 자료 넣기(업그레이드 시험용)
```

**Structure Decision**: 오발있의 `shared/core/*` + `shared/feature/*`를 따르고 NIA처럼 `core/datastore`와 `core/testing`을 따로 둔다.
기능 모듈을 api/impl로 나누지 않는다(화면 수가 적고 혼자 만든다, R1). 이전 코드는 `core/migration` 하나에 모아 레거시 형식을 다 다룬 뒤
통째로 지울 수 있게 한다. 레거시 `app/` 모듈은 이전 코드가 끝날 때까지 참고용으로 두고 새 빌드에 넣지 않는다(`settings.gradle.kts`에서 뺀다).

## 다른 스펙과 나누는 것

| 001이 만드는 것 | 쓰는 스펙 |
| --- | --- |
| 빌드 뼈대, 앱 틀, 시작 관문, Koin, 화면 키 | 002~006 전부 |
| `DiaryTheme`와 컴포넌트, 본문 글꼴 10종, 글자 크기 5단계 | 002 화면, 004 소개, 006 결제 화면 |
| Room DB 버전 1의 `entry`, `media`. 002는 `draft`, `draft_media`, 003은 백업 표를 같은 버전에 더한다 | 002, 003 |
| `media/`, `thumbs/` 파일 규칙과 `ThumbnailRepository` | 002 목록·쓰기, 003 백업(썸네일은 올리지 않음) |
| 이전 결과의 견본 자료(이전 → 백업 → 복원 왕복 시험용, FR-034) | 003 |
| `LegacyBackupLink`(옛 드라이브 계정과 파일 ID) | 003 이전 버전 백업 불러오기 |
| 매일 알림 다시 예약과 옛 알림 수신기 | 004(알림 허락 묻기) |
| `Telemetry`와 사용 통계 고정 표 | 모든 스펙, 006 광고 |

## Complexity Tracking

헌법 위반 없음.
