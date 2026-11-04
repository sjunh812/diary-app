package org.sjhstudio.diary.core.backup

import androidx.room3.useReaderConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.platform.FreeSpace

/** 되돌릴 사본을 둘 자리가 없다. 바꾸기를 시작하지 않아서 기기의 기록은 그대로다. */
class NoRoomForRollback : Exception("no room for the rollback copy")

/**
 * 바꾸기와 7일 되돌리기(contracts/merge-rules.md). 바꾸기는 기기 일기를 비우는 유일한 길이라서
 * 사본을 뜨고 열어서 확인한 뒤에만 비운다. 7일이 지나기 전에는 사진 정리도 멈춘다.
 */
class RollbackManager(
    private val db: DiaryDatabase,
    private val applier: MergeApplier,
    private val store: BackupStore,
    private val freeSpace: FreeSpace,
    private val fileSystem: FileSystem,
    rootDir: Path,
    private val currentSettings: suspend () -> ManifestSettings,
    private val applySettings: suspend (ManifestSettings) -> Unit,
    private val clock: Clock = Clock.System
) {
    private val dir = rootDir / "rollback"
    private val copy = dir / "diary.db"
    private val settingsFile = dir / "settings.json"
    private val json = Json { ignoreUnknownKeys = true }

    val info: Flow<RollbackInfo?> = store.prefs.map { prefs -> prefs.rollbackUntil?.takeIf { it > clock.now() }?.let { info(it) } }

    private suspend fun info(until: Instant): RollbackInfo? {
        if (!fileSystem.exists(copy)) return null
        val replacedAt = (until - KEEP_FOR).toEpochMilliseconds()
        return RollbackInfo(
            until = until,
            entriesBefore = SnapshotReader.read(copy).count { it.entry.deletedAt == null },
            entriesAfter = db.backupDao().allEntries().count { it.deletedAt == null && it.updatedAt > replacedAt }
        )
    }

    /** 사본을 만들고 확인한 뒤 한 트랜잭션으로 백업 내용으로 바꾼다. */
    suspend fun replace(backup: List<MergeEntry>, source: String) {
        val needed = databaseBytes() * 2
        if (freeSpace.bytes() < needed) throw NoRoomForRollback()
        fileSystem.deleteRecursively(dir)
        fileSystem.createDirectories(dir)
        SnapshotWriter(db, fileSystem).copyInto(copy)
        val entries = db.backupDao().allEntries().size
        check(SnapshotReader.read(copy).size == entries && integrityOk()) { "rollback copy does not match" }
        fileSystem.write(settingsFile) { writeUtf8(json.encodeToString(ManifestSettings.serializer(), currentSettings())) }
        db.withWriteTransaction {
            clearAll()
            applier.apply(applier.plan(backup), source)
            store.update { it.copy(rollbackUntil = clock.now() + KEEP_FOR) }
        }
    }

    /** 바꾸기 전으로 돌아간다. 바꾼 뒤에 쓰거나 고친 일기는 합치기 규칙으로 함께 남긴다. */
    suspend fun rollBack(): Boolean {
        val until = store.current().rollbackUntil ?: return false
        if (!fileSystem.exists(copy)) return false
        check(integrityOk()) { "rollback copy is damaged" }
        val before = SnapshotReader.read(copy)
        val bases = readBases()
        val marks = readMarks()
        val saved = fileSystem.read(settingsFile) { readUtf8() }
        val replacedAt = (until - KEEP_FOR).toEpochMilliseconds()
        val dao = db.backupDao()
        val media = dao.allMedia().groupBy { it.entryId }
        val used = before.mapNotNullTo(HashSet()) { it.entry.legacyId }
        val newer =
            dao.allEntries().filter { it.updatedAt > replacedAt }.map { entry ->
                val legacyId = entry.legacyId?.takeUnless { it in used }
                MergeEntry(entry.copy(legacyId = legacyId), media[entry.id].orEmpty())
            }
        db.withWriteTransaction {
            clearAll()
            db.entryDao().insertAll(before.map { it.entry })
            db.mediaDao().insertAll(before.flatMap { it.media })
            dao.setBase(bases)
            dao.mark(marks)
            applier.apply(applier.plan(newer), SOURCE_ROLLBACK)
        }
        applySettings(json.decodeFromString(ManifestSettings.serializer(), saved))
        finish()
        return true
    }

    /** 앱을 열 때 부른다. 7일이 지났으면 사본을 지운다. */
    suspend fun expire() {
        val until = store.current().rollbackUntil ?: return
        if (clock.now() >= until) finish()
    }

    private suspend fun finish() {
        fileSystem.deleteRecursively(dir)
        store.update { it.copy(rollbackUntil = null) }
    }

    private suspend fun clearAll() {
        val dao = db.backupDao()
        dao.clearMedia()
        dao.clearEntries()
        dao.clearBases()
        dao.clearMarks()
    }

    private suspend fun databaseBytes(): Long = db.useReaderConnection { transactor ->
        val pages = transactor.usePrepared("PRAGMA page_count") { statement ->
            statement.step()
            statement.getLong(0)
        }
        val size = transactor.usePrepared("PRAGMA page_size") { statement ->
            statement.step()
            statement.getLong(0)
        }
        pages * size
    }

    private fun integrityOk(): Boolean = connection {
        it.prepare("PRAGMA integrity_check").use { statement -> statement.step() && statement.getText(0) == "ok" }
    }

    private fun readBases(): List<SyncBaseEntity> = connection {
        it.prepare("SELECT entry_id, base_updated_at FROM sync_base").use { statement ->
            buildList { while (statement.step()) add(SyncBaseEntity(statement.getText(0), statement.getLong(1))) }
        }
    }

    private fun readMarks(): List<MergeMarkEntity> = connection {
        it.prepare("SELECT entry_id, kind, pair_entry_id, source, created_at FROM merge_mark").use { statement ->
            buildList {
                while (statement.step()) {
                    val pair = if (statement.isNull(2)) null else statement.getText(2)
                    add(MergeMarkEntity(statement.getText(0), statement.getText(1), pair, statement.getText(3), statement.getLong(4)))
                }
            }
        }
    }

    private fun <T> connection(block: (androidx.sqlite.SQLiteConnection) -> T): T {
        val connection = BundledSQLiteDriver().open(copy.toString(), READ_ONLY)
        return try {
            block(connection)
        } finally {
            connection.close()
        }
    }

    private companion object {
        val KEEP_FOR = 7.days
        const val SOURCE_ROLLBACK = "rollback"

        // SQLITE_OPEN_READONLY
        const val READ_ONLY = 0x00000001
    }
}
