---

description: "스펙 003 백업 v2 작업 목록"
---

# Tasks: 백업 v2

**Input**: `specs/003-backup-v2/`의 spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md, checklists/data-safety.md

**Prerequisites**: Phase 1의 프로토타입·Figma 작업은 지금 할 수 있다. Phase 2(첫 확인)는 구글 클라우드 콘솔에서 시험 클라이언트를 만들어야 해서
사용자와 같이 한다. 코드는 스펙 001 뼈대(빌드, `diary.db` 버전 1, `media/`, 이전 견본 자료)와 스펙 002의 일기 저장 뒤에 같은 브랜치에서 이어 간다.

**Tests**: 스펙이 시험을 요구한다(FR-034, SC-001~SC-011, 헌법 I·II). `docs/backup.md` "시험" 13묶음과 `contracts/merge-rules.md`의 표를 먼저 시험으로
쓰고 실패하는 것을 본 뒤 구현한다. 백업·복원 시험은 기능 시험보다 먼저, 더 많이 한다(사용자 요청 2026-11-01).

**Organization**: 사용자 이야기별로 묶었다. US1~US3이 P1이고 셋이 모두 있어야 R1을 낼 수 있다. US7(바꾸기)은 P3이지만 R1에 넣는다(사용자 결정 2026-11-01).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 끝나지 않은 작업에 기대지 않아 나란히 할 수 있다
- **[Story]**: US1~US7(spec.md)

## Path Conventions

- 스펙 001 tasks.md와 같다: `{kt}` = `src/commonMain/kotlin/org/sjhstudio/diary`, `{test}` = `src/commonTest/kotlin/org/sjhstudio/diary`,
  `{android}` = `src/androidMain/kotlin/org/sjhstudio/diary`, `{ios}` = `src/iosMain/kotlin/org/sjhstudio/diary`,
  `{host}` = `src/androidHostTest/kotlin/org/sjhstudio/diary`, `{res}` = `src/commonMain/composeResources`
- 새 모듈: `shared/core/drive`, `shared/core/backup`, `shared/feature/backup`(plan.md "Source Code")
- Figma는 파일 "1일 1일기"(`HQOqmp9kJv5EEpQH2mXMj9`)의 페이지와 섹션 이름을 경로로 쓴다. 화면 ID는 B(백업)로 시작한다

---

## Phase 1: 프로토타입과 디자인 (`/diary-flow` 3·4단계, 지금 가능)

- [x] T001 이야기별로 지나가는 화면과 뒤로 가기, 상태(연결 안 함, 연결 중·취소, 백업 중, 완료, 사진 대기, 공간 부족, 다시 연결 필요, 7일 실패, 복원 받는 중·확인 실패, 새 앱 필요, 사진 받는 중, 되돌리기 기간)를 `specs/003-backup-v2/prototype.md`에 쓴다(`contracts/backup-flow.md`, `contracts/merge-rules.md` 기준). 레퍼런스(구글 포토 상태 줄, Day One 상태 줄, WhatsApp·카카오 백업 화면)의 배치를 출처와 함께 붙인다
- [x] T002 [P] "표지 · 흐름"에 섹션 "프로토타입 · 백업"을 만들고 설정 > 백업(B1: 연결 안 함·연결됨·백업 중·사진 대기·실패), 백업 설정(B13: 계정, 사진은 Wi-Fi에서만, 파일로 백업하기, 연결 끊기), 드라이브 연결(B2: 계정 고르기, 옛 백업 계정 먼저, 취소), 지금 백업 진행과 완료, 공간 부족 안내(B10) 프레임을 만들고 잇는다
- [x] T003 [P] 같은 섹션에 복원 목록(B3: 기기별, 깨진 백업, 새 앱 필요, 이전 버전 백업 칸, 묶음 파일 불러오기), 미리보기(B4: 합치기 기본, 바꾸기 다른 선택지, 설정 바뀜 줄), 복원 중(B5), 결과(B6: 찾을 수 없는 사진, 원래 기기에 있는 사진, 잠금 다시 정하기), 이전 버전 백업 불러오기(B7) 프레임을 만들고 잇는다
- [x] T004 [P] 같은 섹션에 바꾸기 확인과 되돌리기(B9: "내일까지 되돌릴 수 있어요" 포함), 묶음 파일 내보내기·불러오기(B8), 홈 머리 상태 줄 상태들과 권하기 카드(B11), 7일 실패 알림과 진행 알림 모양 프레임을 만든다
- [x] T005 흐름 시작점("백업 · 처음 연결", "· 새 폰에서 되살리기", "· 이전 버전 백업", "· 공간 부족", "· 바꾸기와 되돌리기")을 두고 캡처를 보내 사용자 확정을 받는다. 확정하면 `prototype.md`에 날짜와 바뀐 점을 쓰고 스펙에 되먹인다(지금 앱 백업 화면과 크게 달라지는 곳은 먼저 묻는다)
- [x] T006 "화면" 페이지에 12줄을 라이트 폰(왼쪽 여백 번호)·설명 패널·다크 폰·태블릿과 상태 섹션으로 그렸다(2026-11-01, y 17460부터): 백업 (B1), 백업 설정 (B13), 드라이브 연결 (B2), 백업에서 되살리기 (B3), 되살리기 전에 확인 (B4), 되살리는 중 (B5), 되살린 결과 (B6), 예전 앱 백업 (B7), 파일로 백업하기 (B8), 되돌리기 (B9), 드라이브 공간이 모자라요 (B10), 홈 백업 상태와 권하기 (B11, 7일 실패 알림 B12는 상태에). 홈 태블릿은 스펙 002 홈 디자인 때 같이 그린다. 문구는 토스 UX 라이팅 기준으로 다듬었고 맞춤법 검사기는 개발 때 문자열 리소스로 옮기며 돌린다
- [x] T007 "표지 · 흐름" 페이지의 진행 현황 표에 003 화면을 더했다(프로토타입 "확정 2026-11-01(27장)", 디자인 "12줄", 2026-11-01)
- [x] T098 개발 전 디자인 점검: "화면" 페이지 003 12줄과 상태 섹션을 토스 TDS·당근 SEED·Apple HIG·Material 3와 유명 앱에 대어 보고(흐름과 UX 패턴, 여백·간격, 글자 크기·굵기·줄 높이, 모서리, 터치 영역, 색 대비, 폰·태블릿·다크가 같은 규칙인지, 문구) `specs/003-backup-v2/design-review.md`에 남긴다. 고칠 것은 `docs/design.md` → Figma 변수·컴포넌트 → 화면 순서로 고친 뒤 백업 화면 작업에 들어간다(2026-11-01 사용자 요청, 헌법 V) 2026-11-01 전체 점검(아이콘 크기·선 굵기, 상자와 설정 줄 여백)은 `specs/001-foundation-migration/design-review.md` "전체 점검"에서 프로토타입까지 마쳤다. 나머지 항목(흐름, 글자, 색 대비)과 태블릿 틀은 2026-11-05에 마쳤다

