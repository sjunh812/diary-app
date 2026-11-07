# 드라이브 연결과 호출 경계

공통 코드가 드라이브를 부르는 인터페이스와, 플랫폼마다 다른 권한 받기를 정한다. 근거는 [research.md](../research.md) R1~R4다.
시험은 같은 인터페이스의 `FakeDrive`(`shared/core/testing`)로 돈다.

## 권한 받기: `DriveAuthorizer` (`shared/core/platform`, 플랫폼 구현)

```kotlin
interface DriveAuthorizer {
    /** 화면이 필요한 연결. 설정·권하기 카드·온보딩에서 부른다. hint는 먼저 보일 계정 이메일. */
    suspend fun connect(hint: String?): ConnectResult

    /** 화면 없이 토큰을 받는다. 백그라운드 작업에서도 부른다. */
    suspend fun accessToken(): TokenResult

    /** 기기의 토큰을 지우고 이 앱에 준 권한을 거둔다. 드라이브의 백업 파일은 지우지 않는다(FR-004). */
    suspend fun disconnect()
}

sealed interface ConnectResult {
    data class Connected(val account: DriveAccount) : ConnectResult
    data object Cancelled : ConnectResult
    data class Failed(val kind: BackupFailure) : ConnectResult
}

sealed interface TokenResult {
    data class Token(val value: String, val account: DriveAccount) : TokenResult
    data object NeedsUser : TokenResult          // 권한이 없거나 거둬짐 → 상태 "다시 연결이 필요해요"
    data class Failed(val kind: BackupFailure) : TokenResult
}

data class DriveAccount(val key: String, val email: String)  // key = 구글 계정 고유 ID(sub)
```

| | Android | iOS |
| --- | --- | --- |
| 라이브러리 | `play-services-auth` 22.0.0 `AuthorizationClient` | GoogleSignIn-iOS 10.1.0(SPM), Swift 쪽 구현을 Kotlin에 넘김 |
| 범위 | `drive.file`, `openid`, `email` | 기본 프로필 + `addScopes([drive.file])` |
| 계정 알기 | 받은 토큰으로 userinfo를 불러 `sub`, `email`을 얻는다(응답에 계정이 없다) | `GIDGoogleUser.userID`, `profile.email` |
| 계정 먼저 보이기 | `AuthorizationRequest.Builder.setAccount(Account(hint, "com.google"))` | 없음(옛 백업 계정은 iOS에 없다) |
| 화면 없이 토큰 | `authorize()`를 다시 부르면 이미 허락했을 때 화면 없이 토큰. 허락이 없으면 PendingIntent → `NeedsUser` | `restorePreviousSignIn` 뒤 `refreshTokensIfNeeded` |
| 토큰 보관 | 기기에 두지 않는다. 1시간짜리를 그때그때 받는다 | GoogleSignIn이 키체인(이 기기 전용)에 둔다 |
| 끊기 | `revokeAccess`, `clearToken` | `disconnect` |

- 토큰과 계정 이메일은 로그, 사용 통계, 오류 보고에 넣지 않는다.
- 클라이언트 ID는 저장소에 올리지 않고 빌드 설정에서 넣는다. Android 클라이언트는 패키지 이름과 서명 지문으로 묶이므로 비밀값은 아니지만 함께 다룬다.

## 드라이브 호출: `DriveApi` (`shared/core/drive`, 공통 코드)

Ktor 3.6.1(Android OkHttp, iOS Darwin)로 드라이브 REST v3를 직접 부른다. 토큰은 부를 때마다 `DriveAuthorizer.accessToken()`에서 받는다.

