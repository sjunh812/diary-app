package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingData
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.TrashedEntry

// 기능 모듈이 부르는 저장소(스펙 002 contracts/data-access.md).
// 모든 읽기는 Flow, 쓰기는 suspend다.
// 휴지통에 있는 일기는 TrashRepository 말고는 어디서도 돌려주지 않는다.

interface EntryRepository {
    fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>>

    fun entry(id: String): Flow<EntryDetail?>

    /** 일기가 있는 달. */
    fun months(): Flow<Set<YearMonth>>

    /** 그 달 첫 일기의 목록 위치(최신순이라 그 달의 마지막 날 쪽). */
    suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int

    fun day(date: LocalDate): Flow<List<EntryRow>>

    /** 달력 칸: 마지막 일기의 기분과 편수. */
    fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>>

    fun search(filter: SearchFilter): Flow<PagingData<EntryRow>>

    suspend fun setFavorite(id: String, favorite: Boolean)

    /** 체크리스트 [index]번째 항목을 뒤집는다. 합치기 복원이 고친 쪽을 가르도록 고친 시각도 바꾼다. */
    suspend fun toggleCheck(id: String, index: Int)

    suspend fun moveToTrash(id: String)

    /** "두 가지로 남겼어요" 표시를 닫는다. 일기는 그대로다. */
    suspend fun clearKeptBoth(id: String)

    suspend fun restore(id: String)
}

interface DraftRepository {
    fun draft(key: DraftKey): Flow<Draft?>

    /** 장소를 고를 때 보일 최근 장소. */
    suspend fun recentPlaces(): List<String>

    /** 새 일기 임시 저장본 가운데 가장 최근 것. 없으면 null이다. */
    suspend fun latestNew(): Draft?

    /** 고치기를 열 때 임시 저장본이 없으면 일기에서 만든다. 일기가 없으면 null이다. */
    suspend fun openEdit(entryId: String): Draft?

    /** 글과 칩을 쓴다. 사진은 아래 함수들이 맡는다. 디바운스는 화면 모델이 한다. */
    suspend fun save(draft: Draft)

    /** 한도와 공간이 허락하는 만큼 더한다. 임시 저장본이 먼저 있어야 한다. */
    suspend fun addPhotos(key: DraftKey, picked: List<PickedImage>): AddPhotosResult

    suspend fun removePhoto(key: DraftKey, index: Int)

    /** [order]는 지금 사진 위치를 새 순서대로 나열한 것이다. */
    suspend fun reorderPhotos(key: DraftKey, order: List<Int>)

    /** 한 트랜잭션: 일기와 사진을 쓰고 임시 저장본을 지운다. 일기 ID를 돌려준다. 실패하면 임시 저장본이 남는다. */
    suspend fun commit(key: DraftKey): String

    suspend fun discard(key: DraftKey)
}

interface TrashRepository {
    /** 지운 날짜순, 남은 날 포함. */
    fun items(): Flow<List<TrashedEntry>>

    suspend fun restore(id: String)

    suspend fun deleteForever(ids: List<String>)

    suspend fun empty()

    /** 30일 지난 것을 지운다. 앱 시작 때 부른다. */
    suspend fun purgeExpired(now: Instant)
}

interface StatsRepository {
    fun moodCounts(period: StatsPeriod): Flow<MoodCounts>

    fun moodFlow(period: StatsPeriod): Flow<List<DayMood>>

    fun advanced(period: StatsPeriod): Flow<AdvancedStats>

    fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>>
}

interface MediaRepository {
    /** 일기와 임시 저장본 어디에서도 쓰지 않고 하루 넘게 지난 사진 파일을 지운다. */
    suspend fun cleanUnused(now: Instant)
}
