package org.sjhstudio.diary.core.data.diary

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
import org.sjhstudio.diary.core.model.MonthCount
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

    private val allPages = InvalidatingPagingSourceFactory { EntryRowPagingSource(entries.visibleEntries()) { pageRows(it) } }
    private val favoritePages = InvalidatingPagingSourceFactory { EntryRowPagingSource(entries.favoriteEntries()) { pageRows(it) } }

    override fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> = pages(if (favoritesOnly) favoritePages else allPages)

    /**
     * 자리 표시를 켜 다른 달로 바로 건너뛸 수 있게 함
     * 일기 표만 살피는 쪽은 받은 사진과 두 판 표시를 모르니, 둘이 바뀌면 보던 쪽을 다시 읽음
     */
    private fun pages(factory: InvalidatingPagingSourceFactory<Int, EntryRow>): Flow<PagingData<EntryRow>> = channelFlow {
        // 사진 상태와 두 판 표시의 기준을 잡은 뒤 목록을 시작. 그 사이의 변화를 놓치지 않음
        awaitAll(
            watchFromBaseline({ media.photoStatusChanges(photoRefreshEvery, it) }) { factory.invalidate() },
            watchFromBaseline({ marks.shownMarks().map { rows -> rows.toHashSet() }.changesSinceFirst(it) }) { factory.invalidate() }
        )
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
    }.rejoiningAfterFirst()

    /** 표시를 닫으면 짝의 표시도 함께 지움. 짝이 이미 다른 일기와 짝이면 그 표시는 둠 */
    override suspend fun clearKeptBoth(id: String) = marks.clearPair(id)

    // 쓰는 곳(홈 쓰기 버튼, 연·월 고르기, 달력 끝 달)에 다시 시도가 없어 처음부터 다시 붙음
    override fun months(): Flow<Set<YearMonth>> = entries.months().rejoining().map { rows -> rows.mapTo(HashSet()) { YearMonth.parse(it) } }

    override fun monthCounts(favoritesOnly: Boolean): Flow<List<MonthCount>> =
        entries.monthCounts(favoritesOnly).rejoining().map { rows -> rows.map { MonthCount(YearMonth.parse(it.month), it.count) } }

    override suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int =
        entries.countAfter(month.onDay(month.numberOfDays).toString(), favoritesOnly)

    override fun day(date: LocalDate): Flow<List<EntryRow>> = combine(
        entries.onDay(date.toString()),
        shownMarks(),
        media.photoStatusChanges(photoRefreshEvery).onStart { emit(Unit) }
    ) { rows, ids, _ ->
        val photos = media.forRows(rows.map { it.id })
        rows.map { it.toRow(photos[it.id].orEmpty(), mediaDir).copy(keptBoth = it.id in ids) }
    }.rejoiningAfterFirst()

    /** 보일 두 판 표시. 같은 값은 다시 내지 않아 일기를 저장할 때마다 다시 읽지 않음 */
    private fun shownMarks(): Flow<Set<String>> = marks.shownMarks().map { it.toHashSet() }.distinctUntilChanged()

    override fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>> =
        entries.dayMoods(month.onDay(1).toString(), month.onDay(month.numberOfDays).toString()).rejoiningAfterFirst().map(::toDayMoods)

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

    override suspend fun toggleFavorite(id: String) = entries.toggleFavorite(id, nowMillis())

    override suspend fun toggleCheck(id: String, index: Int) {
        entries.edit(id) { entry ->
            Checklist.toggleInHtml(entry.bodyHtml, index)?.let { body ->
                entry.copy(bodyHtml = body, plainText = toPlainText(body), updatedAt = nowMillis())
            }
        }
        // 고치다 남겨 둔 글을 저장해도 지금 누른 체크가 사라지지 않게
        db.draftDao().markEntryChanged(id)
    }

    // 짝 표시는 남김. 짝이 휴지통에 있으면 표시가 숨고, 되돌리면 다시 보임
    override suspend fun moveToTrash(id: String) {
        val now = nowMillis()
        entries.setDeletedAt(id, now, now)
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
