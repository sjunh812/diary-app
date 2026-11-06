package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.testing.FakeBackupScheduler
import org.sjhstudio.diary.core.testing.FakeDrive
import org.sjhstudio.diary.core.testing.FakeDriveAuthorizer
import org.sjhstudio.diary.core.testing.FakeNetworkState
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.TestClock

/**
 * 시험용 기기 하나.
 * DB, 사진 폴더, 설정, 시계를 따로 갖고 드라이브만 다른 기기와 같이 쓴다.
 */
internal class TestPhone(
    val root: Path,
    val drive: FakeDrive,
    start: Instant = Instant.parse("2026-11-05T01:00:00Z"),
    private val name: String = "Galaxy S24"
) {
    val fs: FileSystem = FileSystem.SYSTEM
    val media = root / "media"
    val work = root / "backup"
    val clock = TestClock(start)
    val store = BackupStore(InMemoryDataStore(emptyPreferences()))
    val auth = FakeDriveAuthorizer()
    val network = FakeNetworkState()
    val gate = BackupGate()
    var zone: TimeZone = TimeZone.UTC
    var freeBytes = 1_000_000_000L

    /** 복원이 복원한 설정. */
    val appliedSettings = mutableListOf<ManifestSettings>()

    /** 설정을 쓰다 실패하게 할 때 값을 넣는다. */
    var settingsFailure: Exception? = null

    val db: DiaryDatabase

    init {
        fs.createDirectories(media)
        db = Room.databaseBuilder<DiaryDatabase>((root / "diary.db").toString()).configured().build()
    }

    fun close() = db.close()

    fun runner(api: DriveApi = drive, deviceName: String = name) = BackupRunner(
        db, api, auth, store, network, DeviceName { deviceName }, fs, media, work,
        BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings(font = "pretendard") }, clock, { zone }, gate = gate
    )

    fun applier() = MergeApplier(db, { fs.exists(media / it) }, clock)

    fun planner(api: DriveApi = drive) = RestorePlanner(api, applier(), FreeSpace { freeBytes }, fs, work)

    fun catalog(api: DriveApi = drive) = RestoreCatalog(api, fs, work)

    fun fetcher(api: DriveApi = drive, files: FileSystem = fs) = PhotoFetcher(db, api, files, media, work, FreeSpace { freeBytes })

    fun manager(files: FileSystem = fs, applier: MergeApplier = applier()) = RollbackManager(
        db, applier, store, FreeSpace { freeBytes }, files, work, { ManifestSettings(font = "before") }, { appliedSettings += it }, clock
    )

    fun exporter() = BundleExporter(
        db, store,
        DeviceName {
            name
        },
        fs, media, work, BackupAppInfo("2.0.0", 37, "android"), { "README" }, { ManifestSettings() }, clock, { zone }
    )

    fun controller(
        scope: CoroutineScope,
        photos: DriveApi = drive,
        files: FileSystem = fs,
        api: DriveApi = drive,
        rollback: RollbackManager = manager(files)
    ): RestoreController {
        val applier = lazy { applier() }
        return RestoreController(
            lazy { catalog(api) }, lazy { planner(api) }, applier, lazy { fetcher(photos, files) },
            lazy { LegacyBackupImporter(db, api, { _, _, _ -> null }, fs, work, clock, { zone }) },
            lazy { rollback },
            lazy { exporter() },
            lazy { db },
            store, network, files, work,
            { settings ->
                settingsFailure?.let { throw it }
                appliedSettings += settings
            },
            scope, gate, FakeBackupScheduler(), FreeSpace { freeBytes }, clock
        )
    }

    /**
     * 일기 하나를 넣는다.
     * 시각은 시계가 아니라 [updated]다.
     */
    suspend fun write(
        id: String,
        updated: Long = 10,
        date: String = "2026-10-31",
        body: String = id,
        deletedAt: Long? = null,
        legacyId: Int? = null
    ) {
        db.entryDao().insertAll(listOf(entry(id, updated, date, body, deletedAt, legacyId)))
    }

    /** 사진 파일과 줄을 넣고 내용 확인값을 돌려준다. */
    suspend fun photo(entryId: String, order: Int, content: String): String {
        val row = photoRow(entryId, order, content)
        fs.write(media / requireNotNull(row.fileName)) { writeUtf8(content) }
        db.mediaDao().insertAll(listOf(row))
        return requireNotNull(row.sha256)
    }

    suspend fun entries() = db.backupDao().allEntries().sortedBy { it.id }

    suspend fun ids() = db.backupDao().allEntries().map { it.id }.toSet()

    suspend fun photoStates() = db.backupDao().allMedia().associate { it.id to it.status }

    /** 받는 중 쓰던 임시 파일이 남았는지 본다. */
    fun partsLeft() = fs.listOrNull(work).orEmpty().filter { it.name.endsWith(".part") }

    /**
     * 드라이브에 파일 하나를 만들어 올린다.
     * 깨지거나 남이 만든 백업을 흉내 낸다.
     */
    suspend fun plant(name: String, bytes: ByteArray, props: Map<String, String>, parent: String? = null): DriveFile {
        fs.createDirectories(work)
        val path = work / "planted.tmp"
        fs.write(path) { write(bytes) }
        return drive.uploadSmall(NewFile(name, parent, "application/octet-stream", props), path).also { fs.delete(path) }
    }

    fun driveFiles(kind: String) = drive.visible.filter { it.props[DriveLayout.KEY_KIND] == kind }

    suspend fun exportTo(zip: Path) = fs.sink(zip).buffer().use { exporter().write(it) }

    companion object {
        fun entry(
            id: String,
            updated: Long = 10,
            date: String = "2026-10-31",
            body: String = id,
            deletedAt: Long? = null,
            legacyId: Int? = null
        ) = EntryEntity(id, date, "21:40", "<p>$body</p>", body, "good", "sunny", "망원동", false, 1, updated, deletedAt, legacyId, null, null)

        fun hashOf(content: String) = content.encodeToByteArray().toByteString().sha256().hex()

        fun photoRow(entryId: String, order: Int, content: String): MediaEntity {
            val hash = hashOf(content)
            val size = content.encodeToByteArray().size.toLong()
            return MediaEntity("$entryId-$order", entryId, order, "normal", "$hash.jpg", hash, 1, 1, size, null, 1)
        }
    }
}

