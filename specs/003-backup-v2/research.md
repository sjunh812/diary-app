# Research: 백업 v2

조사일 2026-11-01. 공식 문서, 라이브러리 저장소, Maven 메타데이터에서 확인한 것만 적고, 확인하지 못한 것은 "확인 못 함"으로 둔다.
빌드 축과 버전은 스펙 001 research R2를 그대로 쓰고, 이 스펙이 더하는 것만 아래 R19 표에 적는다.

## 정한 것

### R1. Android가 만든 파일을 iOS가 보는지: 먼저 실제로 확인한다

- 사실: 공식 문서 어디에도 `drive.file` 파일이 프로젝트 단위로 보이는지, OAuth 클라이언트 단위로 보이는지 적혀 있지 않다.
  - cross-client 문서는 한 클라이언트에 준 동의가 그 범위에 대해 앱 전체를 믿는다는 뜻이라고 하지만 드라이브 파일 얘기는 없다
    ([cross-client identity](https://developers.google.com/identity/protocols/oauth2/cross-client-identity)).
  - 드라이브 문서는 "앱"을 정의하지 않는다. `drive.file`은 앱으로 열거나 앱에 공유한 파일이고, `isAppAuthorized`는 요청한 앱이 만들었거나 연 파일이다
    ([스코프](https://developers.google.com/workspace/drive/api/guides/api-specific-auth), [files](https://developers.google.com/workspace/drive/api/reference/rest/v3/files)).
- Decision: 계획의 첫 작업(T001)으로 같은 프로젝트의 Android·iOS 시험 클라이언트로 네 방향을 확인한다(`quickstart.md` 1). 이 결과가 나오기 전에는
  Android↔iOS 복원(SC-001)과 iOS의 이전 버전 백업(US3)을 만들지 않는다.
- 안 보일 때(차례대로 시험한다):
  1. 구글 Picker(데스크톱·모바일용)로 사용자가 "1일 1일기 백업" 폴더나 `note.db`를 한 번 고르게 한다. 시스템 브라우저에서 열리고 `drive.file`만
     허용되며 `allow_folder_selection`이 있다([Picker for desktop & mobile](https://developers.google.com/workspace/drive/picker/guides/desktop-mobile-picker)).
     Android는 `AuthorizationRequest`의 picker 트리거로 같은 일을 한다. iOS용 SDK는 없어 앱이 직접 브라우저 OAuth를 연다(GoogleSignIn-iOS에 이 옵션이 없음).
     폴더를 고르면 안의 파일까지 권한이 생기는지는 확인 못 함이다. 시험한다.
  2. `note.db`는 파일 하나라 Picker로 충분하다. v2 백업이 폴더 권한으로 안 되면 Android → iOS는 묶음 파일(US6)로 옮기게 안내한다.
- 이 결과는 스펙의 가정(Assumptions)과 SC-001, US3을 바꿀 수 있어 결과가 나오면 사용자에게 먼저 알린다.

### R2. 권한 받기: Android `AuthorizationClient`, iOS GoogleSignIn-iOS

- Android: `play-services-auth` 22.0.0(2026-08-25)의 `AuthorizationClient.authorize()`. 22.0.0에서 옛 Google 로그인 API가 빠졌다
  ([출시 노트](https://developers.google.com/android/guides/releases)).
  - 응답에 계정 정보가 없다. 로그인 없이 `openid`·`email` 범위를 같이 받아 userinfo로 `sub`와 이메일을 얻는다
    ([authorization](https://developer.android.com/identity/authorization)). Credential Manager 로그인을 먼저 하는 방법도 있지만 웹 클라이언트 ID와
    라이브러리가 더 들어 고르지 않았다.
  - 이미 허락했으면 다음에 `authorize()`가 화면 없이 토큰을 준다. 토큰은 1시간이다. `Context`로 만드는 클라이언트가 있어 Activity 없이 부를 수 있다
    ([Identity](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/Identity)). 작업 관리자(WorkManager) 안에서
    부르는 것은 문서로 확인 못 함이라 T001과 같이 시험한다. 안 되면 자동 백업은 화면이 필요한 상태(`NeedsUser`)로 끝나고 앱을 열 때 이어 간다.
  - `setAccount(Account)`로 옛 백업 계정을 먼저 보일 수 있다
    ([AuthorizationRequest.Builder](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationRequest.Builder)).
  - 끊기는 `revokeAccess`, `clearToken`(21.4.0부터).
- iOS: GoogleSignIn-iOS 10.1.0(2026-10-23, SPM, iOS 15 이상)([릴리스](https://github.com/google/GoogleSignIn-iOS/releases/tag/10.1.0)).
  `addScopes`로 `drive.file`을 더하고, `restorePreviousSignIn`·`refreshTokensIfNeeded`로 화면 없이 토큰을 받는다. 토큰은 키체인(처음 잠금 해제 뒤, 이 기기만)에 있다.
  Info.plist에 `GIDClientID`와 거꾸로 쓴 클라이언트 ID URL 스킴이 필요하다([설정](https://developers.google.com/identity/sign-in/ios/start-integrating)).
  백그라운드 작업 안에서 토큰 갱신이 되는지는 확인 못 함이라 시험한다.
- 계정 구분 값은 구글 계정 고유 ID(`sub`)다. 이메일은 바뀔 수 있어 화면 표시에만 쓴다.
- iOS 구현은 Swift 쪽이 `DriveAuthorizer`를 구현해 `MainViewController`에 넘긴다(스펙 001 R21 `Telemetry`와 같은 방법).
- 비용: 둘 다 무료다. 동의 화면에 앱 이름·로고를 띄우는 브랜드 검증도 무료이고 도메인과 개인정보처리방침이 필요하다(`docs/release.md`).

### R3. 드라이브 REST는 공통 코드에서 Ktor로 직접 부른다

- Decision: `shared/core/drive`에 `DriveApi`(`contracts/drive-gateway.md`)를 Ktor 3.6.1(Android OkHttp, iOS Darwin)로 만든다. 구글 Java 클라이언트
  라이브러리는 JVM 전용이라 iOS와 나눠 쓸 수 없다.
- 이어 올리기: 조각은 256KiB의 배수, 끊기면 빈 PUT(`Content-Range: bytes */전체`)로 받은 곳을 묻고, 세션 주소는 일주일 간다
  ([업로드](https://developers.google.com/workspace/drive/api/guides/manage-uploads)). 조각 크기는 8MiB로 둔다(사진 대부분이 한 조각).
- 본문은 파일에서 흘려 보낸다. Ktor 문서의 공통 코드 예는 multipart뿐이고, 날 본문은 `ChannelWriterContent`와 `writeSource`로 만든다
  ([ChannelWriterContent](https://api.ktor.io/ktor-http/io.ktor.http.content/-channel-writer-content/index.html)). 메모리 사용은 SC-005 시험으로 본다.
- Darwin 엔진은 백그라운드 URLSession을 지원한다는 문서가 없다. iOS 백그라운드 올리기는 R15에서 따로 정한다.
- 한도: 프로젝트 분당 1,000,000단위, 사용자 분당 325,000단위([한도](https://developers.google.com/workspace/drive/api/guides/limits), 2026-10-30 갱신).
  하루 사진 수백 장 백업은 한도와 멀다.
- 비용: 지금은 무료다. 같은 페이지에 한도를 넘는 쓰임에 2026년 안에 요금을 매길 계획이고 90일 전에 알린다고 적혀 있다. 날짜는 아직 없다.
  우리 쓰임은 한도 안이라 영향이 없을 것으로 보지만, 공지가 나오면 `docs/backup.md` "지켜볼 것"에서 다시 본다(헌법 IV).

### R4. 사진 확인값은 Okio로 잰다

- Decision: Okio 3.18.2의 `HashingSink.sha256`(공통 코드)과 `FileSystem.SYSTEM`(Android·iOS)으로 흘려 가며 잰다. 스펙 001·002가 사진 이름을 만들 때도 같은 함수를 쓴다.
- Rationale: Okio는 DataStore·Coil이 이미 끌어온다. 저장소가 `github.com/lysine-dev/okio`로 옮겨졌다(새 문서 [lysine.dev/okio](https://lysine.dev/okio/)).
- Alternatives: `org.kotlincrypto.hash:sha2` 0.8.0(Apache-2.0). 의존이 하나 늘어 고르지 않았다.

### R5. 스냅샷은 `VACUUM INTO` 사본에서 기기 표를 비운 것

- Decision: `contracts/backup-format.md` "스냅샷". 번들 SQLite의 `VACUUM INTO`는 열린 DB를 한 파일로 뜬다([VACUUM](https://www.sqlite.org/lang_vacuum.html)).
  사본에서 `backup_media`, `sync_base`, `merge_mark`, `draft`, `draft_media`를 비운다.
- 임시 저장본을 빼는 까닭: 기기마다 하나씩 있는 쓰던 글이라 다른 기기에서 열면 그 기기의 쓰던 글과 부딪친다. 저장한 일기는 모두 들어간다.
- 설정은 DB가 아니라 목록 파일의 `settings`에 담는다. 잠금 확인값은 담지 않는다(스펙 001 R11).

### R6. 목록 파일이 완료 표시, 드라이브 확인값으로 끝을 확인한다

- Decision: 사진 → 스냅샷 → 목록 파일 순서. 파일마다 드라이브의 `sha256Checksum`을 견주고, 비어 있으면 `md5Checksum`을 쓴다.
- 사실: `md5Checksum`은 바이너리 파일에만, `sha256Checksum`은 "있을 때" 채워진다([files](https://developers.google.com/workspace/drive/api/reference/rest/v3/files)).
  그래서 MD5를 함께 잰다(Okio `HashingSink.md5`).
- 목록 파일을 고칠 수 없게 하지는 않는다. 사용자가 드라이브에서 지우거나 바꾸면 그 백업은 목록에서 "깨졌어요"로 보인다.

### R7. 폴더와 파일은 `appProperties`로 찾는다

- Decision: `diary.kind`, `diary.device`, `diary.format`, `diary.unrefSince`. 앱마다 파일당 30개, 키+값 124바이트까지이고 다른 앱에는 안 보인다
  ([properties](https://developers.google.com/workspace/drive/api/guides/properties)). `appProperties has { key='diary.kind' and value='root' }`로 찾는다
  ([검색](https://developers.google.com/workspace/drive/api/guides/search-files)).
- 사용자가 폴더 이름을 바꿔도 이어지고, 다른 이름이 같은 폴더와 헷갈리지 않는다.

### R8. 공간이 모자라면 글 먼저, 최근 사진부터 (사용자 결정 2026-11-01)

- `about.get`의 `storageQuota.limit`·`usage`를 본다. `limit`이 없으면 한도가 없는 계정이다([about](https://developers.google.com/workspace/drive/api/reference/rest/v3/about)).
- 스냅샷·목록 파일 자리를 먼저 떼고 남은 공간에 최근 일기의 사진부터 고른다. 순서(사진 → 스냅샷 → 목록 파일)는 바꾸지 않는다. 고르지 못한 사진은
  목록 파일에 `inBackup=false`로 남는다.
- 정리한 파일은 드라이브 휴지통으로 가서 30일 동안 공간을 차지한다. 안내에 휴지통 비우기를 넣는다.

### R9. 보관은 기기마다 최근 5개 + 달마다 1개 (사용자 결정 2026-11-01)

- 기기마다 자기 백업만 정리해 두 기기가 서로의 백업을 지우지 않는다.
- 달마다 1개는 기한 없이 남긴다. 스냅샷은 DB 하나(1만 편이 수십 MB)라 1년에 12개가 더해져도 작고, 사진은 여러 스냅샷이 함께 가리켜 늘지 않는다.
  옛 스냅샷이 가리키는 사진은 그 스냅샷이 남는 동안 지우지 않는다. 지운 사진도 옛 시점으로 되돌릴 수 있게 하려는 것이라 공간보다 기록을 앞에 둔다.

### R10. 사진 정리는 30일 동안 아무도 안 가리킬 때만

- 모든 기기의 목록 파일을 읽어 가리키는 해시를 모으고, 안 가리키는 사진에 `diary.unrefSince`를 적어 30일 뒤에도 그대로면 휴지통에 넣는다.
  기기에 상태를 두지 않아 어느 기기가 정리해도 같다.

### R11. 합치기는 `sync_base`로 어느 쪽이 고쳐졌는지 가른다

- Decision: `contracts/merge-rules.md`. 일기마다 "마지막으로 백업·복원으로 맞춰진 `updated_at`"을 기기에 두고, 그 뒤 양쪽이 다 바뀌었으면 둘 다 남긴다(FR-022).
- `updated_at`만으로 "늦은 쪽"을 고르면 양쪽에서 고친 경우 한쪽 고친 글을 잃는다. 기준값이 있어야 "둘 다 고쳤다"를 알 수 있다.
- 영구 삭제의 흔적(묘비)은 두지 않는다. 영구 삭제한 일기가 백업에 있으면 다시 생긴다. 지우는 쪽으로 틀리는 것보다 남기는 쪽이 헌법 I에 맞고,
  미리보기 숫자로 사용자가 미리 안다. 같은 계정의 여러 기기 동기화(나중 스펙)에서 다시 본다.
- 스펙 002 작업에 "즐겨찾기·휴지통도 `updated_at`을 바꾼다"를 더한다(`data-model.md` `updated_at` 규칙).

### R12. 복원은 글을 한 트랜잭션으로 먼저, 사진은 그 뒤에 이어 받기

- Decision: 받은 스냅샷 확인 → 미리보기(쓰기 없이 같은 규칙) → 한 트랜잭션으로 합치기 → 사진은 `pending`으로 두고 이어 받기. 사진 하나를 받을 때마다
  확인값을 맞추고 `normal`로 바꾼다.
- 일기 글이 먼저 보여 새 폰에서 바로 쓸 수 있고, 사진이 끊겨도 `pending`만 다시 받으면 된다. 이를 위해 `media.status`에 `pending`을 더한다
  (스펙 001 표를 바꿈, 사용자 확인 필요).
- 복원할 기기의 공간은 시작 전에 (받을 사진 + 스냅샷) × 1.1로 본다(FR-025).

### R13. 바꾸기는 기기 안 사본과 7일 되돌리기 (사용자 결정 2026-11-01)

- Decision: `contracts/merge-rules.md` "바꾸기", "되돌리기". 사본은 `VACUUM INTO`한 DB와 설정 JSON이다. 사진 파일은 지우지 않고 7일 동안 사진 정리를 멈춘다.
- 되돌릴 때 바꾸기 뒤에 쓴 일기는 남긴다. "바꾸기 전으로"가 그 사이 쓴 일기를 지우면 헌법 I을 어긴다.
- 레퍼런스: Journey는 가져오기가 덮어쓸 수 있고, Daylio 파일 가져오기는 바꾸기다. 두 앱 모두 사용자가 미리 백업해 두라고 한다(스펙 근거). 우리는 앱이 사본을 뜬다.

### R14. Android 자동 백업은 작업 관리자, 지금 백업은 사용자가 시작한 데이터 전송 작업

- 자동: WorkManager 2.12.0(2026-09-23, minSdk 24)의 하루 주기 작업 하나. 조건은 배터리 부족 아님만 걸고 네트워크는 작업 안에서 본다.
  네트워크가 없으면 실패(`OFFLINE`)로 적고 끝낸다. 조건에 네트워크를 걸면 오래 끊긴 기기에서 작업이 아예 돌지 않아 7일 알림 판단도 못 하기 때문이다.
  - 데이터 요금이 없는 연결(`UNMETERED`)이 아니면 사진 없이 스냅샷과 목록 파일만 올리고, 남은 사진은 `UNMETERED` 조건의 일회성 작업(이름 하나로 겹치지 않게)에
    넘긴다. 작업 묶음(chain)은 일회성 작업만 받아 주기 작업과 묶지 않는다
    ([NetworkType](https://developer.android.com/reference/androidx/work/NetworkType), [작업 잇기](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/chain-work)).
  - 백그라운드에서 시작한 작업이 스스로 포그라운드 서비스가 되는 것은 Android 12부터 막힐 수 있다(`setForeground`가 예외를 낼 수 있다고 문서에 있음).
    그래서 자동 백업은 포그라운드 서비스에 기대지 않고 10분 안쪽 조각으로 돈다. 다 못 하면 상태 파일에 남기고 다음 작업에 넘긴다
    ([백그라운드 시작 제한](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start),
    [dataSync 옮기기](https://developer.android.com/about/versions/15/changes/datasync-migration)).
- 지금 백업·복원 사진 받기(사용자가 누름):
  - Android 14 이상: `JobInfo.Builder.setUserInitiated(true)`(사용자가 시작한 데이터 전송). `RUN_USER_INITIATED_JOBS` 권한, 네트워크 조건, 알림이 반드시
    필요하고, 한도 없이 바로 돈다. 자동 기능에는 쓰지 말라고 적혀 있어 사용자가 누른 것에만 쓴다. 작업 관리자 지원이 없어 `JobService`를 직접 둔다
    ([setUserInitiated](https://developer.android.com/reference/android/app/job/JobInfo.Builder#setUserInitiated(boolean)), [UIDT](https://developer.android.com/develop/background-work/background-tasks/uidt)).
  - Android 13 이하: 앱이 앞에 있을 때 작업 관리자의 급한 작업(expedited) + `setForeground`(서비스 종류 `dataSync`). 앞에 있을 때 시작하니 백그라운드
    시작 제한에 걸리지 않는다([오래 도는 작업](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)).
  - 둘 다 같은 `BackupRunner`를 부른다. 진행 알림에는 단계와 사진 n/N만 보인다.
- 매니페스트: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `RUN_USER_INITIATED_JOBS`, 작업 관리자의 `SystemForegroundService`에
  `foregroundServiceType="dataSync"`를 합친다. targetSdk 35부터 `dataSync`는 하루 6시간까지이고 넘으면 `onTimeout`에서 멈춰야 한다. 멈추면 상태를 남기고
  다음에 잇는다([타임아웃](https://developer.android.com/develop/background-work/services/fgs/timeout)).
- 알림 허용(POST_NOTIFICATIONS)이 없어도 포그라운드 서비스는 돈다. 알림이 서랍에 안 보일 뿐이다
  ([알림 권한](https://developer.android.com/develop/ui/views/notifications/notification-permission)).
- Play Console에서 포그라운드 서비스 종류(`dataSync`)를 신고해야 한다. `docs/release.md`에 더한다.

### R15. iOS는 백그라운드 처리 작업과 앱을 닫을 때·열 때, 지금 백업은 앱 안에서

- iOS 출시는 R2지만(`docs/PRD.md`) 처음부터 같은 공통 코드로 시뮬레이터에서 돈다.
- 자동: `BGProcessingTask`(`requiresNetworkConnectivity`) 하루 1번을 요청한다. 기기가 쉬는 동안 몇 분 돌고, 사용자가 기기를 쓰면 멈춘다. 실행은 보장되지
  않는다([BGProcessingTask](https://developer.apple.com/documentation/backgroundtasks/bgprocessingtask),
  [earliestBeginDate](https://developer.apple.com/documentation/backgroundtasks/bgtaskrequest/earliestbegindate)). 그래서 앱이 뒤로 갈 때와 열릴 때
  마지막 성공이 24시간 넘었으면 시작한다(`docs/backup.md`, FR-015). 뒤로 갈 때는 `beginBackgroundTask`의 남은 시간 안에서 조각으로 돈다.
  Info.plist에 `BGTaskSchedulerPermittedIdentifiers`와 `UIBackgroundModes`의 `processing`을 둔다.
- 지금 백업·복원: 앱이 앞에 있는 동안 공통 코드로 돈다. iOS 26 이상이면 `BGContinuedProcessingTask`로 넘겨 앱을 떠나도 시스템 진행 표시와 함께 잇는다.
  사용자가 누른 동작에서 앞에 있을 때만 요청할 수 있고, 앱을 쓸어 닫으면 취소된다
  ([오래 도는 작업](https://developer.apple.com/documentation/backgroundtasks/performing-long-running-tasks-on-ios-and-ipados)). iOS 17~25는 앱을 떠나면
  `beginBackgroundTask` 시간만큼 하고 다음에 잇는다.
- 백그라운드 URLSession 올리기는 이번에 쓰지 않는다. 파일 올리기만 되고, 수천 개를 걸지 말고 적고 큰 전송으로 바꾸라고 Apple이 권한다
  ([백그라운드 받기](https://developer.apple.com/documentation/foundation/downloading-files-in-the-background)). 사진 수천 장 구조와 맞지 않고, Ktor
  Darwin 엔진도 지원 문서가 없다. 실제 사용에서 백업이 자꾸 밀리면 열린 결정 O2로 다시 본다.
- iOS 기기 백업(iCloud·컴퓨터)에는 앱 데이터가 그대로 들어간다(줄임 사진만 뺌). 아이폰을 바꿀 때 기록이 따라가는 길이라 막지 않는다.

### R16. 7일 실패 알림은 미리 예약해 두고, 성공할 때마다 다시 잡는다

- 백업이 성공할 때마다 "마지막 성공 + 7일"에 알림 하나를 예약하고(같은 ID로 바꿔 끼움), 실패가 기록될 때마다 그 알림의 본문을 실패 종류에 맞게 바꾼다.
  Android는 지연 7일의 일회성 작업이 알림을 올리고(그때도 실패 중일 때만), iOS는 로컬 알림 시간 예약을 쓴다.
- 기기가 7일 동안 백업 작업을 한 번도 못 돌려도(오프라인, iOS 백그라운드 미실행) 알림이 온다. 실패 기록이 없으면 "7일 동안 백업하지 못했어요"와 연결 확인 안내를 쓴다.
- 알림 허용이 없으면 홈의 빨간 점으로 대신한다(FR-016). 알림 허용 묻기는 스펙 004다.

### R17. 연결 안 한 사람에게 권하기: 상태 줄은 늘, 카드는 세 번까지 (사용자 결정 2026-11-01)

- 근거: 구글 포토는 백업이 꺼져 있으면 상태와 함께 켜기 버튼을 두고([구글 포토](https://support.google.com/photos/answer/6174018)), Day One은 동기화를 꺼도
  상태 줄을 없애지 않는다([Day One](https://dayoneapp.com/releases/easily-check-sync-status-in-settings/)). 화면을 막는 창이나 푸시로 조르는 예는 공식 자료에서 찾지 못했다.
- 규칙은 `contracts/backup-flow.md` "권하기 카드". 쓰기·읽기 화면과 전면 광고 자리(저장 뒤 목록으로 돌아갈 때)에는 띄우지 않는다. 카드는 홈 목록 안의 줄이다.

### R18. 묶음 파일은 직접 만든 압축 없는 ZIP64 쓰기, 읽기는 Okio

- 쓰기: `shared/core/backup`의 `BundleWriter`가 공통 코드로 STORE(압축 없음) 방식만 쓴다. 4GB나 항목 65,535개를 넘으면 ZIP64 기록을 쓴다
  ([APPNOTE 6.3.10](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT) 4.3.14~4.3.15, 4.5.3). 사진은 이미 JPEG라 압축 이득이 작다.
- 읽기: Okio `FileSystem.openZip`. ZIP64를 읽고, STORE·DEFLATE만 받고, 3.9.0부터 Kotlin/Native에서 돈다
  ([변경 기록](https://github.com/square/okio/blob/main/CHANGELOG.md)). Android·iOS만 대상인 공통 코드에서 보이는지는 확인 못 함이라 첫 개발 작업에서 본다.
  안 보이면 Android는 `java.util.zip.ZipInputStream`(API 24 소스에 ZIP64 읽기·쓰기가 있음), iOS는 ZIPFoundation(MIT)을 Swift 쪽에 둔다.
- 다른 안: 공통 코드 zip 라이브러리 중 ZIP64 흘려 쓰기가 되는 것은 `no.synth:kmp-zip` 2.0.0(MPL-2.0)뿐인데 ZIP64가 2026-10-28에 막 들어갔다.
  kzip은 ZIP64가 없고, korlibs-io는 32비트 칸만 쓴다. 직접 쓰는 STORE 쓰기는 짧고 시험(Android `ZipInputStream`, Okio, 컴퓨터 `unzip -t`로 읽어 보기)으로 지킬 수 있다.
- iOS `NSFileCoordinator`의 `.forUploading`은 폴더를 zip으로 만들어 주지만 ZIP64와 크기 한도가 문서에 없어 쓰지 않는다.

### R19. 더하는 의존성과 버전

| 묶음 | 버전 | 확인 | 라이선스 |
| --- | --- | --- | --- |
| Ktor(`client-core`, `okhttp`, `darwin`, `content-negotiation`, `serialization-kotlinx-json`) | 3.6.1 | [Maven Central](https://repo1.maven.org/maven2/io/ktor/ktor-client-core/maven-metadata.xml) | Apache-2.0 |
| Okio | 3.18.2 (2026-09-04) | [Maven Central](https://repo1.maven.org/maven2/com/squareup/okio/okio/maven-metadata.xml) | Apache-2.0 |
| `play-services-auth` | 22.0.0 (2026-08-25) | [Google Maven](https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-auth/maven-metadata.xml) | Android SDK 라이선스 |
| `androidx.work:work-runtime` | 2.12.0 (2026-09-23) | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/maven-metadata.xml) | Apache-2.0 |
| GoogleSignIn-iOS (SPM) | 10.1.0 (2026-10-23) | [GitHub](https://github.com/google/GoogleSignIn-iOS/releases/tag/10.1.0) | Apache-2.0 |

알파·베타는 쓰지 않는다. 유료 구간이 있는 것은 드라이브 API(R3)뿐이고 지금은 무료다.

### R20. Android 자동 백업은 끄고, 기기 간 이전에는 일기를 담는다 (스펙 001 O5를 닫음)

- 사실: Android 클라우드 자동 백업은 앱당 25MB이고 넘으면 클라우드에 올리지 않는다. 기기 간 이전은 크기 한도가 적혀 있지 않다. Android 12부터
  `dataExtractionRules`의 `cloud-backup`과 `device-transfer`를 따로 정하고, 11 이하는 `fullBackupContent`를 쓴다
  ([자동 백업](https://developer.android.com/identity/data/autobackup)).
- Decision:
  - 클라우드 자동 백업: 일기 DB, 사진, 설정을 모두 뺀다. 사용자가 고른 저장소가 아니고(헌법 II), 25MB를 넘는 순간 말없이 멈춰 "백업되는 줄 알았다"를
    다시 만든다. 백업은 이 스펙의 드라이브 백업 하나로 분명하게 둔다.
  - 기기 간 이전(새 폰 설정 때 케이블·무선으로 옮기기): `diary.db`, `files/media/`, `settings.preferences_pb`만 담는다. 사용자가 자기 기기 사이에서
    시작하는 이전이라 헌법 II 안이다. 레거시 `note.db`, `legacy-snapshot/`, `migration/`, 잠금, 백업 설정, `backup/`은 뺀다. 레거시 DB가 따라가면
    새 기기에서 이전이 다시 돌아 일기가 두 번 생길 수 있다.
  - 11 이하에는 기기 간 이전만 담는 `requireFlags="deviceToDeviceTransfer"`를 쓴다(개발 때 공식 문서로 다시 확인).
- 시험: 기기 간 이전 뒤 새 기기에서 이전이 다시 돌지 않고 일기·사진이 같은지 본다(개발 작업).

### R21. 상태 표시와 문구

- 상태 줄과 설정의 상태는 `BackupStatusRepository` 하나의 흐름에서 나온다. 문구는 문자열 리소스이고 디자인 단계에서 한국어 스킬로 다듬는다.
- 시각은 상대 표기(오늘·어제·n일 전)와 시각을 쓴다. 진행 숫자는 바이트 단위로도 나아가 멈춰 보이지 않게 한다(SC-005).

## 열린 결정

| 번호 | 무엇 | 정하는 조건 | 기본값 |
| --- | --- | --- | --- |
| O1 | Android가 만든 파일을 iOS가 보는지(R1) | 첫 작업 결과 | 보인다고 보고 설계, 안 보이면 Picker → 묶음 파일 |
| O2 | iOS 백그라운드 URLSession 올리기 | iOS 실제 사용에서 자동 백업 성공률(SC-008)이 낮으면 | 쓰지 않음 |
| O3 | 묶음 파일을 저장 위치에 바로 흘려 쓰기 | FileKit 0.17.0이 큰 파일을 흘려 쓸 수 있는지 첫 개발에서 확인. 안 되면 Android SAF 출력 스트림, iOS는 앱 폴더에 만든 뒤 공유 시트 | 바로 흘려 쓰기 |
| O4 | 작업 관리자·백그라운드 작업 안에서 토큰 받기(R2) | 첫 작업과 함께 시험 | 된다고 보고, 안 되면 앱을 열 때 잇기 |
| O5 | 조각 크기 8MiB와 동시에 올릴 사진 수 | SC-004 시간을 재고 | 8MiB, 한 번에 2장 |

## 출처

- 드라이브: [스코프](https://developers.google.com/workspace/drive/api/guides/api-specific-auth), [files](https://developers.google.com/workspace/drive/api/reference/rest/v3/files), [업로드](https://developers.google.com/workspace/drive/api/guides/manage-uploads), [about](https://developers.google.com/workspace/drive/api/reference/rest/v3/about), [검색](https://developers.google.com/workspace/drive/api/guides/search-files), [properties](https://developers.google.com/workspace/drive/api/guides/properties), [한도](https://developers.google.com/workspace/drive/api/guides/limits), [Picker 데스크톱·모바일](https://developers.google.com/workspace/drive/picker/guides/desktop-mobile-picker)
- 권한: [Android authorization](https://developer.android.com/identity/authorization), [AuthorizationRequest.Builder](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationRequest.Builder), [Play 서비스 출시 노트](https://developers.google.com/android/guides/releases), [cross-client identity](https://developers.google.com/identity/protocols/oauth2/cross-client-identity), [GoogleSignIn-iOS](https://github.com/google/GoogleSignIn-iOS/releases/tag/10.1.0), [iOS 설정](https://developers.google.com/identity/sign-in/ios/start-integrating)
- Android 백그라운드: [WorkManager 출시](https://developer.android.com/jetpack/androidx/releases/work), [오래 도는 작업](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running), [서비스 종류](https://developer.android.com/develop/background-work/services/fgs/service-types), [타임아웃](https://developer.android.com/develop/background-work/services/fgs/timeout), [백그라운드 시작 제한](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start), [UIDT](https://developer.android.com/develop/background-work/background-tasks/uidt), [자동 백업](https://developer.android.com/identity/data/autobackup)
- iOS 백그라운드: [BGProcessingTask](https://developer.apple.com/documentation/backgroundtasks/bgprocessingtask), [BGContinuedProcessingTask](https://developer.apple.com/documentation/backgroundtasks/bgcontinuedprocessingtask), [beginBackgroundTask](https://developer.apple.com/documentation/uikit/uiapplication/beginbackgroundtask(expirationhandler:)), [백그라운드 받기](https://developer.apple.com/documentation/foundation/downloading-files-in-the-background)
- 파일: [Ktor 엔진](https://ktor.io/docs/client-engines.html), [Okio](https://lysine.dev/okio/), [APPNOTE](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT), [SQLite VACUUM](https://www.sqlite.org/lang_vacuum.html)
- 레거시 사실: `app/src/main/java/org/sjhstudio/diary/BackupActivity.java`(`DRIVE_FILE` 범위, `"<계정>'s id"` 저장), `googledrive/DriveServiceHelper.java`(내 드라이브 맨 위에 `note.db`, `application/x-sqlite3`, 설명 "Diary app backup file")