```kotlin
interface DriveApi {
    suspend fun about(): StorageQuota                                      // limit이 null이면 한도 없음
    suspend fun list(query: DriveQuery, pageToken: String?): DrivePage     // pageSize 1000, trashed=false
    suspend fun createFolder(name: String, parentId: String?, props: Map<String, String>): DriveFile
    suspend fun uploadSmall(meta: NewFile, body: FileRef): DriveFile       // 5MB 이하: multipart 한 번
    suspend fun startResumable(meta: NewFile, size: Long): UploadSession   // 그보다 크면
    suspend fun uploadChunk(session: UploadSession, from: Long, body: FileRef): ChunkResult
    suspend fun queryResumable(session: UploadSession): ChunkResult        // 끊긴 뒤 어디까지 갔는지
    suspend fun get(fileId: String): DriveFile
    suspend fun download(fileId: String, to: FileRef, fromByte: Long): Long
    suspend fun setProperties(fileId: String, props: Map<String, String?>): DriveFile
    suspend fun trash(fileId: String)
}

data class DriveFile(
    val id: String, val name: String, val size: Long?, val parents: List<String>,
    val sha256: String?, val md5: String?, val props: Map<String, String>, val createdAt: Long,
)

sealed interface ChunkResult {
    data class Progress(val confirmedBytes: Long) : ChunkResult
    data class Done(val file: DriveFile) : ChunkResult
    data object Expired : ChunkResult            // 세션이 없어짐(404, 일주일 지남) → 처음부터
}
```

- 큰 파일은 이어 올리기(resumable)로 올린다. 조각은 256KiB의 배수(8MiB)다. 끊기면 `queryResumable`로 받은 곳부터 다시 보낸다.
  세션 주소는 `backup/state.json`에 남겨 앱이 다시 열려도 잇는다(일주일 동안 유효).
- 본문은 파일에서 흘려 보낸다(`ChannelWriterContent` + Okio 소스). 사진 하나를 통째로 메모리에 올리지 않는다(SC-005).
- 받은 파일 정보에는 늘 `id,name,size,parents,sha256Checksum,md5Checksum,appProperties,createdTime`을 달라고 한다.
- 이전 버전 백업 찾기: `legacy_backup_file_id`가 있으면 `get`으로 먼저 보고, 없거나 실패하면 `name = 'note.db' and trashed = false`로 찾는다.

## 오류 나누기

| 드라이브 응답 | 종류 | 처리 |
| --- | --- | --- |
| 401, 토큰 받기 실패(권한 없음) | `AUTH` | 토큰을 한 번 새로 받아 다시. 또 실패하면 멈추고 다시 연결 안내 |
| 403 `storageQuotaExceeded` | `NO_SPACE` | `backup-format.md` "공간이 모자랄 때" |
| 403 `userRateLimitExceeded`·`rateLimitExceeded`, 429 | `DRIVE_ERROR`(다시 함) | 1, 2, 4, 8, 16초(+무작위)로 다섯 번까지 다시 |
| 5xx | `DRIVE_ERROR`(다시 함) | 위와 같음 |
| 404 | 파일·폴더가 없어짐 | 폴더면 다시 찾고 없으면 새로 만든다. 사진이면 `backup_media`에서 빼고 다시 올린다 |
| 403 그 밖(`insufficientPermissions` 등) | `AUTH` | 다시 연결 안내 |
| 연결 끊김, 시간 초과 | `OFFLINE` | 다음 기회에 이어서 |

## `FakeDrive` (`shared/core/testing`)

메모리에 파일을 두고 실제 드라이브처럼 SHA-256·MD5를 계산하고 `appProperties` 질의와 쪽 넘김을 흉내 낸다. 시험마다 아래를 끼워 넣을 수 있다.

- N바이트를 받은 뒤 끊기, 세션 만료, 특정 파일 내용 깨기(확인값 불일치), 목록 파일 깨기
- 공간 한도(남은 바이트), 한도 없음
- 한 번만 401, 속도 제한 몇 번, 5xx 몇 번
- 폴더 지우기(휴지통), 다른 계정으로 바꾸기
- 다른 클라이언트가 만든 파일 숨기기(research R1의 "안 보일 때"를 시험하려고)
