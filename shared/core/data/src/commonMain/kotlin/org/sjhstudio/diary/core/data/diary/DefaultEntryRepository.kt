package org.sjhstudio.diary.core.data.diary

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.onDay
import okio.Path
import org.sjhstudio.diary.core.database.DayMoodRow
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryPhoto
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter

private const val PAGE = 60

class DefaultEntryRepository(private val db: DiaryDatabase, private val mediaDir: Path, private val clock: Clock = Clock.System) :
    EntryRepository {
    private val entries get() = db.entryDao()
    private val media get() = db.mediaDao()
    private val marks get() = db.backupDao()

    // 표시는 일기 표 밖에 있어서 지울 때 목록을 직접 새로 읽힌다.
    private val allPages = InvalidatingPagingSourceFactory { entries.visibleEntries() }
    private val favoritePages = InvalidatingPagingSourceFactory { entries.favoriteEntries() }

    override fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> = // 자리 표시를 켜서 다른 달로 바로 건너뛸 수 있게 한다.
        Pager(
            PagingConfig(pageSize = PAGE, initialLoadSize = PAGE, prefetchDistance = PAGE / 2, enablePlaceholders = true),
            pagingSourceFactory = if (favoritesOnly) favoritePages else allPages
        ).flow.map { page ->
            val marked = marks.markedIds().toSet()
            page.map { it.toRow(media.forEntry(it.id), mediaDir).copy(keptBoth = it.id in marked) }
        }

    override fun entry(id: String): Flow<EntryDetail?> = combine(entries.observeVisible(id), marks.markedPair(id)) { entity, pair ->
        entity?.let {
            EntryDetail(
                entry = it.asModel(),
                photos =
                    media.forEntry(id).map { row ->
                        EntryPhoto(row.id, row.pathIn(mediaDir), row.width, row.height, pending = row.status == MediaStatus.PENDING.id)
                    },
                keptBothWith = pair
            )
        }
    }

    override suspend fun clearKeptBoth(id: String) {
        clearMarks(id)
        allPages.invalidate()
        favoritePages.invalidate()
    }

    /** 한쪽을 지우거나 표시를 닫으면 짝의 표시도 함께 지운다. */
    private suspend fun clearMarks(id: String) {
        marks.markedPair(id).first()?.let { marks.clearMark(it) }
        marks.clearMark(id)
    }

    override fun months(): Flow<Set<YearMonth>> = entries.months().map { rows -> rows.mapTo(HashSet()) { YearMonth.parse(it) } }

    override suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int =
        entries.countAfter(month.onDay(month.numberOfDays).toString(), favoritesOnly)

    override fun day(date: LocalDate): Flow<List<EntryRow>> = combine(entries.onDay(date.toString()), marks.marks()) { rows, marked ->
        val photos = media.forRows(rows)
        val ids = marked.mapTo(HashSet()) { it.entryId }
        rows.map { it.toRow(photos[it.id].orEmpty(), mediaDir).copy(keptBoth = it.id in ids) }
    }

    override fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>> =
        entries.dayMoods(month.onDay(1).toString(), month.onDay(month.numberOfDays).toString()).map(::toDayMoods)

    override fun search(filter: SearchFilter): Flow<List<EntryRow>> {
        val query = filter.query.trim()
        return entries.search(
            like = if (query.isEmpty()) "" else "%${escapeLike(query)}%",
            anyMood = filter.moods.isEmpty(),
            moods = filter.moods.map { it.id },
            anyWeather = filter.weathers.isEmpty(),
            weathers = filter.weathers.map { it.id },
            favoritesOnly = filter.favoritesOnly,
            hasPhoto = filter.hasPhoto,
            from = filter.period?.start?.toString(),
            to = filter.period?.end?.toString()
        ).map { rows ->
            val photos = media.forRows(rows)
            rows.map { it.toRow(photos[it.id].orEmpty(), mediaDir, query) }
        }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = entries.setFavorite(id, favorite, nowMillis())

    override suspend fun moveToTrash(id: String) {
        val now = nowMillis()
        entries.setDeletedAt(id, now, now)
        clearMarks(id)
    }

    override suspend fun restore(id: String) = entries.setDeletedAt(id, null, nowMillis())

    private fun nowMillis() = clock.now().toEpochMilliseconds()
}

/** 사용자가 쓴 `%`, `_`, `\`가 글자 그대로 찾아지게 한다. */
internal fun escapeLike(text: String): String = buildString(text.length) {
    for (c in text) {
        if (c == '\\' || c == '%' || c == '_') append('\\')
        append(c)
    }
}

/** 시간순 줄을 날짜로 묶는다. 뒤에 있는 줄이 그날 마지막 일기다. */
internal fun toDayMoods(rows: List<DayMoodRow>): Map<LocalDate, DayMood> = rows.groupBy { it.date }.entries.associate { (date, day) ->
    val parsed = LocalDate.parse(date)
    val previous = day.getOrNull(day.lastIndex - 1)
    parsed to DayMood(parsed, Mood.fromId(day.last().mood), day.size, previous?.mood?.let(Mood::fromId))
}
