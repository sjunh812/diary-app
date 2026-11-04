package org.sjhstudio.diary.core.testing

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.SearchFilter

/** 화면 시험용. [moods]에 넣은 날에 일기가 있는 것으로 보인다. 기본은 일기가 하나도 없다. */
class FakeEntryRepository(private val moods: Map<LocalDate, DayMood> = emptyMap()) : EntryRepository {
    override fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> = flowOf(PagingData.empty())

    override fun entry(id: String): Flow<EntryDetail?> = flowOf(null)

    override fun months(): Flow<Set<YearMonth>> = flowOf(moods.keys.mapTo(HashSet()) { it.yearMonth })

    override suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int = 0

    override fun day(date: LocalDate): Flow<List<EntryRow>> = flowOf(emptyList())

    override fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>> = flowOf(moods.filterKeys { it.yearMonth == month })

    override fun search(filter: SearchFilter): Flow<List<EntryRow>> = flowOf(emptyList())

    override suspend fun setFavorite(id: String, favorite: Boolean) = Unit

    override suspend fun clearKeptBoth(id: String) = Unit

    override suspend fun moveToTrash(id: String) = Unit

    override suspend fun restore(id: String) = Unit
}
