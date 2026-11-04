package org.sjhstudio.diary.core.data.diary

import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.TrashedEntry

/** 휴지통에서 영구 삭제까지 두는 날(FR-035). */
private val KEEP = 30.days

class DefaultTrashRepository(private val db: DiaryDatabase, private val mediaDir: Path, private val clock: Clock = Clock.System) :
    TrashRepository {
    private val entries get() = db.entryDao()

    override fun items(): Flow<List<TrashedEntry>> = entries.trashed().map { rows ->
        val photos = db.mediaDao().forRows(rows)
        val now = clock.now()
        rows.map { row ->
            val deletedAt = Instant.fromEpochMilliseconds(requireNotNull(row.deletedAt))
            TrashedEntry(row.toRow(photos[row.id].orEmpty(), mediaDir), deletedAt, daysLeft(deletedAt, now))
        }
    }

    override suspend fun restore(id: String) = entries.setDeletedAt(id, null, clock.now().toEpochMilliseconds())

    override suspend fun deleteForever(ids: List<String>) = ids.chunked(CHUNK).forEach { entries.deleteTrashed(it) }

    override suspend fun empty() = entries.deleteAllTrashed()

    override suspend fun purgeExpired(now: Instant) {
        entries.purgeBefore((now - KEEP).toEpochMilliseconds())
    }

    private companion object {
        const val CHUNK = 500
    }
}

/** 남은 날을 올림해서 센다. 오늘 지웠으면 30일, 마지막 날은 1일이다. */
internal fun daysLeft(deletedAt: Instant, now: Instant): Int {
    val left = (deletedAt + KEEP - now) / 1.days
    return ceil(left).toInt().coerceIn(1, KEEP.inWholeDays.toInt())
}
