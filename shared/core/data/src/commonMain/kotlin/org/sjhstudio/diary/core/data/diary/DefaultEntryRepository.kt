package org.sjhstudio.diary.core.data.diary

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.onDay
import okio.Path
import org.sjhstudio.diary.core.database.DayMoodRow
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.EntryListRow
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryPhoto
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.richtext.toPlainText

private const val PAGE = 60

/**
 * 읽은 곳에서 이만큼 벗어나면 사이 쪽을 건너뛰고 그 자리부터 읽음
 * Paging 문서가 권하는 쪽 크기의 몇 배
 * 평소 스크롤은 이 안이라 쪽을 차례로 읽음
 */
private const val JUMP_ROWS = PAGE * 3

class DefaultEntryRepository(
    private val db: DiaryDatabase,
    private val mediaDir: Path,
    private val clock: Clock = Clock.System,
    private val photoRefreshEvery: Duration = PHOTO_REFRESH_EVERY
) : EntryRepository {
    private val entries get() = db.entryDao()
    private val media get() = db.mediaDao()
    private val marks get() = db.backupDao()

    internal val pageConfig =
        PagingConfig(
            pageSize = PAGE,
            initialLoadSize = PAGE,
            prefetchDistance = PAGE / 2,
            enablePlaceholders = true,
            jumpThreshold = JUMP_ROWS
        )

    // 표시는 일기 표 밖에 있어 지울 때 목록을 직접 다시 읽힘
    private val allPages = InvalidatingPagingSourceFactory { EntryRowPagingSource(entries.visibleEntries()) { pageRows(it) } }
    private val favoritePages = InvalidatingPagingSourceFactory { EntryRowPagingSource(entries.favoriteEntries()) { pageRows(it) } }

    override fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> = pages(if (favoritesOnly) favoritePages else allPages)

    /**
     * 자리 표시를 켜 다른 달로 바로 건너뛸 수 있게 함
     * 일기 표만 살피는 쪽은 받은 사진을 모르니, 사진 상태가 바뀌면 보던 쪽을 다시 읽음
     */
    private fun pages(factory: InvalidatingPagingSourceFactory<Int, EntryRow>): Flow<PagingData<EntryRow>> = channelFlow {
        launch { media.photoStatusChanges(photoRefreshEvery).collect { factory.invalidate() } }
        Pager(pageConfig, pagingSourceFactory = factory).flow.collect { send(it) }
    }

    /**
     * 쪽 하나를 한 번에 읽음
     * 줄마다 사진을 따로 읽으면 첫 화면이 일기 수만큼 늦어짐
     */
    private suspend fun pageRows(page: List<EntryListRow>, query: String = ""): List<EntryRow> {
        val marked = marks.markedIds().toSet()
        val photos = media.forRows(page.map { it.id })
        return page.map { it.toRow(photos[it.id].orEmpty(), mediaDir, query).copy(keptBoth = it.id in marked) }
    }

    override fun entry(id: String): Flow<EntryDetail?> = combine(
        entries.observeVisible(id),
        // 다른 일기의 사진을 받을 때마다 사진 줄을 다시 읽으니 같은 값은 내보내지 않음
        media.observeForEntry(id).distinctUntilChanged(),
        marks.markedPair(id)
    ) { entity, rows, pair ->
        entity?.let {
            EntryDetail(
                entry = it.asModel(),
                photos =
                    rows.map { row ->
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

    /** 한쪽을 지우거나 표시를 닫으면 짝의 표시도 함께 지움 */
    private suspend fun clearMarks(id: String) {
        marks.markedPair(id).first()?.let { marks.clearMark(it) }
        marks.clearMark(id)
    }

    override fun months(): Flow<Set<YearMonth>> = entries.months().map { rows -> rows.mapTo(HashSet()) { YearMonth.parse(it) } }

    override fun favoriteMonths(): Flow<Set<YearMonth>> =
        entries.favoriteMonths().map { rows -> rows.mapTo(HashSet()) { YearMonth.parse(it) } }

    override suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int =
        entries.countAfter(month.onDay(month.numberOfDays).toString(), favoritesOnly)

    override fun day(date: LocalDate): Flow<List<EntryRow>> = combine(
        entries.onDay(date.toString()),
        marks.marks(),
        media.photoStatusChanges(photoRefreshEvery).onStart { emit(Unit) }
    ) { rows, marked, _ ->
        val photos = media.forRows(rows.map { it.id })
        val ids = marked.mapTo(HashSet()) { it.entryId }
        rows.map { it.toRow(photos[it.id].orEmpty(), mediaDir).copy(keptBoth = it.id in ids) }
    }

    override fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>> =
        entries.dayMoods(month.onDay(1).toString(), month.onDay(month.numberOfDays).toString()).map(::toDayMoods)

    override fun search(filter: SearchFilter): Flow<PagingData<EntryRow>> = pages(InvalidatingPagingSourceFactory { searchPages(filter) })

    /**
     * 찾기 결과 쪽
     * 찾은 글자가 미리보기에 보이게 줄마다 글자를 넘김
     */
    internal fun searchPages(filter: SearchFilter): PagingSource<Int, EntryRow> {
        val query = filter.query.trim()
        val source =
            entries.search(
                like = if (query.isEmpty()) "" else "%${escapeLike(query)}%",
                anyMood = filter.moods.isEmpty(),
                moods = filter.moods.map { it.id },
                anyWeather = filter.weathers.isEmpty(),
                weathers = filter.weathers.map { it.id },
                favoritesOnly = filter.favoritesOnly,
                hasPhoto = filter.hasPhoto,
                from = filter.period?.start?.toString(),
                to = filter.period?.end?.toString()
            )
        return EntryRowPagingSource(source) { pageRows(it, query) }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = entries.setFavorite(id, favorite, nowMillis())

    override suspend fun toggleCheck(id: String, index: Int) {
        entries.edit(id) { entry ->
            Checklist.toggleInHtml(entry.bodyHtml, index)?.let { body ->
                entry.copy(bodyHtml = body, plainText = toPlainText(body), updatedAt = nowMillis())
            }
        }
        // 고치다 남겨 둔 글을 저장해도 지금 누른 체크가 사라지지 않게
        db.draftDao().markEntryChanged(id)
    }

    override suspend fun moveToTrash(id: String) {
        val now = nowMillis()
        entries.setDeletedAt(id, now, now)
        clearMarks(id)
    }

    override suspend fun restore(id: String) = entries.setDeletedAt(id, null, nowMillis())

    private fun nowMillis() = clock.now().toEpochMilliseconds()
}

/** 사용자가 쓴 `%`, `_`, `\`가 글자 그대로 찾아지게 함 */
internal fun escapeLike(text: String): String = buildString(text.length) {
    for (c in text) {
        if (c == '\\' || c == '%' || c == '_') append('\\')
        append(c)
    }
}

/**
 * 시간순 줄을 날짜로 묶음
 * 뒤에 있는 줄이 그날 마지막 일기
 */
internal fun toDayMoods(rows: List<DayMoodRow>): Map<LocalDate, DayMood> = rows.groupBy { it.date }.entries.associate { (date, day) ->
    val parsed = LocalDate.parse(date)
    val previous = day.getOrNull(day.lastIndex - 1)
    parsed to
        DayMood(
            date = parsed,
            mood = Mood.fromId(day.last().mood),
            count = day.size,
            previousMood = previous?.mood?.let(Mood::fromId),
            moods = day.mapNotNull { Mood.fromId(it.mood) }
        )
}
