package org.sjhstudio.diary.core.data.diary

import androidx.room3.withWriteTransaction
import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    private val drafts get() = db.draftDao()

    // 설정 화면이 바로 모으는 흐름이라 읽지 못하면 개수를 비워 둠. 예외가 화면까지 가면 앱이 꺼짐
    override fun count(): Flow<Int> = flow { emitAll(entries.trashedCount()) }.catch { }

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

    // 완전히 지운 일기의 글과 사진이 고치던 임시 저장본에 남으면 안 됨(헌법 II)
    // 임시 저장본이 사라지면 그 사진 파일도 다음 사진 정리에서 함께 지워짐
    override suspend fun deleteForever(ids: List<String>) = ids.chunked(CHUNK).forEach { chunk ->
        db.withWriteTransaction {
            drafts.deleteTrashed(chunk)
            entries.deleteTrashed(chunk)
        }
    }

    override suspend fun empty() = db.withWriteTransaction {
        drafts.deleteAllTrashed()
        entries.deleteAllTrashed()
    }

    override suspend fun purgeExpired(now: Instant) {
        val before = (now - KEEP).toEpochMilliseconds()
        db.withWriteTransaction {
            drafts.purgeBefore(before)
            entries.purgeBefore(before)
        }
    }

    private companion object {
        const val CHUNK = 500
    }
}

/**
 * 남은 날을 올림해서 셈
 * 오늘 지웠으면 30일, 마지막 날은 1일
 */
internal fun daysLeft(deletedAt: Instant, now: Instant): Int {
    val left = (deletedAt + KEEP - now) / 1.days
    return ceil(left).toInt().coerceIn(1, KEEP.inWholeDays.toInt())
}
