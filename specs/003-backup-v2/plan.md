# Implementation Plan: 백업 v2

**Branch**: `docs/renewal-planning`(문서), 개발은 `feat/foundation-migration`에서 이어 간다 | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-backup-v2/spec.md`

## Summary

일기와 사진을 사용자 본인의 구글 드라이브에 빠짐없이 올리고, 새 기기나 다시 설치한 앱에서 지금 기록을 지우지 않고 합쳐서 복원한다.
서버는 두지 않는다.

- 먼저 확인: 같은 클라우드 프로젝트의 Android 클라이언트가 만든 파일을 iOS 클라이언트가 보는지 문서에 없어 첫 작업으로 실제로 본다(R1).
  안 보이면 구글 Picker, 그래도 안 되면 묶음 파일로 옮기는 길을 쓴다.
- 연결: Android는 `AuthorizationClient`, iOS는 GoogleSignIn-iOS로 `drive.file`만 받는다. 계정은 구글 계정 고유 ID로 가른다(R2).
- 드라이브 호출: 공통 코드에서 Ktor로 REST를 직접 부르고, 큰 파일은 이어 올리기로 보낸다(R3).
- 형식: `VACUUM INTO` 스냅샷에서 기기 표를 비우고, 사진은 내용 확인값 이름으로 한 번만, 목록 파일을 마지막에 올려 완료를 표시한다. 파일마다 드라이브가
  잰 확인값을 견준다(R5~R7, `contracts/backup-format.md`).
- 공간 부족은 글 먼저, 최근 사진부터(R8). 보관은 기기마다 최근 5개 + 달마다 1개(R9). 사진은 30일 동안 아무도 안 가리킬 때만 휴지통으로(R10).
- 복원: 미리보기 숫자 → 한 트랜잭션 합치기 → 사진 이어 받기. 어느 쪽이 고쳐졌는지는 일기마다 둔 기준값(`sync_base`)으로 가른다(R11, R12).
  바꾸기는 기기 안 사본으로 7일 되돌리기(R13).
- 자동 백업: Android 작업 관리자 하루 1번, iOS 백그라운드 작업과 앱을 닫을 때·열 때(R14, R15). 연결 안 한 사람에게 권하기(R17).
- 묶음 파일: 압축 없는 zip, 4GB를 넘으면 ZIP64(R18).

## Technical Context

**Language/Version**: 스펙 001 R2와 같다(Kotlin 2.4.21, Compose Multiplatform 1.12.2, AGP 9.3.3, JDK 17).

**Primary Dependencies**: 스펙 001 R2 표에 더해 Ktor 3.6.1(`client-core`, `client-okhttp`, `client-darwin`, `client-content-negotiation`,
`serialization-kotlinx-json`), Okio 3.18.2(확인값, 파일 흘리기, zip 읽기), Android `play-services-auth` 22.0.0과 WorkManager 2.12.0, iOS GoogleSignIn-iOS
10.1.0(SPM)과 `BackgroundTasks`. 버전과 근거는 research R19. zip 쓰기는 라이브러리 없이 직접 만든다(R18).

**Storage**: 스펙 001 `diary.db` 버전 1에 `backup_media`, `sync_base`, `merge_mark`를 더하고 `media.status`에 `pending`을 더한다(사용자 확인 2026-11-01).
백업 설정·상태는 새 DataStore `backup.preferences_pb`, 진행 상태는 `backup/state.json`·`restore.json`, 되돌리기 사본은 `backup/rollback/`.
드라이브에는 `1일 1일기 백업/` 폴더(스냅샷, 목록 파일, 사진, README). 자세한 것은 [data-model.md](data-model.md), [contracts/backup-format.md](contracts/backup-format.md).

**Testing**: `docs/backup.md` "시험" 13묶음을 `shared/core/backup` 공통 시험으로 모두 돈다. 드라이브는 `FakeDrive`(메모리, 확인값, 공간 한도,
끊김·깨짐·만료·속도 제한 주입)로 흉내 내고, 실제 드라이브 시험 계정으로 밤마다 왕복·끊김·공간 부족을 돈다. 합치기 표는 경우마다 시험 하나.
스펙 001 이전 견본 자료로 "이전 → 백업 → 다른 기기 복원"을 Android 호스트와 iOS 시뮬레이터에서 돈다. Room 시험은 번들 드라이버로 양쪽.
화면은 Compose UI 시험(설정 > 백업, 복원 미리보기, 권하기 카드). 대역만 쓰고 mock 라이브러리는 쓰지 않는다(스펙 001과 같음).

**Target Platform**: Android minSdk 24, targetSdk 36. iOS 17 이상.

**Project Type**: 모바일 앱(Compose Multiplatform 공유 모듈 + Android 껍데기 + Xcode iOS 앱).

**Performance Goals**: 일기 1,000편·사진 1,000장(평균 1MB) 첫 백업 Wi-Fi 10분 안, 하루치 변경 1분 안(SC-004). 1만 편·1만 장 백업·복원이
끝까지, 진행 숫자가 30초 넘게 멈추지 않음, 메모리 부족 0건(SC-005). 사진은 흘려 보내고 받는다(파일 하나를 통째로 메모리에 올리지 않는다).

**Constraints**: 지금 기기 일기 손실 0(SC-003), "백업 완료" 뒤 복원할 수 없는 경우 0(SC-006), 일기 내용·사진·장소·기분·파일 이름이 사용 통계와
오류 보고에 0(SC-010), 서버 없음, 백업·복원 화면에 광고 없음(FR-033), 화면 문구는 문자열 리소스, 디자인 토큰만.

**Scale/Scope**: 기기당 일기 1만 편·사진 1만 장, 사진 한 장 30MB까지. 이 스펙이 만드는 화면은 설정 > 백업, 복원 목록, 미리보기, 진행, 결과,
묶음 파일 내보내기·불러오기, 홈의 백업 상태 줄과 권하기 카드(홈 화면은 스펙 002).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 이 계획에서 지키는 자리 | 판정 |
| --- | --- | --- |
| I. 기록은 잃지 않는다 | 사진까지 올리고 파일마다 드라이브 확인값을 견준 뒤에만 "완료"(R6). 목록 파일이 없는 백업은 목록에 없음. 복원은 합치기가 기본이고 한 트랜잭션, 실패하면 아무것도 안 바뀜(R12). 양쪽 다 고친 일기는 둘 다 남김(R11). 바꾸기는 사본 뒤 7일 되돌리기, 되돌릴 때 그 사이 쓴 일기도 남김(R13). 영구 삭제 흔적을 두지 않아 지우는 쪽으로 틀리지 않음. 정리한 파일은 휴지통으로, 사진은 30일 미사용 뒤에만(R10). 13묶음 시험 | 통과 |
| II. 일기는 사용자 것이다 | 드라이브는 사용자 계정, 범위는 `drive.file`만(R2). 목록 파일에 일기 내용·장소·기분·경로 없음. 잠금 확인값은 올리지 않음. 토큰·계정은 로그와 통계에 없음. 사용 통계는 고정 표의 열거형과 개수 구간(`contracts/backup-flow.md`). 회원 계정 없음 | 통과 |
| III. 지금 무료인 기능은 계속 무료 | 사진 포함 백업, 자동 백업, 복원, 묶음 파일 모두 무료. 백업·복원 화면과 진행 중에 광고 없음. 권하기는 홈 카드뿐이고 쓰기·읽기 화면을 막지 않음 | 통과 |
| IV. 돈이 드는 서버 없음 | 서버 없이 사용자 드라이브와 기기만. 드라이브 API는 지금 무료이고 한도 초과 요금은 계획만 발표됨, 우리 쓰임은 한도 안(R3). 라이브러리는 Apache-2.0(Ktor, Okio, AndroidX, GoogleSignIn-iOS) | 통과(지켜볼 것: 드라이브 요금 공지) |
| V. 심플하고 맛있게 | 상태는 한 줄, 강조는 지금 백업 버튼 하나. 복원은 다른 화면·다른 색. 디자인 토큰만 | 통과 |
| VI. 어떤 기기에서도 편하다 | Android·iOS 같은 공통 코드와 화면. 플랫폼마다 다른 것은 권한 받기와 백그라운드 실행뿐. 태블릿은 설정 두 칸 규칙(스펙 001 R25) | 통과(R1 결과에 따라 Android↔iOS 길을 다시 정할 수 있음) |
| VII. 근거를 갖고 만든다 | 레퍼런스(스펙 근거)와 플랫폼 문서(research 출처). 확인 못 한 세 가지(R1 파일 보임, 작업 관리자 안 토큰, iOS 백그라운드 토큰 갱신)는 첫 작업에서 시험 | 통과 |

Phase 1 설계 뒤 다시 봄: 데이터 모델과 계약에 원칙을 어기는 것이 없다. 스펙 001 표를 바꾸는 곳(`media.status`에 `pending`), 새 표 셋, 영구 삭제
흔적을 두지 않는 것, Android 시스템 백업 범위는 2026-11-01 사용자가 확인했다(`/diary-flow` "꼭 묻는 곳").

## Project Structure

### Documentation (this feature)

```text
specs/003-backup-v2/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── prototype.md               (3단계, Figma "표지 · 흐름" 섹션 "프로토타입 · 백업")
├── contracts/
│   ├── backup-format.md       드라이브 폴더, 스냅샷, 목록 파일, 올리는 순서, 공간 부족, 보관, 묶음 파일
│   ├── merge-rules.md         합치기 표, 휴지통, 사진 줄, 설정, 이전 버전 백업, 바꾸기, 되돌리기
│   ├── drive-gateway.md       DriveAuthorizer, DriveApi, 오류 나누기, FakeDrive
│   └── backup-flow.md         화면 키, 언제 도는지, 실패 종류, 상태 표시, 알림, 권하기, 사용 통계
├── checklists/
│   ├── requirements.md
│   └── data-safety.md         (/speckit-checklist: 이전·백업·복원)
└── tasks.md                   (/speckit-tasks)
```

### Source Code (repository root)

```text
shared/
├── core/
│   ├── model/                     BackupStatus, BackupProgress, BackupEntry, LegacyBackupEntry, RestorePreview, RestoreResult,
│   │                              BackupNudge, BackupFailure, MediaStatus.PENDING(더함)
│   ├── database/                  BackupMediaEntity, SyncBaseEntity, MergeMarkEntity와 DAO, MediaEntity.status에 pending
│   ├── datastore/                 BackupStore(backup.preferences_pb)
│   ├── drive/                     (새 모듈) DriveApi(Ktor), 질의 만들기, 이어 올리기, 오류 나누기
│   ├── backup/                    (새 모듈) commonMain: SnapshotWriter·Reader, ManifestCodec, UploadPlanner(공간·Wi-Fi·최근 순),
│   │                              BackupRunner(상태 기계), RetentionPolicy, MediaJanitor, RestorePlanner(미리보기),
│   │                              MergeApplier(한 트랜잭션), PhotoFetcher, RollbackManager, LegacyBackupImporter(core/migration 매핑 사용),
│   │                              BundleWriter·Reader(zip)
│   ├── data/                      BackupStatusRepository(홈·설정이 받는 흐름), BackupNudgeRepository, 사진 정리에 되돌리기 멈춤 조건 더함
│   ├── migration/                 레거시 note.db 읽기·매핑을 LegacyBackupImporter가 같이 쓰도록 공개
│   ├── platform/                  expect/actual: DriveAuthorizer, BackupScheduler, NetworkState(데이터 요금 없는 연결인지),
│   │                              BackupNotifier, DeviceName, FreeSpace
│   └── testing/                   FakeDrive, FakeDriveAuthorizer, FakeBackupScheduler, 큰 양 견본 자료 만들기
└── feature/
    └── backup/                    설정 > 백업, 복원 목록·미리보기·진행·결과, 묶음 파일, 권하기 카드 컴포넌트
