package org.sjhstudio.diary.core.data.diary

import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.TRASH_KEEP_DAYS
import org.sjhstudio.diary.core.model.TrashedEntry

private val KEEP = TRASH_KEEP_DAYS.days

class DefaultTrashRepository(
    private val db: DiaryDatabase,
    private val mediaDir: Path,
    private val clock: Clock = Clock.System,
    private val photoRefreshEvery: Duration = PHOTO_REFRESH_EVERY
) : TrashRepository {
    private val entries get() = db.entryDao()

    override fun items(): Flow<List<TrashedEntry>> = combine(
        entries.trashed(),
        db.mediaDao().photoStatusChanges(photoRefreshEvery).onStart { emit(Unit) }
    ) { rows, _ ->
        val photos = db.mediaDao().forRows(rows.map { it.id })
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
