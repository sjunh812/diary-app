package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.sjhstudio.diary.core.data.BackupList
import org.sjhstudio.diary.core.drive.ChunkResult
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DrivePage
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.drive.StorageQuota
import org.sjhstudio.diary.core.drive.UploadSession
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.DriveAccount
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.TokenResult
import org.sjhstudio.diary.core.testing.FakeDrive

/**
 * 로그인된 계정이 연결해 둔 계정과 다를 때
 * Android가 새 계정 이메일을 먼저 적은 뒤 백업 설정 적기가 실패하거나 앱이 꺼진 경우
 * 백업·복원 목록·사진 받기가 화면의 계정과 다른 드라이브를 쓰지 않아야 함
 */
class ConnectedAccountTokenTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var firstDrive: FakeDrive
    private lateinit var secondDrive: FakeDrive
    private lateinit var a: TestPhone
    private lateinit var b: TestPhone

    private val first = DriveAccount("sub-1", "first@gmail.com")
    private val second = DriveAccount("sub-2", "second@gmail.com")

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "connected-token-${Random.nextLong()}"
        firstDrive = FakeDrive(fs)
        secondDrive = FakeDrive(fs)
        a = TestPhone(dir / "a", secondDrive, name = "A")
        b = TestPhone(dir / "b", firstDrive, name = "B")
    }

    @AfterTest
    fun tearDown() {
        a.close()
        b.close()
        fs.deleteRecursively(dir)
    }

    /** 앱이 드라이브에 쓰는 인증. DI와 같게 연결 계정 토큰만 내줌 */
    private fun appAuthorizer(): DriveAuthorizer = ConnectedAccountAuthorizer(b.auth, b.store)

    private val requests = mutableMapOf<String, Int>()

    /** 실제 드라이브처럼 토큰의 계정 드라이브를 씀 */
    private fun drives() = AccountDrives(appAuthorizer(), mapOf(first.key to firstDrive, second.key to secondDrive), requests)

    private val secondRequests: Int
        get() = requests[second.key] ?: 0

    /** 두 번째 계정 드라이브에 다른 기기 백업이 있고, 이 기기는 첫 계정으로 연결돼 있으나 로그인은 두 번째 계정 */
    private suspend fun signedInElsewhere() {
        a.write("one")
        assertIs<BackupOutcome.Done>(a.runner().run())
        b.store.update { it.copy(accountKey = first.key, accountLabel = first.email) }
        b.auth.account = second
    }

    @Test
    fun the_restore_list_does_not_show_another_accounts_backups() = runTest {
        signedInElsewhere()
        val controller = b.controller(backgroundScope, api = drives())

        val list = controller.backups()

        assertEquals(BackupFailure.AUTH, assertIs<BackupList.Failed>(list).kind)
        assertEquals(0, secondRequests)
    }

    @Test
    fun a_backup_does_not_go_to_another_accounts_drive() = runTest {
        signedInElsewhere()
        b.write("mine")
        val before = secondDrive.visible.size

        val outcome = b.runner(api = drives(), authorizer = appAuthorizer()).run()

        assertEquals(BackupFailure.AUTH, assertIs<BackupOutcome.Failed>(outcome).kind)
        assertEquals(before, secondDrive.visible.size)
    }

    @Test
    fun the_connected_account_still_works() = runTest {
        signedInElsewhere()
        b.auth.account = first
        b.write("mine")

        assertIs<BackupOutcome.Done>(b.runner(api = drives(), authorizer = appAuthorizer()).run())
        assertEquals(0, secondRequests)
    }

    @Test
    fun nothing_connected_yet_passes_the_token_through() = runTest {
        b.auth.account = second

        assertIs<TokenResult.Token>(appAuthorizer().accessToken())
    }
}

/** 토큰의 계정마다 다른 드라이브. 실제 드라이브 호출처럼 토큰을 받은 뒤 부름 */
private class AccountDrives(
    private val authorizer: DriveAuthorizer,
    private val drives: Map<String, FakeDrive>,
    private val requests: MutableMap<String, Int>
) : DriveApi {
    private suspend fun drive(): DriveApi = when (val token = authorizer.accessToken()) {
        is TokenResult.Token -> {
            requests[token.account.key] = (requests[token.account.key] ?: 0) + 1
            drives.getValue(token.account.key)
        }

        TokenResult.NeedsUser -> throw DriveException(BackupFailure.AUTH)

        is TokenResult.Failed -> throw DriveException(token.kind)
    }

    override suspend fun about(): StorageQuota = drive().about()

    override suspend fun list(query: DriveQuery, pageToken: String?): DrivePage = drive().list(query, pageToken)

    override suspend fun createFolder(name: String, parentId: String?, props: Map<String, String>): DriveFile =
        drive().createFolder(name, parentId, props)

    override suspend fun uploadSmall(meta: NewFile, body: Path): DriveFile = drive().uploadSmall(meta, body)

    override suspend fun startResumable(meta: NewFile, size: Long): UploadSession = drive().startResumable(meta, size)

    override suspend fun uploadChunk(session: UploadSession, from: Long, body: Path): ChunkResult = drive().uploadChunk(session, from, body)

    override suspend fun queryResumable(session: UploadSession): ChunkResult = drive().queryResumable(session)

    override suspend fun get(fileId: String): DriveFile = drive().get(fileId)

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long = drive().download(fileId, to, fromByte)

    override suspend fun trash(fileId: String) = drive().trash(fileId)
}
