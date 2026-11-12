package org.sjhstudio.diary.core.backup

import androidx.room3.useReaderConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.IOException
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.ENTRIES_FINGERPRINT
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.SyncBaseEntity
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.platform.FreeSpace

/**
 * 되돌릴 사본을 둘 자리가 없음
 * 바꾸기를 시작하지 않아 기기의 기록은 그대로
 */
class NoRoomForRollback : Exception("no room for the rollback copy")

/**
 * 바꾸기와 7일 되돌리기(contracts/merge-rules.md)
 * 바꾸기는 기기 일기를 비우는 유일한 길이라
 * 사본을 뜨고 열어서 확인한 뒤에만 비움
 * 7일이 지나기 전에는 사진 정리도 멈춤
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
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 사본은 `rollback/<되돌릴 수 있는 마지막 시각>/`에 둠
     * 저장해 둔 그 시각이 지금 쓸 사본을 가리킴
     */
    private fun slot(until: Instant) = dir / until.toEpochMilliseconds().toString()

    private fun copyIn(slot: Path) = slot / "diary.db"

    private fun settingsIn(slot: Path) = slot / "settings.json"

    /** 바꾸기가 넣은 일기 ID. 되돌릴 때 이 밖의 일기는 모두 남김 */
    private fun insertedIn(slot: Path) = slot / "inserted.json"

    val info: Flow<RollbackInfo?> = store.prefs.map { prefs -> prefs.rollbackUntil?.takeIf { it > clock.now() }?.let { info(it) } }

    private suspend fun info(until: Instant): RollbackInfo? {
        val copy = copyIn(slot(until))
        if (!fileSystem.exists(copy)) return null
        val replacedAt = (until - KEEP_FOR).toEpochMilliseconds()
        return try {
            RollbackInfo(
                until = until,
                entriesBefore = countIn(copy, "SELECT COUNT(*) FROM entry WHERE deleted_at IS NULL"),
                entriesAfter = db.backupDao().visibleEntriesUpdatedAfter(replacedAt)
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 사본이 깨졌으면 되돌릴 수 없으니 안내를 숨김
            // 화면 흐름을 죽이지 않음
            null
        }
    }

    /**
     * 사본을 새 자리에 만들고 확인한 뒤 한 트랜잭션으로 백업 내용으로 바꿈
     * 끝나기 전에는 앞선 사본을 지우지 않음
     * 실패해도 앞선 바꾸기는 되돌릴 수 있고, 일기 사본도 남기지 않음
     */
    suspend fun replace(backup: List<MergeEntry>, source: String) {
        val needed = databaseBytes() * 2
        if (freeSpace.bytes() < needed) throw NoRoomForRollback()
        val until = clock.now() + KEEP_FOR
        val slot = slot(until)
        val copy = copyIn(slot)
        var replaced = false
        try {
            fileSystem.createDirectories(slot)
            SnapshotWriter(db, fileSystem).copyInto(copy)
            check(integrityOk(copy)) { "rollback copy is damaged" }
            val copied = textIn(copy, ENTRIES_FINGERPRINT)
            fileSystem.write(settingsIn(slot)) { writeUtf8(json.encodeToString(ManifestSettings.serializer(), currentSettings())) }
            fileSystem.write(insertedIn(slot)) { writeUtf8(json.encodeToString(IDS, backup.map { it.entry.id })) }
            db.withWriteTransaction {
                // 사본을 뜬 뒤에 저장한 일기는 사본에 없음. 비우기 직전에 다시 견줘 다르면 멈춤
                check(db.backupDao().entriesFingerprint() == copied) { "rollback copy does not match" }
                clearAll()
                applier.apply(applier.plan(backup), source)
                db.draftDao().markAllEntriesChanged()
                store.update { it.copy(rollbackUntil = until) }
            }
            replaced = true
        } finally {
            if (!replaced) fileSystem.deleteRecursively(slot)
        }
        clearOlderCopies(slot)
    }

    /**
     * 바꾸기는 이미 끝남
     * 지우지 못한 사본은 7일 뒤에 함께 지워짐
     */
    private fun clearOlderCopies(keep: Path) {
        try {
            fileSystem.list(dir).filter { it != keep }.forEach { fileSystem.deleteRecursively(it) }
        } catch (_: IOException) {
            return
        }
    }

    /**
     * 바꾸기 전으로 되돌림
     * 바꾼 뒤에 쓰거나 고친 일기는 합치기 규칙으로 함께 남김
     */
    suspend fun rollBack(): Boolean {
        val until = store.current().rollbackUntil ?: return false
        val copy = copyIn(slot(until))
        if (!fileSystem.exists(copy)) return false
        check(integrityOk(copy)) { "rollback copy is damaged" }
        val before = SnapshotReader.read(copy)
        val bases = readBases(copy)
        val marks = readMarks(copy)
        val saved = fileSystem.read(settingsIn(slot(until))) { readUtf8() }
        val replacedAt = (until - KEEP_FOR).toEpochMilliseconds()
        val dao = db.backupDao()
        val media = dao.allMedia().groupBy { it.entryId }
        val used = before.mapNotNullTo(HashSet()) { it.entry.legacyId }
        // 바꾸기가 넣은 일기는 기준값이 `updated_at`과 같음
        // 시계가 뒤로 간 뒤에 쓰거나 고친 일기는 시각으로는 가를 수 없어 기준값으로도 봄
        val synced = dao.bases().associate { it.entryId to it.baseUpdatedAt }
        // 바꾼 뒤에 합쳐 온 일기는 시각도 기준값도 바꾸기가 넣은 일기와 같아 보임. 적어 둔 ID로 가름
        val inserted = insertedIds(slot(until))
        val newer =
            dao.allEntries().filter { it.id !in inserted || it.updatedAt > replacedAt || synced[it.id] != it.updatedAt }.map { entry ->
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
            db.draftDao().markAllEntriesChanged()
        }
        applySettings(json.decodeFromString(ManifestSettings.serializer(), saved))
        finish()
        return true
    }

    /**
     * 앱을 열 때 호출
     * 7일이 지났으면 사본 삭제
     */
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

    /** 적어 둔 목록이 없으면 모두 남길 일기로 봄 */
    private fun insertedIds(slot: Path): Set<String> {
        val file = insertedIn(slot)
        if (!fileSystem.exists(file)) return emptySet()
        return json.decodeFromString(IDS, fileSystem.read(file) { readUtf8() }).toSet()
    }

    private fun textIn(copy: Path, sql: String): String = connection(copy) {
        it.prepare(sql).use { statement ->
            statement.step()
            statement.getText(0)
        }
    }

    private fun countIn(copy: Path, sql: String): Int = connection(copy) {
        it.prepare(sql).use { statement ->
            statement.step()
            statement.getInt(0)
        }
    }

    private fun integrityOk(copy: Path): Boolean = connection(copy) {
        it.prepare("PRAGMA integrity_check").use { statement -> statement.step() && statement.getText(0) == "ok" }
    }

    private fun readBases(copy: Path): List<SyncBaseEntity> = connection(copy) {
        it.prepare("SELECT entry_id, base_updated_at FROM sync_base").use { statement ->
            buildList { while (statement.step()) add(SyncBaseEntity(statement.getText(0), statement.getLong(1))) }
        }
    }

    private fun readMarks(copy: Path): List<MergeMarkEntity> = connection(copy) {
        it.prepare("SELECT entry_id, kind, pair_entry_id, source, created_at FROM merge_mark").use { statement ->
            buildList {
                while (statement.step()) {
                    val pair = if (statement.isNull(2)) null else statement.getText(2)
                    add(MergeMarkEntity(statement.getText(0), statement.getText(1), pair, statement.getText(3), statement.getLong(4)))
                }
            }
        }
    }

    private fun <T> connection(copy: Path, block: (androidx.sqlite.SQLiteConnection) -> T): T {
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
        val IDS = ListSerializer(String.serializer())

        // SQLITE_OPEN_READONLY
        const val READ_ONLY = 0x00000001
    }
}
