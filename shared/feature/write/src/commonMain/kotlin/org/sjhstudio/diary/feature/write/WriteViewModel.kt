package org.sjhstudio.diary.feature.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.DraftRepository
import org.sjhstudio.diary.core.data.diary.canSave
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.model.richtext.toPlainText
import org.sjhstudio.diary.core.platform.NoPlaceFinder
import org.sjhstudio.diary.core.platform.PlaceFinder
import org.sjhstudio.diary.core.platform.PlaceLookup
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

/** 쓰기를 어디로 여는가 */
sealed interface WriteTarget {
    /** [date]가 없으면 오늘, 달력 빈 날에서 오면 그 날 */
    data class New(val date: LocalDate? = null) : WriteTarget

    data class Edit(val entryId: String) : WriteTarget
}

/** "쓰던 일기가 있어요" 시트에 보일 것 */
/** [date]·[time]은 마지막으로 고친 때. 일기 날짜가 아님 */
data class ResumeOffer(val date: LocalDate, val time: LocalTime, val preview: String) {
    override fun toString(): String = "ResumeOffer"
}

/** 사진을 추가한 뒤 한 번 알릴 일 */
sealed interface PhotoNotice {
    data class Limit(val limit: Int, val canUpgrade: Boolean) : PhotoNotice

    data object NoSpace : PhotoNotice

    data object Unreadable : PhotoNotice
}

/** 장소 이름 길이(FR-002) */
internal const val MAX_PLACE = 30

data class WriteUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val date: LocalDate = LocalDate(1970, 1, 1),
    val time: LocalTime = LocalTime(0, 0),
    val mood: Mood? = null,
    val weather: Weather? = null,
    val placeName: String = "",
    /** 저장본에서 본문을 불러올 때마다 오름. 바뀌면 편집기가 [WriteViewModel.currentBody]를 넣음 */
    val bodyVersion: Int = 0,
    val photos: List<DraftPhoto> = emptyList(),
    val canSave: Boolean = false,
    val recentPlaces: List<String> = emptyList(),
    val locating: PlaceLocating = PlaceLocating.Idle,
    val resume: ResumeOffer? = null,
    val photoNotice: PhotoNotice? = null,
    val saveFailed: Boolean = false,
    /** 수정할 일기를 불러오지 못함. 빈 글로 덮어쓰지 않게 저장도 막음 */
    val openFailed: Boolean = false,
    /** 남겨 둔 뒤에 일기가 바뀜. 저장하면 두 가지로 남는다고 미리 알림 */
    val entryChanged: Boolean = false,
    /** 고른 사진을 넣는 중 */
    val addingPhotos: Boolean = false,
    /** 남겨 둔 임시 저장본을 이어 열기. 그때만 버리는 길을 보임 */
    val canDiscard: Boolean = false,
    /**
     * 쓰고 있는 새 일기 임시 저장본의 키
     * 화면이 저장해 두었다가 화면 모델이 다시 만들어져도 같은 글을 이음
     */
    val draftKey: String? = null,
    /** 저장을 누름. 끝나고 화면이 닫힐 때까지 다시 누를 수 없음 */
    val saving: Boolean = false
) {
    override fun toString(): String = "WriteUiState"
}

/** 장소 시트의 내 위치 가져오기 상태 */
sealed interface PlaceLocating {
    data object Idle : PlaceLocating

    data object Finding : PlaceLocating

    data class Done(val result: PlaceLookup) : PlaceLocating
}

/** 화면을 닫을 때 한 번 */
sealed interface WriteEvent {
    data class Saved(val entryId: String) : WriteEvent

    /** [keptDraft]면 "임시 저장했어요"를 알림 */
    data class Closed(val keptDraft: Boolean) : WriteEvent

    data object Discarded : WriteEvent
}