---

## Phase 2: 첫 확인 (사용자와 같이, 다른 개발보다 먼저)

- [x] T008 사용자와 구글 클라우드 콘솔에서 지금 앱과 같은 프로젝트에 디버그 서명용 Android 클라이언트와 iOS 클라이언트를 만든다. 클라이언트 ID는 `local.properties`와 Xcode 설정 파일(저장소 밖)에 둔다. 동의 화면 범위에 `drive.file`, `openid`, `email`이 있는지 본다
- [x] T009 `scripts/drive-probe/`에 시험용 최소 앱 둘(Android 액티비티 하나, iOS SwiftUI 화면 하나)을 만들고 `quickstart.md` 1의 네 방향(Android 생성 → iOS 찾기, iOS 생성 → Android 찾기, 1.7.4 `note.db` → 새 Android·iOS 찾기)을 시험 계정으로 돌린다. 결과를 research R1과 `docs/backup.md` 인증 절에 적는다
- [x] T010 같은 시험 앱으로 WorkManager 작업 안에서 `AuthorizationClient.authorize()`가 화면 없이 토큰을 주는지, iOS `BGProcessingTask` 안에서 `refreshTokensIfNeeded`가 되는지 본다. 결과로 research O4를 닫는다
- [x] T011 (해당 없음, 2026-11-01 iOS에서 보임) T009에서 하나라도 안 보이면 research R1 "안 보일 때" 1(Picker 폴더 고르기 → 안의 파일 권한)을 시험하고, 결과와 바꿀 스펙(US3, SC-001, Assumptions)을 사용자에게 알린 뒤 정한다. 정한 것은 `docs/DECISIONS.md`에 남긴다

---

## Phase 3: Setup

