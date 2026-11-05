# 데이터 접근 계약

기능 모듈이 부르는 저장소 인터페이스다. 구현은 `shared/core/data`, 대역은 `shared/core/testing`에 둔다. 모든 읽기는 `Flow`, 쓰기는 `suspend`다.
휴지통에 있는 일기는 `TrashRepository` 말고는 어디서도 돌려주지 않는다.

```kotlin
interface EntryRepository {
    fun timeline(favoritesOnly: Boolean): Flow<PagingData<EntryRow>> // Paging 3(스펙 001 research R4)
    fun entry(id: String): Flow<EntryDetail?>
    fun day(date: LocalDate): Flow<List<EntryRow>>
    fun month(yearMonth: YearMonth): Flow<Map<LocalDate, DayMood>> // 달력 칸: 마지막 일기의 기분, 편수
    fun search(filter: SearchFilter): Flow<PagingData<EntryRow>> // 홈과 같은 쪽 읽기(2026-11-05)
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun moveToTrash(id: String)
    suspend fun restore(id: String)
}

interface DraftRepository {
    fun draft(key: DraftKey): Flow<Draft?>
    suspend fun openEdit(entryId: String): Draft?  // 임시 저장본이 없으면 일기에서 만든다
    suspend fun save(draft: Draft)                 // 디바운스는 ViewModel이 한다
    suspend fun addPhotos(key: DraftKey, picked: List<PickedImage>): AddPhotosResult // 한도, 공간 부족
    suspend fun removePhoto(key: DraftKey, index: Int)     // 옮겨 온 일기는 같은 사진이 두 줄일 수 있어 위치로 고른다
    suspend fun reorderPhotos(key: DraftKey, order: List<Int>)
    suspend fun commit(key: DraftKey): String      // 한 트랜잭션: Entry·Media 쓰고 Draft 지움. 일기 ID를 돌려준다
    suspend fun discard(key: DraftKey)
}

interface TrashRepository {
    fun items(): Flow<List<TrashedEntry>>          // 지운 날짜순, 남은 날 포함
    suspend fun restore(id: String)
    suspend fun deleteForever(ids: List<String>)
    suspend fun empty()
    suspend fun purgeExpired(now: Instant)         // 30일 지난 것. 앱 시작 때
}

interface StatsRepository {
    fun moodCounts(period: StatsPeriod): Flow<MoodCounts>     // 9종 개수, 기분 없음 수
    fun moodFlow(period: StatsPeriod): Flow<List<DayMood>>    // 월·년
    fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>>
}

interface MediaRepository {
    suspend fun cleanUnused(now: Instant)          // R6 규칙
}
```

규칙:

- `EntryRow`, `EntryDetail`에는 화면에 필요한 것만 담는다. 로그·분석 이벤트로 넘기지 않는다(헌법 II).
- `search`는 사용자 입력의 `%`, `_`, `\`를 이스케이프한다.
- `commit`이 실패하면 `Draft`는 그대로 남는다.
- 고칠 때 찾을 수 없는 사진 줄은 임시 저장본에 싣지 않고 저장 때 그대로 뒤에 남긴다(헌법 I).
- 한도는 `AddPhotosResult.LimitReached(limit, canUpgrade)`로 알려 주고 저장은 막지 않는다.