/**
 * 쓰기와 수정(FR-002~FR-012)
 * 내용이 바뀌면 입력이 멈추고 1초 뒤 임시 저장본에 쓰고, 화면을 떠날 때는 바로 씀
 * 본문은 글자마다 HTML로 바꾸지 않고 바뀜 표시만 해 두었다가 쓰기 직전에 편집기에서 꺼냄
 * 빈 글은 임시 저장본을 만들지 않음
 * 사진은 고른 즉시 저장소로 들어감(research R6)
 */
class WriteViewModel(
    private val target: WriteTarget,
    private val drafts: DraftRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val mediaPath: (String) -> String = { it },
    settings: SettingsRepository,
    /** 글꼴 ID로 받아 둔 글꼴 파일 찾기(`FontInstaller.installedPath`) */
    fontPath: (String) -> String? = { null },
    plans: PlanRepository,
    private val places: PlaceFinder = NoPlaceFinder,
    /** 앱이 종료되거나 잠겼다 돌아와 이어 쓰는 새 일기의 키 */
    private val restoredKey: String? = null
) : ViewModel() {
    /** 한 편에 넣을 수 있는 사진 수. 요금제를 바꾸면 바로 따름 */
    val photoLimit: StateFlow<Int> =
        plans.confirmed.filterNotNull()
            .map { it.photoLimit }
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, Plan.FREE.photoLimit)

    /** 새 체크리스트를 만들 수 있는지. 구독이 끝나도 이미 쓴 체크리스트는 수정하고 체크할 수 있음 */
    val checklistOpen: StateFlow<Boolean> =
        plans.confirmed.filterNotNull()
            .map { it.checklist }
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, Plan.FREE.checklist)

    /** 본문은 고른 글꼴과 글자 크기로 보임(FR-014) */
    val look: StateFlow<BodyLook> = settings.bodyLookIn(viewModelScope, fontPath)

    fun mediaPath(fileName: String): String = mediaPath.invoke(fileName)

    private val _state = MutableStateFlow(blank((target as? WriteTarget.New)?.date).copy(isEdit = target is WriteTarget.Edit))
    val state: StateFlow<WriteUiState> = _state.asStateFlow()

    private val _events = Channel<WriteEvent>(Channel.BUFFERED)
    val events: Flow<WriteEvent> = _events.receiveAsFlow()

    // 사진을 뺄 때마다 한 번. 화면이 [되돌리기] 토스트를 띄움
    // 상태로 두면 화면을 다시 그릴 때(회전, 잠금 풀기, 결제 화면에서 돌아오기) 토스트가 또 뜸
    private val _photoRemovals = Channel<Unit>(Channel.CONFLATED)
    val photoRemovals: Flow<Unit> = _photoRemovals.receiveAsFlow()

    private var key: DraftKey = DraftKey.New()
    private var body: String = ""

    /**
     * 편집기가 불러온 뒤 다시 내보낸 HTML과 그때의 저장 값
     * 태그 모양만 다른 값을 고친 것으로 치지 않는 데 씀
     */
    private var loadedBody: String? = null
    private var loadedSource: String = ""

    /** 편집기에서 지금 HTML을 꺼내는 함수. 화면이 붙어 있을 때만 있음 */
    private var editor: (() -> String)? = null

    /** 편집기가 바뀌었는데 아직 [body]로 꺼내지 않음 */
    private var editorChanged = false

    /** [editorChanged]인 동안 저장 버튼을 켤지 정하는 값 */
    private var editorHasText = false

    private var weatherAutoFilled = false
    private var isFavorite = false
    private var pending: Job? = null
    private var photosJob: Job? = null
    private var locatingJob: Job? = null

    /**
     * 어떤 글을 쓸지 정해짐
     * 그 전에 임시 저장본을 쓰면 빈 글로 남겨 둔 글을 덮음
     * 카메라에서 돌아온 결과는 화면 모델이 글을 읽기 전에 들어오기도 함
     */
    private val ready = CompletableDeferred<Unit>()

    /** 넣는 중인 사진. 닫기와 저장은 이 일이 끝나길 기다림 */
    private val photoWork = mutableListOf<Job>()

    /**
     * 화면을 떠나는 중. 저장, 닫기, 버리기는 모두 떠나는 길이라 먼저 시작한 하나만 감
     * 실패해 남게 되면 다시 해제
     */
    private var leaving = false
    private var lastRemoved: Instant? = null

    /** 마지막으로 뺀 사진과 자리. 되돌리기용 */
    private var undoRemove: Pair<DraftPhoto, Int>? = null

    /** 전에 남겨 둔 임시 저장본을 이어 열기 */
    private var resumed = false

    /** 저장본에 쓴 뒤로 바뀐 것이 있는가 */
    private var dirty = false

    /** 임시 저장본이 저장소에 있는가 */
    private var stored = false

    init {
        viewModelScope.launchSafely {
            // 최근 장소는 곁일이라 읽지 못하면 비워 둠
            val places = orElseOnFailure(emptyList()) { drafts.recentPlaces() }
            _state.update { it.copy(recentPlaces = places) }
        }
        viewModelScope.launchSafely {
            when (target) {
                // 쓰던 글을 읽지 못해도 저장본은 그대로 두고 빈 글로 바로 씀
                is WriteTarget.New -> orElseOnFailure(null) { openNew(target) } ?: startBlankIfNotReady(target.date)

                // 수정할 일기를 읽지 못하면 빈 글로 덮어쓰지 않게 안내만 하고 저장 준비를 끝내지 않음
                is WriteTarget.Edit ->
                    orElseOnFailure(null) { openEdit(target) } ?: openFailed()
            }
        }
    }

    private suspend fun openNew(new: WriteTarget.New) {
        restoredKey?.let { value ->
            val restored = DraftKey.New(value)
            val draft = drafts.draft(restored).first()
            if (draft != null) load(draft, isEdit = false) else startBlank(new.date, restored)
            return
        }
        val existing = drafts.latestNew()
        if (existing != null && existing.canSave()) {
            // "…에 쓰던 글"은 마지막으로 고친 때. 일기 날짜를 지난날로 바꿨어도 쓴 때를 알림
            val edited = existing.updatedAt.toLocalDateTime(timeZone)
            _state.update {
                blank(new.date).copy(resume = ResumeOffer(edited.date, edited.time, toPlainText(existing.bodyHtml)))
            }
            return
        }
        startBlank(new.date, existing?.key ?: key)
    }

    /** 빈 글로 시작. 편집기에 남은 글도 함께 비움 */
    private fun startBlank(date: LocalDate?, draftKey: DraftKey) {
        key = draftKey
        body = ""
        loadedBody = null
        editorChanged = false
        stored = false
        dirty = false
        _state.update { blank(date).copy(loading = false, draftKey = draftKey.value, bodyVersion = it.bodyVersion + 1) }
        ready.complete(Unit)
    }

    private fun startBlankIfNotReady(date: LocalDate?) {
        if (!ready.isCompleted) startBlank(date, key)
    }

    private suspend fun openEdit(entry: WriteTarget.Edit) {
        val edit = drafts.openEdit(entry.entryId)
        if (edit == null) {
            closed(keptDraft = false)
            return
        }
        resumed = edit.resumed
        load(edit.draft, isEdit = true)
        _state.update { it.copy(entryChanged = edit.entryChanged) }
    }

    /** [이어 쓰기] */
    fun continueDraft() {
        viewModelScope.launchSafely {
            // 읽지 못하면 [이어 쓰기] 안내를 그대로 둬 다시 누를 수 있게
            val draft = orElseOnFailure(null) { drafts.latestNew() } ?: return@launchSafely
            resumed = true
            load(draft, isEdit = false)
        }
    }

    /** [새로 쓰기]. 쓰던 글은 버리기 전까지 그대로 두고 다른 칸에 씀(FR-010) */
    fun startFresh() = startBlank((target as? WriteTarget.New)?.date, freshKey())

    @OptIn(ExperimentalUuidApi::class)
    private fun freshKey() = DraftKey.New("${DraftKey.New.PREFIX}-${Uuid.random()}")

    /**
     * 시트에서 아무것도 고르지 않고 뒤로 가면 들어온 화면으로 돌아감
     * 쓰던 글은 불러오지도, 건드리지도 않음
     */
    fun leaveOffer() {
        if (leaving) return
        leaving = true
        _state.update { it.copy(resume = null) }
        viewModelScope.launchSafely { closed(keptDraft = false) }
    }

    private fun load(draft: Draft, isEdit: Boolean) {
        key = draft.key
        body = draft.bodyHtml
        editorChanged = false
        stored = true
        weatherAutoFilled = draft.weatherAutoFilled
        isFavorite = draft.isFavorite
        _state.update {
            it.copy(
                loading = false,
                isEdit = isEdit,
                date = draft.date,
                time = draft.time,
                mood = draft.mood,
                weather = draft.weather,
                placeName = draft.placeName.orEmpty(),
                bodyVersion = it.bodyVersion + 1,
                photos = draft.photos,
                resume = null,
                draftKey = draft.key.value.takeUnless { isEdit }
            )
        }
        refreshCanSave()
        watchPhotos()
        ready.complete(Unit)
    }

    private fun blank(date: LocalDate?): WriteUiState {
        val now = clock.now().toLocalDateTime(timeZone)
        return WriteUiState(date = date ?: now.date, time = LocalTime(now.hour, now.minute))
    }

    /** 편집기에 넣을 지금 본문. 저장본의 옛 글이 아니라 마지막으로 꺼낸 글 */
    fun currentBody(): String = body

    /**
     * 편집기에 본문을 넣은 뒤 편집기가 다시 내보낸 HTML.
     * 태그 모양만 다른 값을 고친 것으로 치지 않아 손대지 않은 레거시 글이 원문 그대로 남음
     */
    fun onBodyLoaded(editorHtml: String) {
        loadedBody = editorHtml
        loadedSource = body
    }

    /** 화면이 편집기를 붙임. [read]는 메인에서 불리고 지금 본문을 저장 값으로 돌려줌 */
    fun attachEditor(read: () -> String) {
        editor = read
    }

    /** 편집기가 화면에서 사라짐. 사라지면 꺼낼 수 없어 마지막 입력을 먼저 받아 둠 */
    fun detachEditor() {
        takeEditorBody()
        editor = null
    }

    /**
     * 글자를 칠 때마다 불림
     * 변환은 글이 길수록 느려 표시만 해 두고 임시 저장본에 쓰기 직전에 꺼냄
     * [hasText]는 변환 전에도 저장 버튼을 바로 켜고 끄는 데 씀
     */
    fun onEditorChanged(hasText: Boolean) {
        editorChanged = true
        editorHasText = hasText
        changed(contentChanged = false)
    }

    fun onBodyChange(html: String) {
        if (applyBody(html)) changed()
    }

    private fun applyBody(html: String): Boolean {
        val value = if (html == loadedBody) loadedSource else html
        if (value == body) return false
        body = value
        dirty = true
        return true
    }

    /**
     * 쓰기 직전이나 화면을 떠날 때 불림
     * 앱이 꺼져도 마지막 입력이 남게 여기서 동기로 꺼냄(헌법 I)
     */
    private fun takeEditorBody() {
        val read = editor ?: return
        if (!editorChanged) return
        editorChanged = false
        applyBody(read())
        refreshCanSave()
    }

    /** 같은 기분을 다시 누르면 해제(FR-003) */
    fun selectMood(mood: Mood?) {
        _state.update { it.copy(mood = if (it.mood == mood) null else mood) }
        changed()
    }

    fun selectWeather(weather: Weather?) {
        weatherAutoFilled = false
        _state.update { it.copy(weather = if (it.weather == weather) null else weather) }
        changed()
    }

    fun setPlace(name: String) {
        // 한도에서 자를 때 이모지가 반쪽만 남지 않게
        val cut = name.take(MAX_PLACE)
        _state.update { it.copy(placeName = if (cut.lastOrNull()?.isHighSurrogate() == true) cut.dropLast(1) else cut) }
        changed()
    }

    /** 찾은 이름은 시트 입력 칸에 넣음. 완료를 눌러야 일기에 들어감(FR-005) */
    fun findPlace() {
        if (_state.value.locating == PlaceLocating.Finding) return
        _state.update { it.copy(locating = PlaceLocating.Finding) }
        locatingJob =
            viewModelScope.launchSafely {
                val result = places.find()
                _state.update { it.copy(locating = PlaceLocating.Done(result)) }
            }
    }

    /** 시트를 닫으면 찾던 일을 멈추고 안내도 지움 */
    fun stopFindingPlace() {
        locatingJob?.cancel()
        locatingJob = null
        _state.update { it.copy(locating = PlaceLocating.Idle) }
    }

    /** 오늘이 아닌 날로 바꾸면 자동으로 채운 날씨만 지움(FR-005) */
    fun setDate(date: LocalDate) {
        val today = currentDate(clock, timeZone)
        val clearWeather = weatherAutoFilled && date != today
        if (clearWeather) weatherAutoFilled = false
        _state.update { it.copy(date = date, weather = if (clearWeather) null else it.weather) }
        changed()
    }

    fun setTime(time: LocalTime) {
        _state.update { it.copy(time = time) }
        changed()
    }

    // 열지 못한 일기에는 사진을 붙일 글이 없음. 준비를 기다리던 사진 넣기를 끝내 "넣는 중"이 남지 않게 함
    private fun openFailed() {
        _state.update { it.copy(loading = false, openFailed = true) }
        photoWork.toList().forEach { it.cancel() }
    }

    fun addPhotos(picked: List<PickedImage>) {
        if (picked.isEmpty() || _state.value.saving || _state.value.openFailed) return
        _state.update { it.copy(addingPhotos = true) }
        val work = viewModelScope.launchSafely {
            ready.await()
            pending?.cancel()
            val notice =
                try {
                    flush(force = true)
                    watchPhotos()
                    when (val result = drafts.addPhotos(key, picked)) {
                        is AddPhotosResult.Added -> if (result.failed > 0) PhotoNotice.Unreadable else null
                        is AddPhotosResult.LimitReached -> PhotoNotice.Limit(result.limit, result.canUpgrade)
                        is AddPhotosResult.NoSpace -> PhotoNotice.NoSpace
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 사진 파일은 썼는데 DB에 적지 못한 때. 공간이 찼을 때 일어남
                    PhotoNotice.NoSpace
                }
            _state.update { it.copy(photoNotice = notice) }
        }
        photoWork += work
        work.invokeOnCompletion {
            photoWork -= work
            if (photoWork.isEmpty()) _state.update { it.copy(addingPhotos = false) }
        }
    }

    /** 빼면 뒤 사진이 그 자리로 당겨짐. 연타한 두 번째가 다음 사진까지 빼지 않게 잠깐 받지 않음 */
    fun removePhoto(index: Int) {
        val now = clock.now()
        if (lastRemoved?.let { now - it < REMOVE_GAP } == true) return
        lastRemoved = now
        write {
            val photo = drafts.removePhoto(key, index) ?: return@write
            undoRemove = photo to index
            _photoRemovals.send(Unit)
        }
    }

    /**
     * 방금 뺀 사진을 원래 자리에 되돌림
     * 사진 칸의 빼기 단추가 72 칸의 절반 가까이를 덮어 잘못 누르기 쉬움(TDS 토스트의 되돌리기)
     */
    fun undoRemovePhoto() {
        val (photo, index) = undoRemove ?: return
        undoRemove = null
        write { drafts.restorePhoto(key, photo, index) }
    }

    /** [from]번째 사진을 [by]칸 옮김. 범위는 지금 목록으로 자름(화면이 쥔 목록은 옛것일 수 있음) */
    fun movePhoto(from: Int, by: Int) {
        val size = _state.value.photos.size
        if (from !in 0 until size) return
        val to = (from + by).coerceIn(0, size - 1)
        if (from == to) return
        val order = (0 until size).toMutableList().apply { add(to, removeAt(from)) }
        write { drafts.reorderPhotos(key, order) }
    }

    fun photoNoticeShown() = _state.update { it.copy(photoNotice = null) }

    /** iCloud에서 받지 못했거나 카메라를 열지 못함. 말없이 넘어가면 왜 사진이 없는지 모름 */
    fun photoPickFailed() = _state.update { it.copy(photoNotice = PhotoNotice.Unreadable) }

    /** 앱이 백그라운드로 갈 때 바로 씀(FR-009) */
    fun onStop() {
        // 닫히는 중이면 버린 글을 임시 저장본으로 되살리지 않음
        if (leaving) return
        pending?.cancel()
        takeEditorBody()
        write { flush() }
    }

    /** 연타해도 한 번만 저장. 메인에서만 불리니 표시를 먼저 세우면 두 번째나 닫기가 들어오지 못함 */
    fun save() {
        if (leaving) return
        takeEditorBody()
        if (!_state.value.canSave) return
        leaving = true
        _state.update { it.copy(saving = true) }
        viewModelScope.launchSafely {
            pending?.cancel()
            try {
                photoWork.toList().joinAll()
                flush(force = true)
                val id = drafts.commit(key)
                _events.send(WriteEvent.Saved(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 임시 저장본은 그대로 있어 다시 누를 수 있게 알리기만
                leaving = false
                _state.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    /** 닫힘을 알리는 길은 여기 하나. 닫히는 전환 중에 뒤로 가기가 또 와도 밑의 화면까지 닫지 않게 표시를 세움 */
    private suspend fun closed(keptDraft: Boolean) {
        leaving = true
        _events.send(WriteEvent.Closed(keptDraft))
    }

    /** 뒤로 가기. 쓴 것이 있으면 묻지 않고 임시 저장(FR-011) */
    fun close() {
        // 저장 중이거나 저장이 끝났으면 화면이 이미 닫히는 중. 임시 저장본을 다시 만들지 않음
        if (leaving) return
        leaving = true
        // 쓸 글을 고르기 전이면 남겨 둔 글을 건드리지 않음
        if (!ready.isCompleted) {
            viewModelScope.launchSafely { closed(keptDraft = false) }
            return
        }
        pending?.cancel()
        takeEditorBody()
        // 임시 저장본을 쓰지 못하면 닫지 않음. 닫으면 쓴 글이 어디에도 남지 않음(헌법 I)
        write(onFailure = { leaving = false }) {
            photoWork.toList().joinAll()
            val edit = key as? DraftKey.Edit
            when {
                !withStoredPhotos(currentDraft()).canSave() -> {
                    if (stored) drafts.discard(key)
                    closed(keptDraft = false)
                }

                edit != null -> {
                    flush(force = true)
                    val same = drafts.matchesEntry(edit)
                    if (same) drafts.discard(key)
                    closed(keptDraft = !same)
                }

                else -> {
                    flush(force = true)
                    closed(keptDraft = true)
                }
            }
        }
    }

    /**
     * 화면이 아직 모르는 사진까지 넣은 임시 저장본
     * DB는 사진을 적은 뒤 조금 늦게 알려 줘, 화면 값만 보고 빈 글로 치면 방금 넣은 사진을 버림
     */
    private suspend fun withStoredPhotos(draft: Draft): Draft =
        if (draft.photos.isEmpty() && stored) draft.copy(photos = drafts.draft(key).first()?.photos.orEmpty()) else draft

    /**
     * [버리기]. 화면이 한 번 물은 뒤에 불림
     * 수정하던 일기는 그대로 남고 고친 내용만 버림
     */
    fun discard() {
        if (leaving) return
        leaving = true
        write(onFailure = { leaving = false }) {
            pending?.cancel()
            photoWork.toList().joinAll()
            drafts.discard(key)
            _events.send(WriteEvent.Discarded)
        }
    }

    /** 시트의 [버리기]: 쓰던 글을 지우고 빈 화면으로 시작 */
    fun discardOffered() {
        write {
            drafts.latestNew()?.let { drafts.discard(it.key) }
            // 남아 있는 다른 임시 저장본을 덮지 않게 새 키로 시작
            startBlank((target as? WriteTarget.New)?.date, freshKey())
        }
    }

    private fun changed(contentChanged: Boolean = true) {
        if (contentChanged) dirty = true
        refreshCanSave()
        pending?.cancel()
        pending =
            write {
                delay(SAVE_DELAY_MILLIS)
                flush()
            }
    }

    /**
     * 임시 저장본을 고치는 일
     * 저장 공간이 차서 실패해도 앱을 끄지 않음. 앱이 꺼지면 아직 쓰지 못한 글을 잃음
     * 글은 화면에 남아 있으니 알리고, 다음에 쓸 때 다시 해 봄
     */
    private fun write(onFailure: () -> Unit = {}, block: suspend () -> Unit): Job = viewModelScope.launchSafely {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onFailure()
            _state.update { it.copy(saveFailed = true) }
        }
    }

    private fun refreshCanSave() {
        val draft = currentDraft()
        val canSave = if (editorChanged) draft.canSave(hasText = editorHasText) else draft.canSave()
        _state.update { it.copy(canSave = canSave && !it.openFailed, canDiscard = resumed) }
    }

    private suspend fun flush(force: Boolean = false) {
        if (!ready.isCompleted) return
        takeEditorBody()
        // 저장 중이면 곧 임시 저장본이 지워져 다시 만들지 않음
        if (force || (dirty && !_state.value.saving)) writeDraft(force)
    }

    private suspend fun writeDraft(force: Boolean) {
        val draft = currentDraft()
        // 빈 글은 저장본을 만들지 않음. 다음에 "쓰던 일기가 있어요"가 뜨지 않게
        if (!stored && !draft.canSave() && !force) return
        // 한 번 시작한 쓰기는 끝까지
        // 화면을 떠나며 화면 모델이 정리되면 쓰기가 취소돼 마지막 입력을 잃을 수 있음
        withContext(NonCancellable) { drafts.save(draft) }
        stored = true
        dirty = false
        _state.update { it.copy(saveFailed = false) }
        watchPhotos()
    }

    private fun currentDraft(): Draft {
        val s = _state.value
        return Draft(
            key = key,
            date = s.date,
            time = s.time,
            bodyHtml = body,
            mood = s.mood,
            weather = s.weather,
            placeName = s.placeName.trim().ifEmpty { null },
            isFavorite = isFavorite,
            weatherAutoFilled = weatherAutoFilled,
            updatedAt = clock.now(),
            photos = s.photos
        )
    }

    private fun watchPhotos() {
        if (photosJob != null) return
        photosJob =
            viewModelScope.launchSafely {
                drafts.draft(key).collectLatest { draft ->
                    if (draft != null) {
                        _state.update { it.copy(photos = draft.photos) }
                        refreshCanSave()
                    }
                }
            }
    }

    private companion object {
        const val SAVE_DELAY_MILLIS = 1_000L
        val REMOVE_GAP = 400.milliseconds
    }
}
