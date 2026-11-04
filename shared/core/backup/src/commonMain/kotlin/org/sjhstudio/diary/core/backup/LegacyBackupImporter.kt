package org.sjhstudio.diary.core.backup

import androidx.room3.withWriteTransaction
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.migration.LegacyDbReader
import org.sjhstudio.diary.core.migration.LegacyEntryMapper
import org.sjhstudio.diary.core.migration.LegacyNote
import org.sjhstudio.diary.core.migration.PhotoPaths
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.model.RestoreResult

/** 레거시 경로의 사진을 `media/`로 옮긴다. 찾지 못하면 null이다. iOS에서는 늘 null이다. */
fun interface LegacyPhotoCopier {
    fun copy(entryId: String, sortOrder: Int, fragment: String): MediaEntity?
}

/** 더할 일기 하나. [sameTimeAs]는 같은 날 같은 시각에 글이 다른 기기 일기다. */
data class LegacyItem(val entry: EntryEntity, val photos: List<String>, val sameTimeAs: String?)

data class LegacyPlan(val items: List<LegacyItem>, val unchanged: Int) {
    val preview: RestorePreview
        get() = RestorePreview(
            entriesAdded = items.size,
            entriesUpdated = 0,
            entriesKeptBoth = items.count { it.sameTimeAs != null },
            entriesUnchanged = unchanged,
            photosToDownload = 0,
            bytesToDownload = 0,
            photosNotInBackup = items.sumOf { it.photos.size },
            settingsChange = false,
            deviceBytesFree = Long.MAX_VALUE
        )
}

/**
 * 1.7.x가 드라이브에 올린 `note.db`를 찾아 합친다(contracts/merge-rules.md "이전 버전 백업").
 * 다른 기기의 파일이면 같은 번호가 다른 일기라서 `legacy_id`로는 견주지 않는다.
 */
@OptIn(ExperimentalUuidApi::class)
class LegacyBackupImporter(
    private val db: DiaryDatabase,
    private val drive: DriveApi,
    private val copier: LegacyPhotoCopier,
    private val fileSystem: FileSystem,
    private val workDir: Path,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val newId: () -> String = { Uuid.random().toString() }
) {
    /** 옛 설정에 남은 파일을 먼저 보고, 이름으로도 찾아 최근 것을 위로 둔다. */
    suspend fun find(knownFileId: String?): List<DriveFile> {
        val known =
            knownFileId?.let {
                try {
                    drive.get(it)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            }
        val named = drive.listAll(DriveQuery(name = FILE_NAME))
        return (listOfNotNull(known) + named).distinctBy { it.id }.sortedByDescending { it.createdAt }
    }

    suspend fun prepare(file: DriveFile): LegacyPlan {
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        val path = incoming / LOCAL_NAME
        drive.download(file.id, path)
        try {
            return plan(read(path))
        } finally {
            for (suffix in listOf("", "-wal", "-shm")) fileSystem.delete(incoming / "$LOCAL_NAME$suffix", mustExist = false)
        }
    }

    private fun read(path: Path): List<LegacyNote> = try {
        LegacyDbReader.open(path.toString()).use { reader ->
            // 일부가 깨져도 읽히는 만큼 살린다. 일기 표가 없으면 다른 파일이다.
            if (!reader.hasNoteTable) throw RestoreMismatch()
            buildList { reader.readAll { add(it) } }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: RestoreMismatch) {
        throw error
    } catch (_: Exception) {
        throw RestoreMismatch()
    }

    private suspend fun plan(notes: List<LegacyNote>): LegacyPlan {
        val local = db.backupDao().allEntries()
        val zone = timeZone()
        val mapper = LegacyEntryMapper(clock.now().toLocalDateTime(zone).date, zone, newId)
        val sameTime = local.groupBy { it.date to it.time }
        val seen = local.mapTo(HashSet()) { Triple(it.date, it.time, it.plainText) }
        val usedLegacyIds = local.mapNotNullTo(HashSet()) { it.legacyId }
        val items = ArrayList<LegacyItem>()
        var unchanged = 0
        for (note in notes) {
            val mapped = mapper.map(note).entry.asEntity()
            if (!seen.add(Triple(mapped.date, mapped.time, mapped.plainText))) {
                unchanged++
                continue
            }
            val legacyId = mapped.legacyId?.takeIf { usedLegacyIds.add(it) }
            items +=
                LegacyItem(
                    mapped.copy(legacyId = legacyId),
                    PhotoPaths.fragments(note.picture),
                    sameTime[mapped.date to mapped.time]?.first()?.id
                )
        }
        return LegacyPlan(items, unchanged)
    }

    suspend fun apply(plan: LegacyPlan): RestoreResult {
        val now = clock.now().toEpochMilliseconds()
        val rows =
            plan.items.flatMap { item ->
                item.photos.mapIndexed { order, fragment ->
                    copier.copy(item.entry.id, order, fragment)
                        ?: missing(item, order, fragment, now)
                }
            }
        val marks =
            plan.items.flatMap { item ->
                item.sameTimeAs?.let { other ->
                    listOf(
                        MergeMarkEntity(item.entry.id, KIND, other, SOURCE, now),
                        MergeMarkEntity(other, KIND, item.entry.id, SOURCE, now)
                    )
                }.orEmpty()
            }
        db.withWriteTransaction {
            db.entryDao().insertAll(plan.items.map { it.entry })
            db.mediaDao().insertAll(rows)
            db.backupDao().mark(marks)
        }
        val copied = rows.count { it.status == NORMAL }
        return RestoreResult(
            entriesAdded = plan.items.size,
            entriesUpdated = 0,
            entriesKeptBoth = plan.items.count { it.sameTimeAs != null },
            photosRestored = copied,
            photosMissing = rows.size - copied,
            photosPending = 0,
            fromLegacy = true
        )
    }

    private fun missing(item: LegacyItem, order: Int, fragment: String, now: Long) =
        MediaEntity(newId(), item.entry.id, order, MISSING, null, null, null, null, null, fragment, now)

    private companion object {
        const val FILE_NAME = "note.db"
        const val INCOMING = "incoming"
        const val LOCAL_NAME = "legacy-note.db"
        const val KIND = "legacy_same_time"
        const val SOURCE = "legacy_drive"
        const val NORMAL = "normal"
        const val MISSING = "missing"
    }
}
