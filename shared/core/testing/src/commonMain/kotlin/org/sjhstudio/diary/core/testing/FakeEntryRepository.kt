package org.sjhstudio.diary.core.testing

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.SearchFilter

private const val NOON = 12

/** 화면 시험에 쓰는 일기 한 줄. */
fun fakeEntryRow(id: String, date: LocalDate, preview: String = "글 $id", isFavorite: Boolean = false) = EntryRow(
    id = id,
    date = date,
    time = LocalTime(NOON, 0),
    preview = preview,
    mood = null,
    weather = null,
    placeName = null,
    isFavorite = isFavorite,
    photos = emptyList()
)

/**
 * 화면 시험용.
 * [moods]에 넣은 날에 일기가 있는 것으로 보인다.
 * [rows]는 홈과 그날 목록에 나오는 줄이다.
 * 기본은 일기가 하나도 없다.
 */
class FakeEntryRepository(private val moods: Map<LocalDate, DayMood> = emptyMap(), private val rows: List<EntryRow> = emptyList()) :
    EntryRepository {
    override fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> =
        flowOf(if (favoritesOnly || rows.isEmpty()) PagingData.empty() else PagingData.from(rows))

    override fun entry(id: String): Flow<EntryDetail?> = flowOf(null)

    override fun months(): Flow<Set<YearMonth>> = flowOf(moods.keys.mapTo(HashSet()) { it.yearMonth })

    override suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int = 0

    override fun day(date: LocalDate): Flow<List<EntryRow>> = flowOf(rows.filter { it.date == date })

    override fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>> = flowOf(moods.filterKeys { it.yearMonth == month })

    override fun search(filter: SearchFilter): Flow<PagingData<EntryRow>> = flowOf(PagingData.empty())

    override suspend fun setFavorite(id: String, favorite: Boolean) = Unit

    override suspend fun toggleCheck(id: String, index: Int) = Unit

    override suspend fun clearKeptBoth(id: String) = Unit

    override suspend fun moveToTrash(id: String) = Unit

    override suspend fun restore(id: String) = Unit
}