- [x] T012 `gradle/libs.versions.toml`에 research R19 표(Ktor 3.6.1, Okio 3.18.2, `play-services-auth` 22.0.0, `work-runtime` 2.12.0)를 더한다
- [x] T013 [P] `shared/core/drive/build.gradle.kts`, `shared/core/backup/build.gradle.kts`(`diary.kmp.library`), `shared/feature/backup/build.gradle.kts`(`diary.kmp.feature`)와 빈 소스 폴더를 만들고 `settings.gradle.kts`에 넣는다
- [x] T014 [P] `iosApp`에 GoogleSignIn-iOS 10.1.0을 SPM으로 더하고 `iosApp/iosApp/Info.plist`에 `GIDClientID`(빌드 설정 값), 거꾸로 쓴 클라이언트 ID URL 스킴, `BGTaskSchedulerPermittedIdentifiers`(`org.sjhstudio.diary.backup.daily`, `org.sjhstudio.diary.backup.continued.*`), `UIBackgroundModes`의 `processing`을 둔다. `iOSApp.swift`의 `onOpenURL`에서 `GIDSignIn.sharedInstance.handle`
- [x] T015 [P] `composeApp/src/main/AndroidManifest.xml`에 `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `RUN_USER_INITIATED_JOBS`, 작업 관리자 `SystemForegroundService`의 `foregroundServiceType="dataSync"`를 더한다(research R14). `BackupTransferJobService` 선언은 그 클래스를 만드는 T043에서 한다

---

## Phase 4: Foundational (모든 이야기가 기대는 것)

- [x] T016 [P] `shared/core/model/{kt}/core/model/`에 `BackupStatus`, `BackupProgress`, `BackupEntry`, `LegacyBackupEntry`, `RestorePreview`, `RestoreResult`, `BackupNudge`, `BackupFailure`(`AUTH`·`NO_SPACE`·`OFFLINE`·`DRIVE_ERROR`·`LOCAL_IO`·`UNKNOWN`)를 만들고 `MediaStatus`에 `PENDING`을 더한다(data-model.md 모델 표)
- [x] T017 `shared/core/database/{kt}/core/database/`에 `BackupMediaEntity`(기본 키 `(account_key, sha256)`), `SyncBaseEntity`(`entry_id` 외래 키, 영구 삭제 때 함께 지움), `MergeMarkEntity`(`kind`: `kept_both`·`legacy_same_time`, `source`: `drive`·`file`·`legacy_drive`)와 DAO를 만들고 `MediaEntity.status`에 `pending`을 받게 한다. 버전 1 스키마 JSON(`shared/core/database/schemas`)을 다시 만든다
- [x] T018 [P] `shared/core/datastore/{kt}/core/datastore/BackupStore.kt`: `backup.preferences_pb`의 키를 data-model.md 표 그대로(`device_id`는 처음 읽을 때 UUID로 만든다)
- [x] T019 [P] `shared/core/backup/{kt}/core/backup/Hashing.kt`: Okio `HashingSink.sha256`·`md5`로 파일을 흘려 가며 재는 함수. 스펙 002 사진 저장이 같은 함수를 쓰도록 `core/data`에서 연결한다(research R4)
- [x] T020 [P] `shared/core/drive/{kt}/core/drive/`: `DriveApi` 인터페이스와 모델(`DriveFile`, `DriveQuery`, `DrivePage`, `UploadSession`, `ChunkResult`, `StorageQuota`), 오류 나누기 `DriveErrorMapper`(`contracts/drive-gateway.md` 표)
- [x] T021 [P] `shared/core/testing/{kt}/core/testing/FakeDrive.kt`: 메모리 파일, SHA-256·MD5 계산, `appProperties` 질의, 쪽 넘김, 공간 한도, 끊김·세션 만료·내용 깨기·401·속도 제한·5xx·폴더 지우기·계정 바꾸기·다른 클라이언트 파일 숨기기 주입(`contracts/drive-gateway.md` FakeDrive)
- [x] T022 [P] `shared/core/drive/{test}/core/drive/DriveErrorMapperTest.kt`, `ResumableUploadTest.kt`(Ktor `MockEngine`: 8MiB 조각, 308 `Range`, 404 → `Expired`, 다섯 번 다시 하기)
- [x] T023 `shared/core/drive/{kt}/core/drive/KtorDriveApi.kt`: Ktor 3.6.1로 REST v3를 부른다. multipart 작은 파일, 이어 올리기, `ChannelWriterContent` + Okio 소스로 흘려 보내기, 받을 때 흘려 쓰기, 응답 `fields` 고정. 엔진은 `{android}` OkHttp, `{ios}` Darwin
- [x] T024 [P] `shared/core/platform/{kt}/core/platform/DriveAuthorizer.kt`(인터페이스, `contracts/drive-gateway.md`), `shared/core/testing/{kt}/core/testing/FakeDriveAuthorizer.kt`
- [x] T025 `shared/core/platform/{android}/core/platform/AndroidDriveAuthorizer.kt`: `AuthorizationClient`(`drive.file`, `openid`, `email`), userinfo로 `sub`·이메일, `setAccount` 힌트, PendingIntent는 `connect`에서만 띄우고 `accessToken`에서는 `NeedsUser`, `revokeAccess`·`clearToken`
- [x] T026 [P] `iosApp/iosApp/Backup/GoogleDriveAuthorizer.swift`: GoogleSignIn-iOS로 `DriveAuthorizer`를 구현해 `MainViewController`에 넘긴다(`restorePreviousSignIn`, `addScopes`, `refreshTokensIfNeeded`, `disconnect`)
- [x] T027 [P] `shared/core/backup/{kt}/core/backup/ManifestCodec.kt`와 `{test}/ManifestCodecTest.kt`: `contracts/backup-format.md` 목록 파일 JSON(kotlinx-serialization, 모르는 키 무시, 없는 키 기본값), `formatVersion`·`dbVersion` 범위 판단
- [x] T028 `shared/core/backup/{kt}/core/backup/SnapshotWriter.kt`: `VACUUM INTO 'backup/outgoing/<이름>.db'` → 사본에서 기기 표(`backup_media`, `sync_base`, `merge_mark`, `draft`, `draft_media`) 비우기 → `VACUUM` → 일기·사진 줄 수, SHA-256, MD5. `SnapshotReader.kt`: 받은 스냅샷을 읽기 전용으로 열고 버전별로 일기·사진 줄을 읽는다(FR-035)
- [x] T029 [P] `shared/core/backup/{test}/core/backup/SnapshotWriterTest.kt`: 기기 표가 비었는지, 휴지통 일기·`missing`·`pending` 줄이 그대로인지, 쓰는 중에 다른 코루틴이 일기를 써도 사본이 한 시점인지
- [x] T030 [P] `shared/core/platform/{kt}/core/platform/`에 `NetworkState`(연결 여부, 데이터 요금 없는 연결인지), `DeviceName`, `FreeSpace`, `BackupNotifier`, `BackupScheduler`의 expect/actual 뼈대와 `core/testing`의 대역
- [x] T031 [P] 스펙 001 `contracts/app-shell.md` 사용 통계 고정 표에 `contracts/backup-flow.md`의 `backup_*`, `restore_run` 줄을 더하고 `shared/core/platform/{kt}/core/platform/AnalyticsEvents.kt`에 반영한다. `BackupError(kind, step)`로 바꾸는 함수와 시험(메시지에 경로·파일 이름이 없음)
- [x] T032 `shared/core/data/{kt}/core/data/BackupStatusRepository.kt`: `BackupStore`, `backup_media`, `media`, 진행 상태를 묶어 `BackupStatus` 흐름을 낸다(홈·설정이 같이 받음)

---

## Phase 5: User Story 1 - 사진까지 백업되고, 확인된 백업만 "완료"라고 한다 (Priority: P1) 🎯 MVP

**Goal**: 드라이브를 연결하고 지금 백업을 하면 일기와 사진이 모두 올라가고, 드라이브 확인값을 맞춘 뒤에만 완료라고 한다.

**Independent Test**: `FakeDrive`로 일기 수백 편·사진 수백 장을 백업해 드라이브 파일, 목록 파일, 앱 숫자, 사진 확인값을 견준다. 실제 드라이브 시험 계정으로 한 번 더.

### Tests for User Story 1 ⚠️

- [x] T033 [P] [US1] `shared/core/backup/{test}/core/backup/BackupRunnerTest.kt`: 처음 백업(폴더·README·`appProperties`), 두 번째 백업은 새 사진만, 같은 사진은 드라이브에 하나, 목록 파일이 마지막, 확인값이 다르면 다시 올림, 완료 뒤에만 `last_success_at`·`sync_base`
- [x] T034 [P] [US1] `{test}/core/backup/BackupInterruptionTest.kt`: 사진 올리는 중 끊김·앱 종료(상태 파일에서 잇기), 세션 만료, 목록 파일 전 끊김(목록에 안 보임, 이전 백업 그대로), 이미 올린 사진 재전송 0(SC-002)
- [x] T035 [P] [US1] `{test}/core/backup/BackupAccountTest.kt`: 토큰 만료 → 한 번 새로 받기 → `AUTH`, 다른 계정이면 `backup_media`가 섞이지 않음, 드라이브에서 폴더를 지우면 새로 만들고 처음부터, 폴더 이름을 바꿔도 `appProperties`로 이어짐
- [x] T036 [P] [US1] `{test}/core/backup/RetentionPolicyTest.kt`, `MediaJanitorTest.kt`: 기기마다 최근 5개 + 달마다 1개, 다른 기기 백업 안 건드림, 목록 파일 없는 스냅샷 7일 뒤 정리, `diary.unrefSince` 30일, 다시 가리키면 지움, 모두 휴지통으로
- [x] T037 [P] [US1] `{test}/core/backup/BackupConcurrencyTest.kt`: 백업 중 일기 쓰기·고치기·지우기 → 다음 백업에 들어가고 이번 스냅샷은 한 시점. 이전·복원 중에는 백업이 시작되지 않음 2026-11-05: 시험 5건. 백업과 복원이 잠금 하나(`BackupGate`)를 나누게 했고, 기록 이전이 끝나기 전에는 백업이 돌지 않는다. 복원은 도는 백업을 기다린다
- [x] T038 [P] [US1] `{test}/core/backup/LargeBackupTest.kt`: 일기 1만 편·사진 1만 장(가짜 바이트, 한 장 30MB 몇 개)이 메모리 상한 안에서 끝까지, 진행 숫자가 바이트 단위로 나아감(SC-005) 2026-11-05: `androidHostTest`에 뒀다. 내용을 쌓지 않는 대역 드라이브로 돌았고 힙 증가 64MB 상한 안에서 끝난다

### Implementation for User Story 1

- [x] T039 [US1] `shared/core/backup/{kt}/core/backup/DriveLayout.kt`: 루트·`snapshots`·`media` 폴더 찾기와 만들기(`appProperties`, 같은 종류 여럿이면 가장 먼저 만든 것), README 올리기(문자열 리소스 `backup_readme_body`), 스냅샷 이름 규칙
- [x] T040 [US1] `{kt}/core/backup/UploadPlanner.kt`: 드라이브 `media/` 목록으로 `backup_media` 맞추기 → 올릴 사진 정하기(공간·Wi-Fi 규칙은 US4·US5에서 채울 자리) → 목록 파일의 `media` 칸 만들기
- [x] T041 [US1] `{kt}/core/backup/BackupRunner.kt`: 상태 기계(data-model.md 백업), `backup/state.json` 쓰기·잇기, 사진 → 스냅샷 → 목록 파일, 파일마다 `sha256Checksum`(없으면 MD5) 확인, 동시에 2장(research O5), 다시 하기 규칙, 끝나면 `sync_base`·`BackupStore` 고치기
- [x] T042 [US1] `{kt}/core/backup/RetentionPolicy.kt`, `MediaJanitor.kt`: `contracts/backup-format.md` "보관과 정리"
- [x] T043 [US1] `shared/core/platform/{android}/core/platform/AndroidBackupScheduler.kt` 지금 백업: Android 14 이상 `BackupTransferJobService`(사용자가 시작한 데이터 전송, 진행 알림), 13 이하 급한 작업 + `setForeground`(`dataSync`), `onTimeout`·작업 멈춤에서 상태 남기기(research R14) 2026-11-05: 작업 관리자 하나로 모든 버전을 처리했다(`BackupNowWorker`, 급한 작업 + `dataSync` 진행 알림). research R14의 Android 14 전용 경로는 만들지 않아 `RUN_USER_INITIATED_JOBS`를 뺐다. 실제 드라이브로 느린 네트워크에서 지금 백업을 누르고 앱을 나가도 포그라운드 서비스로 끝까지 도는 것을 API 36 에뮬레이터에서 봤다. 연결 직후 자동 백업이 먼저 돌면 그 작업이 끝날 때까지 지키도록 고쳤다. 사진이 많은 큰 업로드와 실기기는 남았다
- [ ] T044 [P] [US1] `shared/core/platform/{ios}/core/platform/IosBackupScheduler.kt`와 `iosApp/iosApp/Backup/ContinuedBackupTask.swift`: 앞에서는 공통 코드로, iOS 26 이상은 `BGContinuedProcessingTask`로 넘겨 진행 보고, 17~25는 `beginBackgroundTask` 안에서 조각(research R15)
- [x] T045 [US1] `shared/feature/backup/{kt}/feature/backup/BackupSettingsScreen.kt`·`BackupSettingsViewModel.kt`: 연결, 상태(마지막 성공, 사진 n/N), 지금 백업과 진행, 자동 백업, "백업에서 되살리기"와 "백업 설정" 두 줄(복원은 지금 백업 버튼과 떨어뜨림, FR-027). `BackupOptionsScreen.kt`: 계정, 사진은 Wi-Fi에서만, 파일로 백업하기, 연결 끊기(드라이브 백업은 지우지 않음 안내). Figma "화면" 백업 (B1), 백업 설정 (B13), 드라이브 연결 (B2)
- [x] T046 [US1] 홈 머리 백업 상태 줄 컴포넌트 `shared/feature/backup/{kt}/feature/backup/BackupStatusLine.kt`를 내보내고 스펙 002 홈에 꽂는다(`contracts/backup-flow.md` 상태 표시). Figma 홈 백업 상태와 권하기 (B11)
- [x] T047 [P] [US1] `shared/feature/backup/{res}/values/strings.xml`: 백업 화면·상태 줄·README·진행 알림 문구(해요체, README 둘째 단락은 영어). 한국어 스킬로 다듬는다
- [ ] T048 [US1] 실제 드라이브 시험 `shared/core/backup/src/androidDeviceTest/.../RealDriveBackupTest.kt`(시험 계정, CI 비밀값이 있을 때만): 왕복 준비용 백업, 끝나면 시험 폴더 지우기

**Checkpoint**: 지금 백업이 끝까지 되고 드라이브 확인값으로 완료를 말한다.

---

## Phase 6: User Story 2 - 새 폰이나 다시 설치한 앱에서 합쳐서 되살린다 (Priority: P1)

**Goal**: 복원 목록에서 고르면 숫자로 미리 보여 주고, 지금 기록을 지우지 않고 한 번에 합친 뒤 사진을 이어 받는다.

**Independent Test**: 백업한 뒤 일기가 몇 편 있는 다른 기기(Android, iOS 시뮬레이터)에서 복원해 양쪽 일기와 사진 확인값을 견준다.

### Tests for User Story 2 ⚠️

- [x] T049 [P] [US2] `shared/core/backup/{test}/core/backup/MergeRulesTest.kt`: `contracts/merge-rules.md` 드라이브 합치기 표 경우 1~7을 하나씩, 둘 다 남길 때 새 `id`와 사진 줄 새 `id`·같은 `sha256`, `sync_base` 쓰기
- [x] T050 [P] [US2] `{test}/core/backup/MergeTrashAndPhotosTest.kt`: 휴지통 일기 남은 날 그대로, 하루 미만이면 7일, 영구 삭제한 일기가 다시 생김(미리보기에 셈), 사진 줄 표(있음 → `normal`, 받을 것 → `pending`, 백업에 없음 → `pending` 유지, `missing` 그대로), 바뀌지 않은 일기의 `pending` 사진 채우기
- [x] T051 [P] [US2] `{test}/core/backup/RestorePreviewTest.kt`: 미리보기 숫자가 실제 적용 결과와 같음, 설정 바뀜 표시, 기기 공간 부족이면 시작 안 함(FR-025)
- [x] T052 [P] [US2] `{test}/core/backup/RestoreInterruptionTest.kt`: 받기·확인 중 끊김(기기 그대로, 처음부터), 합치기 트랜잭션 중 실패(아무것도 안 바뀜), 사진 받는 중 끊김(`pending`만 이어 받기), 깨진 사진 하나만 `missing`, 스냅샷 확인값 불일치면 쓰지 않음(FR-021)
- [x] T053 [P] [US2] `{test}/core/backup/RestoreListTest.kt`: 끝까지 된 백업만, 기기별 묶음, 깨진 목록 파일·스냅샷 없음·스냅샷 깨짐·새 앱 필요의 이유, 다른 계정 백업 안 보임
- [x] T054 [P] [US2] `{test}/core/backup/RestoreSettingsTest.kt`: 설정은 백업 값, 잠금은 꺼짐 + `lockEnabled`면 한 번 안내, 백업 설정·옛 백업 연결 값은 그대로, 알림 다시 예약
- [x] T055 [P] [US2] `{test}/core/backup/RoundTripTest.kt`: 스펙 001 이전 견본 자료로 이전 → 백업 → 빈 기기 복원, 일기 모든 칸·사진 확인값·순서·`missing`과 `legacy_path` 100% 같음(SC-001). 시간대 다른 기기에서 날짜 그대로. Android 호스트와 `iosSimulatorArm64Test` 양쪽에서 돈다

### Implementation for User Story 2

- [x] T056 [US2] `{kt}/core/backup/RestoreCatalog.kt`: 목록 파일 모아 읽기, `contracts/backup-format.md` "끝까지 된 백업인지" 확인, `BackupEntry` 만들기
- [x] T057 [US2] `{kt}/core/backup/RestorePlanner.kt`: 스냅샷 받기(`backup/incoming/`, 이어 받기) → SHA-256·일기 수 확인 → 쓰기 없이 합치기 규칙 돌려 `RestorePreview`
- [x] T058 [US2] `{kt}/core/backup/MergeApplier.kt`: 한 트랜잭션으로 `contracts/merge-rules.md` 드라이브 합치기(휴지통, 사진 줄, `merge_mark`, `sync_base`), 설정 적용
- [x] T059 [US2] `{kt}/core/backup/PhotoFetcher.kt`: `pending` 줄의 사진을 받아 `media/`에 쓰고 확인값이 맞으면 `normal`, 다르면 `missing`. `backup/restore.json`으로 잇기. 지금 백업과 같은 스케줄러(T043, T044)로 돈다
- [x] T060 [US2] `shared/feature/backup/{kt}/feature/backup/restore/`: `RestoreListScreen`, `RestorePreviewScreen`(합치기 기본, 바꾸기는 다른 선택지·다른 색), `RestoreProgressScreen`, `RestoreResultScreen`과 ViewModel. Figma 복원할 백업 고르기 (B3), 복원 미리보기 (B4), 복원 중 (B5), 복원 결과 (B6)
- [x] T061 [US2] 스펙 002 사진 칸·상세에서 `PENDING` 사진을 받는 중 표시와 "원래 기기에 있는 사진"으로, `MISSING`은 "찾을 수 없는 사진"으로 그린다. 스펙 002 사진 정리가 `pending`을 지우지 않는지 시험을 더한다
- [x] T062 [US2] 둘 다 남긴 일기 표시: 스펙 002 목록·상세에 `merge_mark` 줄이 있으면 "두 가지로 남겼어요"와 닫기·짝 보기. 닫거나 하나를 지우면 줄을 지운다
- [x] T063 [US2] 앱 시작 관문(스펙 001 `shared/app`)에 남은 복원 작업 잇기: `restore.json`이 `FetchingPhotos`면 사진 받기를 다시 건다

**Checkpoint**: 새 폰(Android, iOS 시뮬레이터)에서 드라이브 백업을 합쳐 되살린다.

---

## Phase 7: User Story 3 - 예전 앱(1.7.x)이 올린 드라이브 백업을 불러온다 (Priority: P1)

**Goal**: 드라이브를 연결하면 1.7.x가 올린 `note.db`가 "이전 버전 백업"으로 보이고, 합치기로만 불러온다.

**Independent Test**: 1.7.4로 드라이브 백업을 만든 시험 계정으로 새 앱(Android, iOS)에서 불러와 같은 일기는 한 번만, 다른 글은 둘 다 남는지 본다.

### Tests for User Story 3 ⚠️

- [x] T064 [P] [US3] `shared/core/backup/{test}/core/backup/LegacyBackupMergeTest.kt`: `contracts/merge-rules.md` 이전 버전 표 세 경우, `legacy_id`로 견주지 않음(다른 기기 `note.db`), 이 기기에서 이미 이전한 일기는 걸러짐, iOS에서는 사진이 모두 `missing`, WAL 없이 올라간 옛 백업은 들어 있는 만큼
- [x] T065 [P] [US3] `{test}/core/backup/LegacyBackupFinderTest.kt`: `legacy_backup_file_id`로 먼저, 없거나 실패하면 `name = 'note.db'` 질의, 여럿이면 최근 것을 위로

### Implementation for User Story 3

- [x] T066 [US3] `shared/core/migration`의 레거시 DB 읽기와 매핑(스펙 001 `LegacyEntryMapper`, `PhotoPaths`)을 공통에서 부를 수 있게 공개한다. iOS에서도 `note.db` 사본을 날 SQL로 읽는다(번들 드라이버)
- [x] T067 [US3] `{kt}/core/backup/LegacyBackupImporter.kt`: `note.db` 받기 → 읽기 전용으로 열기 → 후보 만들기 → 날짜·시각·글로 가르기 → 한 트랜잭션 합치기 → Android는 레거시 경로 사진을 이전과 같은 방법으로 복사
- [x] T068 [US3] 복원 목록의 "이전 버전 백업" 칸과 불러오기 화면(사진이 없다는 안내, 최근 일기가 빠졌을 수 있다는 안내). Figma 이전 버전 백업 불러오기 (B7)
- [ ] T069 [US3] 실제 1.7.4 기기 시험: `scripts/legacy/`로 1.7.4를 설치해 드라이브 백업 → 새 앱으로 업데이트 → 불러오기, 같은 일기 두 번 0(SC-009)

**Checkpoint**: 예전 백업이 Android·iOS(R1 결과에 따름)에서 겹치지 않게 합쳐진다.

---

## Phase 8: User Story 4 - 신경 쓰지 않아도 매일 백업된다 (Priority: P2)

**Goal**: 연결하면 자동 백업이 켜지고 하루 1번 바뀐 것만 올린다. 7일 넘게 실패하면 알린다. 연결 안 한 사람에게는 홈에서 권한다.

**Independent Test**: 미뤄진 작업을 돌려 보이는 시각이 실제 성공 시각인지, 8일째 알림이 오는지, 권하기 카드가 세 번 뒤 사라지는지 본다.

### Tests for User Story 4 ⚠️

- [x] T070 [P] [US4] `shared/core/backup/{test}/core/backup/AutoBackupPolicyTest.kt`: 바뀐 것이 없으면 안 함, 모바일 데이터면 사진 없이 스냅샷과 목록 파일(`inBackup=false`), Wi-Fi 일회성 작업 넘김, 앱을 열 때 24시간 넘었으면 시작, 이전·복원 중이면 안 함
- [x] T071 [P] [US4] `{test}/core/backup/FailureNotificationTest.kt`: 성공마다 "+7일" 다시 예약, 실패 기록마다 본문 바꿈, 실패 기록이 없을 때 문구, 다시 성공할 때까지 한 번만, 알림 허용이 없으면 빨간 점 상태
- [x] T072 [P] [US4] `shared/core/data/{test}/core/data/BackupNudgeRepositoryTest.kt`: `contracts/backup-flow.md` 권하기 조건 전부(이전한 사용자, 3편, 사진 일기, 14일 숨김, 세 번, 스스로 끊음, 홈에서만)

### Implementation for User Story 4

- [x] T073 [US4] `composeApp/src/main/kotlin/org/sjhstudio/diary/backup/BackupWorker.kt`, `PhotoUploadWorker.kt`와 `AndroidBackupScheduler`의 자동 부분: 하루 주기(네트워크 연결됨 + 배터리 부족 아님), `UNMETERED` 일회성 사진 작업, 10분 안쪽 조각, 앱이 뒤로 간 뒤 앱 안 코루틴으로 잇지 않기(research R14, 2026-11-01 확인)
- [x] T074 [P] [US4] `iosApp/iosApp/Backup/DailyBackupTask.swift`와 `IosBackupScheduler` 자동 부분: `BGProcessingTask`(`requiresNetworkConnectivity`) 등록·요청, 앱이 뒤로 갈 때 `beginBackgroundTask` 안에서, 앱을 열 때 24시간 넘었으면(research R15)
- [x] T075 [US4] `shared/core/platform`의 `BackupNotifier` 구현: Android 채널 "백업"·지연 7일 일회성 작업, iOS 로컬 알림 시간 예약, 진행 알림(research R16)
- [x] T076 [US4] 백업 화면의 자동 백업 켜기·끄기, 백업 설정의 사진은 Wi-Fi에서만(모바일 데이터 허용 시 안내), 연결하면 자동 백업 켜기
- [x] T077 [US4] `shared/core/data/{kt}/core/data/BackupNudgeRepository.kt`와 `shared/feature/backup/{kt}/feature/backup/BackupNudgeCard.kt`, 스펙 002 홈 목록 맨 위에 꽂기. [백업 켜기]는 연결 흐름(옛 백업 계정 먼저), [나중에]는 14일 숨김. Figma 홈 백업 상태와 권하기 (B11)
- [x] T078 [US4] 스펙 004 온보딩 "[자동 백업 켜기]"가 부를 연결 진입점(`BackupConnectEntry`)을 `feature/backup`에서 내보낸다 2026-11-05: 따로 내보낼 것이 없었다. 온보딩은 `WelcomeGate`의 `viewModel.connect`로 같은 연결 흐름을 쓴다

**Checkpoint**: 연결한 사람은 매일 백업되고, 연결 안 한 사람은 홈에서 권하기를 본다.

---

## Phase 9: User Story 5 - 드라이브 공간이 모자라도 글은 지킨다 (Priority: P2)

**Goal**: 시작 전에 모자란 양을 숫자로 알리고, 글을 먼저 올리고 최근 일기의 사진부터 들어가는 만큼 올린다.

**Independent Test**: `FakeDrive` 공간 한도와 공간을 거의 채운 시험 계정으로 백업해 글이 올라가고 못 올린 장수가 맞는지 본다.

### Tests for User Story 5 ⚠️

- [x] T079 [P] [US5] `shared/core/backup/{test}/core/backup/LowSpaceTest.kt`: 스냅샷 자리 먼저, 최근 일기 순으로 사진 고르기, `inBackup=false` 수, 스냅샷도 안 들어가면 시작 안 함, 올리는 중 `storageQuotaExceeded` 처리, 한도 없는 계정은 확인 건너뜀, 공간을 비운 뒤 남은 사진만

### Implementation for User Story 5

- [x] T080 [US5] `UploadPlanner`에 공간 계획(`about.storageQuota`)과 `BackupRunner`에 중간 공간 부족 처리를 더한다(`contracts/backup-format.md` "공간이 모자랄 때")
- [x] T081 [US5] 공간 부족 안내 화면: 모자란 양, 사진 정리 방법, 드라이브 휴지통 비우기, 구글 계정 저장 공간 화면 열기, 파일로 백업하기. 우리가 공간을 팔지 않는다(FR-019). Figma 드라이브 공간이 모자라요 (B10)

---

## Phase 10: User Story 6 - 드라이브 없이 파일로 내보내고 불러온다 (Priority: P3)

**Goal**: 일기·사진·목록 파일을 zip 하나로 내보내고, 그 파일로 합쳐서 되살린다.

**Independent Test**: 묶음 파일로 내보낸 뒤 다른 기기에서 불러와 일기와 사진이 모두 같은지 본다.

### Tests for User Story 6 ⚠️

- [x] T082 [P] [US6] `shared/core/backup/{test}/core/backup/BundleWriterTest.kt`: STORE 항목, CRC·크기, 4GB·65,535개를 넘으면 ZIP64 기록(가짜 큰 항목), Okio `openZip`으로 다시 읽기. `{host}`에서 `java.util.zip.ZipInputStream`으로도 읽기
- [x] T083 [P] [US6] `{test}/core/backup/BundleImportTest.kt`: `manifest.json` 먼저, 깨진 사진 하나만 `missing`, `pending` 사진은 `inBackup=false`, 합치기 규칙이 드라이브 복원과 같음

### Implementation for User Story 6

- [x] T084 [US6] Okio `openZip`이 Android·iOS 공통 코드에서 보이는지 확인하고(research R18), `{kt}/core/backup/BundleWriter.kt`(STORE + ZIP64), `BundleReader.kt`를 만든다. 안 보이면 Android `ZipInputStream`, iOS ZIPFoundation으로 읽기를 플랫폼에 둔다
- [x] T085 [US6] 저장 위치에 바로 흘려 쓰기(research O3): FileKit 0.17.0으로 되는지 보고, 안 되면 Android SAF 출력 스트림·iOS 앱 폴더 + 공유 시트. 결과를 research O3에 적는다
- [x] T086 [US6] `shared/feature/backup/{kt}/feature/backup/export/FileBackupScreen.kt`: 파일 기능의 입구 하나. [파일 만들기](크기 미리 보이기, 진행), "파일에서 되살리기"(파일 고르기 → 드라이브 복원과 같은 미리보기·합치기), 새 휴대폰으로 옮기는 방법 안내. 연결 전 화면·백업 설정·공간 부족 화면의 "파일로 백업하기" 줄이 모두 여기로 온다. Figma 묶음 파일 (B8) 고쳐 받기 문의로 돌려받은 zip(`legacy-snapshot/note.db`만 든 예전 형식)도 받아 이전 버전 백업과 같은 규칙으로 합친다(스펙 001 FR-010a, `scripts/recover/README.md`)

---

## Phase 11: User Story 7 - 정말 필요할 때만 바꾸기, 7일 안에 되돌리기 (Priority: P3, R1에 넣음)

**Goal**: 바꾸기는 따로 고르고, 바꾸기 전에 기기 안에 사본을 떠 7일 동안 되돌린다.

**Independent Test**: 바꾸기 → 일기 하나 쓰기 → 되돌리기 뒤 바꾸기 전 일기와 새로 쓴 일기가 모두 있는지 본다.

### Tests for User Story 7 ⚠️

- [x] T087 [P] [US7] `shared/core/backup/{test}/core/backup/ReplaceAndRollbackTest.kt`: 사본 공간이 없으면 시작 안 함, 사본 확인, 한 트랜잭션 바꾸기, 임시 저장본 그대로, 되돌리기에서 바꾸기 뒤 쓴 일기 남음, 7일 뒤 사본 지우기, 그동안 사진 정리 멈춤과 다시 켜기

### Implementation for User Story 7

- [x] T088 [US7] `{kt}/core/backup/RollbackManager.kt`와 `MergeApplier`의 바꾸기: `contracts/merge-rules.md` "바꾸기", "되돌리기". 스펙 002 사진 정리에 `rollback_until` 조건을 더한다
- [x] T089 [US7] 미리보기의 바꾸기 확인("지금 기기의 일기 n편이 이 백업으로 바뀌어요. 7일 동안 되돌릴 수 있어요"), 설정 > 백업의 되돌리기 줄과 "내일까지 되돌릴 수 있어요". Figma 되돌리기 (B9)

---

## Phase 12: Polish & Cross-Cutting

- [x] T090 [P] Android 시스템 백업 규칙(research R20): `composeApp/src/main/res/xml/data_extraction_rules.xml`(클라우드 백업과 기기 간 이전 모두 모든 영역 제외), Android 11 이하 `backup_rules.xml`(모든 영역 제외), `bmgr` 백업·복원과 기기 간 이전 뒤 일기가 따라가지 않고 빈 상태로 정상 시작하는 시험
- [ ] T091 [P] SC-010 점검: Firebase DebugView로 `backup_*`, `restore_run`을 한 바퀴 돌려 고정 표 안인지, 로그·오류 보고에 토큰·이메일·파일 이름·경로가 없는지 `grep`과 함께 `specs/003-backup-v2/test-report.md`에 남긴다
- [ ] T092 [P] 실제 드라이브 밤마다 시험을 `.github/workflows/ci.yml`에 더한다(왕복·끊김·공간 부족, 시험 계정 갱신 토큰은 저장소 비밀값)
- [ ] T093 [P] SC-004 시간 재기(1,000편·1,000장 Wi-Fi 10분, 하루치 1분)와 research O5(조각 크기, 동시 장수), O6(모바일 데이터 스냅샷 크기)을 재서 닫는다
- [x] T094 [P] 태블릿·글자 크기 200%·다크 모드에서 백업 화면들을 보고 설정 두 칸 규칙(스펙 001 R25)대로 그린다. Compose UI 시험(설정 > 백업 상태들, 복원 미리보기의 합치기·바꾸기 구분, 권하기 카드)을 `shared/feature/backup/{test}`에 둔다
- [ ] T095 `docs/release.md`의 포그라운드 서비스 신고, 브랜드 검증, 개인정보처리방침(드라이브 범위와 쓰는 까닭)을 이 스펙 결과와 맞춘다
- [ ] T096 문서 동기화: `docs/backup.md`, `docs/migration.md`, 스펙 001·002 data-model, 헌법, `docs/DECISIONS.md`가 같은 내용인지, Figma 진행 현황이 맞는지 본다. 백업·복원 화면과 진행 중이 광고를 띄우지 않는 화면 목록(스펙 006, FR-033)에 들어 있는지 본다
- [ ] T097 quickstart.md 전체를 처음부터 돌려 통과를 확인하고 `checklists/data-safety.md`를 검토자에게 넘긴다

---

## Dependencies & Execution Order

- Phase 1(프로토타입·디자인)은 지금 할 수 있다. 화면 코드(T045, T046, T060, T068, T077, T081, T086, T089)는 T005(흐름 확정)와 T006(Figma) 뒤에 한다.
- Phase 2(첫 확인)는 개발 전에 한다. T009 결과가 iOS 쪽 길(T026, T055의 iOS, T064의 iOS, T066)을 바꿀 수 있다.
- 스펙 001 뼈대와 DB, 스펙 002 일기 저장이 끝나야 Phase 3부터 시작한다. T061, T062, T046, T077은 스펙 002 화면(사진 칸, 목록, 상세, 홈)에 기댄다.
- Phase 3 → Phase 4 → 이야기들. US1이 먼저다(US2·US3·US6이 US1의 형식과 올리기에 기댄다).
  - US2는 US1의 `DriveLayout`·`ManifestCodec`·`SnapshotReader`에 기댄다. US3은 US2의 `RestoreCatalog`·미리보기 화면을 같이 쓴다.
  - US4는 US1의 `BackupRunner`에, US5는 US1의 `UploadPlanner`에 기댄다. US4와 US5는 나란히 할 수 있다.
  - US6은 US2의 `MergeApplier`, US7은 US2의 미리보기와 `MergeApplier`에 기댄다.

## Parallel Example: User Story 2

```text
T049 MergeRulesTest, T050 MergeTrashAndPhotosTest, T051 RestorePreviewTest, T052 RestoreInterruptionTest,
T053 RestoreListTest, T054 RestoreSettingsTest, T055 RoundTripTest                         (시험 먼저, 나란히)
T056 RestoreCatalog → T057 RestorePlanner → T058 MergeApplier → T059 PhotoFetcher          (차례대로)
T060 복원 화면들, T061 사진 상태 그리기, T062 둘 다 남김 표시                               (T058 뒤 나란히)
```

## Implementation Strategy

1. **먼저**: Phase 2 첫 확인. 결과에 따라 iOS 길을 사용자와 정한다.
2. **MVP**: Phase 3·4 + US1. 실제 드라이브에 사진까지 올라가고 확인값으로 완료를 말한다.
3. **R1에 꼭 필요**: US2·US3(P1), US7(R1 포함 결정). 합쳐서 되살리기와 예전 백업 불러오기가 없으면 기록 유실을 끝낼 수 없다.
4. **그다음**: US4(자동 백업·권하기), US5(공간 부족), US6(묶음 파일).
5. 각 단계 끝에 `./gradlew spotlessCheck designTokenCheck allTests`가 통과하고, 커밋은 `CLAUDE.md` 커밋 규칙대로 작업 브랜치에 바로 푸시한다.