/** 복원이 끝나거나 실패할 때까지 기다린다. */
internal suspend fun RestoreController.finished(): RestoreState = withContext(Dispatchers.Default) {
    withTimeout(10_000) { state.first { it is RestoreState.Done || it is RestoreState.Failed } }
}

/** 조건이 참이 될 때까지 기다린다. */
internal suspend fun until(check: suspend () -> Boolean) = withContext(Dispatchers.Default) {
    withTimeout(10_000) {
        while (!check()) kotlinx.coroutines.delay(10)
    }
}

/**
 * 사진을 [after]장 받은 뒤로는 네트워크가 끊긴 드라이브.
 * 사진은 동시에 받으니 잠그고 센다.
 */
internal class CutDrive(private val real: DriveApi, private val after: Int) : DriveApi by real {
    private val lock = Mutex()
    private var served = 0

    override suspend fun download(fileId: String, to: Path, fromByte: Long): Long {
        val allowed = lock.withLock { served++ < after }
        if (!allowed) throw DriveException(BackupFailure.OFFLINE)
        return real.download(fileId, to, fromByte)
    }
}

/**
 * 파일을 쓸 때 실패하는 기기.
 * [jam]이 참인 경로에서 저장 공간이 없는 것처럼 군다.
 */
internal class JammedFiles(base: FileSystem, var jam: (Path) -> Boolean = { false }) : ForwardingFileSystem(base) {
    override fun sink(file: Path, mustCreate: Boolean): Sink {
        if (jam(file)) throw IOException("저장 공간이 없다")
        return super.sink(file, mustCreate)
    }
}
