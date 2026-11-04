package org.sjhstudio.diary.core.data.diary

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
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
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter

private const val PAGE = 60

class DefaultEntryRepository(private val db: DiaryDatabase, private val mediaDir: Path, private val clock: Clock = Clock.System) :
    EntryRepository {
    private val entries get() = db.entryDao()
    private val media get() = db.mediaDao()

    override fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> =
        Pager(PagingConfig(pageSize = PAGE, initialLoadSize = PAGE, prefetchDistance = PAGE / 2)) {
            if (favoritesOnly) entries.favoriteEntries() else entries.visibleEntries()
        }.flow.map { page -> page.map { it.toRow(media.forEntry(it.id), mediaDir) } }

    override fun entry(id: String): Flow<EntryDetail?> = entries.observeVisible(id).map { entity ->
        entity?.let {
            EntryDetail(
                entry = it.asModel(),
                photos = media.forEntry(id).map { row -> EntryPhoto(row.id, row.pathIn(mediaDir), row.width, row.height) }
            )
        }
    }

    override fun day(date: LocalDate): Flow<List<EntryRow>> = entries.onDay(date.toString()).map { rows ->
        val photos = media.forRows(rows)
        rows.map { it.toRow(photos[it.id].orEmpty(), mediaDir) }
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
    parsed to DayMood(parsed, Mood.fromId(day.last().mood), day.size)
}