composeApp/                        BackupWorker(작업 관리자), 진행 알림 서비스 선언, 알림 채널 "백업", 데이터 추출 규칙(기기 간 이전)
iosApp/                            GoogleSignIn 연결(Swift DriveAuthorizer), BGTaskScheduler 등록, Info.plist(GIDClientID, URL 스킴,
                                   BGTaskSchedulerPermittedIdentifiers)
```

**Structure Decision**: 드라이브 호출(`core/drive`)과 백업 규칙(`core/backup`)을 나눈다. 백업 규칙은 드라이브를 몰라도 `FakeDrive`로 모든 묶음을
시험할 수 있어야 하고, 나중의 여러 기기 동기화도 `core/drive`를 같이 쓴다. 화면은 `feature/backup` 하나에 둔다. 홈 상태 줄과 권하기 카드는
스펙 002 홈이 `core/data`의 흐름을 받아 `feature/backup`의 컴포넌트로 그린다.

## 다른 스펙과 나누는 것

| 003이 쓰거나 바꾸는 것 | 가진 스펙 | 내용 |
| --- | --- | --- |
| `media.status`에 `pending` | 001 | 복원 중·백업에 없는 사진. 001·002 data-model에 반영함(2026-11-01) |
| `updated_at` 규칙(즐겨찾기·휴지통도 바꿈) | 002 | 002 저장·즐겨찾기·휴지통 작업에 더한다 |
| 사진 정리 멈춤(되돌리기 7일, `pending` 줄 보호) | 002 | 002 사진 정리 조건에 더한다 |
| 홈 백업 상태 줄과 권하기 카드 자리 | 002 | 홈 머리 상태 줄(`docs/design.md`), 목록 맨 위 카드 |
| 레거시 `note.db` 읽기와 매핑, `LegacyBackupLink` | 001 | 그대로 쓴다 |
| 온보딩 "[자동 백업 켜기]" 장, 알림 허용 묻기 | 004 | 연결 흐름을 그대로 부른다 |
| 사용 통계 고정 표 | 001 | `backup_*`, `restore_run` 줄을 더한다 |

## Complexity Tracking

헌법 위반 없음. 새 모듈 둘(`core/drive`, `core/backup`)은 위 Structure Decision의 까닭으로 둔다.
