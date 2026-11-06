package org.sjhstudio.diary.core.backup

import androidx.room3.useReaderConnection
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.platform.DeviceName

/** 드라이브 폴더와 같은 내용을 zip 하나로 내보낸다(contracts/backup-format.md "묶음 파일", FR-031). */
class BundleExporter(
    private val db: DiaryDatabase,
    private val store: BackupStore,
    private val deviceName: DeviceName,
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val workDir: Path,
    private val app: BackupAppInfo,
    private val readme: suspend () -> String,
    private val settings: suspend () -> ManifestSettings,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    suspend fun estimate(): BundleEstimate {
        val dao = db.backupDao()
        val photos = dao.localPhotos().filter { it.fileName != null }
        return BundleEstimate(
            entries = dao.entrySizes().visibleEntries,
            photos = photos.size,
            bytes = photos.sumOf { it.byteSize ?: 0 } + databaseBytes()
        )
    }

    /** 스냅샷은 빈 쪽 없이 만들어지므로 빈 쪽을 뺀 크기가 파일 안의 DB 크기와 가깝다. */
    private suspend fun databaseBytes(): Long = db.useReaderConnection { connection ->
        suspend fun pragma(name: String): Long = connection.usePrepared("PRAGMA $name") { statement ->
            statement.step()
            statement.getLong(0)
        }
        (pragma("page_count") - pragma("freelist_count")) * pragma("page_size")
    }

    /** 글을 먼저 담고 사진을 잇는다. 쓰다 멈추면 저장한 곳의 파일은 쓸 수 없고 기기의 기록은 그대로다. */
    suspend fun write(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val dir = workDir / OUTGOING
        fileSystem.deleteRecursively(dir)
        fileSystem.createDirectories(dir)
        try {
            val snapshot = SnapshotWriter(db, fileSystem).write(dir / SNAPSHOT)
            val dao = db.backupDao()
            val local = dao.localPhotos().filter { row -> row.fileName?.let { fileSystem.exists(mediaDir / it) } == true }
            val inBundle = local.mapTo(HashSet()) { it.sha256 }
            val manifest =
                manifestOf(
                    now = clock.now(),
                    timeZone = timeZone(),
                    app = app,
                    deviceId = store.deviceId(),
                    deviceName = deviceName.current(),
                    snapshot = snapshot,
                    local = local,
                    pending = dao.pendingFiles(),
                    inBackup = inBundle,
                    settings = settings()
                )
            fileSystem.write(dir / README) { writeUtf8(readme()) }
            fileSystem.write(dir / MANIFEST) { writeUtf8(ManifestCodec.encode(manifest)) }
            val items =
                listOf(BundleItem(README, dir / README), BundleItem(MANIFEST, dir / MANIFEST), BundleItem(SNAPSHOT, snapshot.path)) +
                    local.map { row -> row.fileName!!.let { BundleItem("$MEDIA/$it", mediaDir / it) } }
            BundleWriter(fileSystem).write(sink, items) { done -> onProgress(done, items.size) }
            // 파일로만 주고받아도 다음 합치기가 한쪽만 고쳤는지 알아볼 수 있게 드라이브 백업처럼 적어 둔다.
            db.rememberBases(snapshot.path)
        } finally {
            fileSystem.deleteRecursively(dir)
        }
    }

    companion object {
        const val README = "README.txt"
        const val MANIFEST = "manifest.json"
        const val SNAPSHOT = "snapshot.db"
        const val MEDIA = "media"
        const val LEGACY_DB = "legacy-snapshot/note.db"
        private const val OUTGOING = "bundle"
    }
}
