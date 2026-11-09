package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingData
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.DraftPhoto
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

    /** 즐겨찾기가 있는 달. */
    fun favoriteMonths(): Flow<Set<YearMonth>> = months()

    /** 그 달 첫 일기의 목록 위치(최신순이라 그 달의 마지막 날 쪽). */
    suspend fun positionOf(month: YearMonth, favoritesOnly: Boolean): Int

    fun day(date: LocalDate): Flow<List<EntryRow>>

    /** 달력 칸: 마지막 일기의 기분과 편수. */
    fun month(month: YearMonth): Flow<Map<LocalDate, DayMood>>

    fun search(filter: SearchFilter): Flow<PagingData<EntryRow>>

    suspend fun setFavorite(id: String, favorite: Boolean)

    /**
     * 체크리스트 [index]번째 항목을 뒤집는다.
     * 합치기 복원이 고친 쪽을 가르도록 고친 시각도 바꾼다.
     */
    suspend fun toggleCheck(id: String, index: Int)

    suspend fun moveToTrash(id: String)

    /**
     * "두 가지로 남겼어요" 표시를 닫는다.
     * 일기는 그대로다.
     */
    suspend fun clearKeptBoth(id: String)

    suspend fun restore(id: String)
}

/**
 * 수정 화면을 열어 받은 임시 저장본.
 * [resumed]면 전에 남겨 둔 것을 이어 연 것이고, 아니면 일기에서 새로 만든 것이다.
 */
data class OpenedEdit(
    val draft: Draft,
    val resumed: Boolean,
    /** 남겨 둔 뒤에 복원이 일기를 바꿨다. 저장하면 두 가지로 남는다. */
    val entryChanged: Boolean = false
) {
    override fun toString(): String = "OpenedEdit"
}

// 임시 저장본 하나를 다루는 일(글, 사진 넣기·빼기·되돌리기·순서, 저장)이 한곳에 모여 있어야 순서를 지킬 수 있다(inOrder).
@Suppress("TooManyFunctions")
interface DraftRepository {
    fun draft(key: DraftKey): Flow<Draft?>

    /** 장소를 고를 때 보일 최근 장소. */
    suspend fun recentPlaces(): List<String>

    /**
     * 새 일기 임시 저장본 가운데 가장 최근 것.
     * 없으면 null이다.
     */
    suspend fun latestNew(): Draft?

    /**
     * 수정 화면을 열 때 임시 저장본이 없으면 일기에서 만든다.
     * 일기가 없으면 null이다.
     */
    suspend fun openEdit(entryId: String): OpenedEdit?

    /**
     * 수정용 임시 저장본이 일기와 같은지.
     * 앱이 다시 떠도 알 수 있어야 손대지 않은 글이 저장본으로 남지 않는다.
     */
    suspend fun matchesEntry(key: DraftKey.Edit): Boolean

    /**
     * 글과 칩을 쓴다.
     * 사진은 아래 함수들이 맡는다.
     * 디바운스는 화면 모델이 한다.
     */
    suspend fun save(draft: Draft)

    /**
     * 한도와 공간이 허락하는 만큼 더한다.
     * 임시 저장본이 없으면 모두 못 더한 것으로 센다.
     */
    suspend fun addPhotos(key: DraftKey, picked: List<PickedImage>): AddPhotosResult

    /**
     * [index]번째 사진을 빼고 뺀 사진을 돌려준다.
     * 사진 파일은 지우지 않아 [restorePhoto]로 되돌릴 수 있다. 없는 자리면 null이다.
     */
    suspend fun removePhoto(key: DraftKey, index: Int): DraftPhoto?

    /**
     * 뺀 사진을 [index] 자리에 다시 넣는다(되돌리기).
     * 그 사이 한도만큼 채웠거나 임시 저장본이 없어졌으면 넣지 않고 false를 돌려준다.
     */
    suspend fun restorePhoto(key: DraftKey, photo: DraftPhoto, index: Int): Boolean

    /**
     * [order]는 지금 사진 위치를 새 순서대로 나열한 것이다.
     * 그 사이 사진이 바뀌어 맞지 않으면 그대로 둔다.
     */
    suspend fun reorderPhotos(key: DraftKey, order: List<Int>)

    /**
     * 한 트랜잭션: 일기와 사진을 쓰고 임시 저장본을 지운다.
     * 일기 ID를 돌려준다.
     * 실패하면 임시 저장본이 남는다.
     */
    suspend fun commit(key: DraftKey): String

    suspend fun discard(key: DraftKey)
}

interface TrashRepository {
    /** 지운 날짜순, 남은 날 포함. */
    fun items(): Flow<List<TrashedEntry>>

    /** 휴지통에 든 일기 수. */
    fun count(): Flow<Int> = items().map { it.size }

    suspend fun restore(id: String)

    suspend fun deleteForever(ids: List<String>)

    suspend fun empty()

    /**
     * 30일 지난 것을 지운다.
     * 앱 시작 때 부른다.
     */
    suspend fun purgeExpired(now: Instant)
}

interface StatsRepository {
    fun moodCounts(period: StatsPeriod): Flow<MoodCounts>

    fun moodFlow(period: StatsPeriod): Flow<List<DayMood>>

    fun advanced(period: AdvancedPeriod, today: LocalDate): Flow<AdvancedStats>

    fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>>
}

interface MediaRepository {
    /** 일기와 임시 저장본 어디에서도 쓰지 않고 하루 넘게 지난 사진 파일을 지운다. */
    suspend fun cleanUnused(now: Instant)
}
