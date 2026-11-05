package org.sjhstudio.diary.feature.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.DraftRepository
import org.sjhstudio.diary.core.data.diary.canSave
import org.sjhstudio.diary.core.data.richtext.toPlainText
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.platform.FontInstaller

/** 쓰기를 어디로 여는가. */
sealed interface WriteTarget {
    /** [date]가 없으면 오늘이다. 달력 빈 날에서 오면 그 날이다. */
    data class New(val date: LocalDate? = null) : WriteTarget

    data class Edit(val entryId: String) : WriteTarget
}

/** 본문 글꼴과 크기. */
data class BodyLook(val font: DiaryFont = DiaryFont.DEFAULT, val size: BodyTextSize = BodyTextSize.DEFAULT, val fontPath: String? = null)

/** "쓰던 일기가 있어요" 시트에 보일 것. */
data class ResumeOffer(val date: LocalDate, val time: LocalTime, val preview: String) {
    override fun toString(): String = "ResumeOffer"
}

/** 사진을 더하고 나서 한 번 알릴 일. */
sealed interface PhotoNotice {
    data class Limit(val limit: Int, val canUpgrade: Boolean) : PhotoNotice

    data object NoSpace : PhotoNotice

    data object Unreadable : PhotoNotice
}

data class WriteUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val date: LocalDate = LocalDate(1970, 1, 1),
    val time: LocalTime = LocalTime(0, 0),
    val mood: Mood? = null,
    val weather: Weather? = null,
    val placeName: String = "",
    /** 편집기에 넣을 본문. [bodyVersion]이 바뀔 때만 다시 넣는다. */
    val initialBody: String = "",
    val bodyVersion: Int = 0,
    val photos: List<DraftPhoto> = emptyList(),
    val canSave: Boolean = false,
    val recentPlaces: List<String> = emptyList(),
    val resume: ResumeOffer? = null,
    val photoNotice: PhotoNotice? = null,
    val saveFailed: Boolean = false
) {
    override fun toString(): String = "WriteUiState"
}

/** 화면을 닫을 때 한 번. */
sealed interface WriteEvent {
    data class Saved(val entryId: String) : WriteEvent

    /** [keptDraft]면 "임시 저장했어요"를 알린다. */
    data class Closed(val keptDraft: Boolean) : WriteEvent

    data object Discarded : WriteEvent
}

/**
 * 쓰기와 고치기(FR-002~FR-012).
 * 내용이 바뀌면 입력이 멈추고 1초 뒤 임시 저장본에 쓰고, 화면을 떠날 때는 바로 쓴다.
 * 빈 글은 임시 저장본을 만들지 않는다. 사진은 고른 즉시 저장소로 들어간다(research R6).
 */
class WriteViewModel(
    private val target: WriteTarget,
    private val drafts: DraftRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val mediaPath: (String) -> String = { it },
    settings: SettingsRepository? = null,
    private val fonts: FontInstaller? = null,
    plans: PlanRepository? = null
) : ViewModel() {
    /** 한 편에 넣을 수 있는 사진 수. 요금제를 바꾸면 바로 따른다. */
    val photoLimit: StateFlow<Int> =
        (plans?.plan ?: flowOf(Plan.FREE))
            .map { it.photoLimit }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Plan.FREE.photoLimit)

    /** 본문은 고른 글꼴과 글자 크기로 보인다(FR-014). */
    val look: StateFlow<BodyLook> =
        (settings?.settings ?: flowOf(AppSettings()))
            .map { BodyLook(it.font, it.bodyTextSize, fonts?.installedPath(it.font.id)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    fun mediaPath(fileName: String): String = mediaPath.invoke(fileName)

    private val _state = MutableStateFlow(WriteUiState())
    val state: StateFlow<WriteUiState> = _state.asStateFlow()

    private val _events = Channel<WriteEvent>(Channel.BUFFERED)
    val events: Flow<WriteEvent> = _events.receiveAsFlow()

    private var key: DraftKey = DraftKey.New()
    private var body: String = ""
    private var loadedBody: String? = null
    private var weatherAutoFilled = false
    private var isFavorite = false
    private var pending: Job? = null
    private var photosJob: Job? = null

    /** 저장본에 쓴 뒤로 바뀐 것이 있는가. */
    private var dirty = false

    /** 임시 저장본이 저장소에 있는가. */
    private var stored = false

    /** 고치기로 열었을 때의 값. 같으면 닫을 때 임시 저장본을 남기지 않는다. */
    private var opened: Draft? = null

    init {
        viewModelScope.launch {
            val places = drafts.recentPlaces()
            _state.update { it.copy(recentPlaces = places) }
        }
        viewModelScope.launch {
            when (target) {
                is WriteTarget.New -> openNew(target)
                is WriteTarget.Edit -> openEdit(target)
            }
        }
    }

    private suspend fun openNew(new: WriteTarget.New) {
        val existing = drafts.latestNew()
        if (existing != null && existing.canSave()) {
            _state.update {
                blank(new.date).copy(resume = ResumeOffer(existing.date, existing.time, toPlainText(existing.bodyHtml)))
            }
            return
        }
        existing?.let { key = it.key }
        _state.value = blank(new.date).copy(loading = false)
    }

    private suspend fun openEdit(edit: WriteTarget.Edit) {
        val draft = drafts.openEdit(edit.entryId)
        if (draft == null) {
            _events.send(WriteEvent.Closed(keptDraft = false))
            return
        }
        opened = draft
        load(draft, isEdit = true)
    }

    /** [이어 쓰기]. */
    fun continueDraft() {
        viewModelScope.launch {
            val draft = drafts.latestNew() ?: return@launch
            load(draft, isEdit = false)
        }
    }

    /** [새로 쓰기]. 쓰던 글은 버리기 전까지 그대로 두고 다른 칸에 쓴다(FR-010). */
    @OptIn(ExperimentalUuidApi::class)
    fun startFresh() {
        key = DraftKey.New("${DraftKey.New.PREFIX}-${Uuid.random()}")
        _state.update { it.copy(loading = false, resume = null) }
    }

    private fun load(draft: Draft, isEdit: Boolean) {
        key = draft.key
        body = draft.bodyHtml
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
                initialBody = draft.bodyHtml,
                bodyVersion = it.bodyVersion + 1,
                photos = draft.photos,
                resume = null
            )
        }
        refreshCanSave()
        watchPhotos()
    }

    private fun blank(date: LocalDate?): WriteUiState {
        val now = clock.now().toLocalDateTime(timeZone)
        return WriteUiState(date = date ?: now.date, time = LocalTime(now.hour, now.minute))
    }

    /**
     * 편집기에 본문을 넣은 뒤 편집기가 다시 내보낸 HTML을 알린다.
     * 태그 모양만 다른 값을 고친 것으로 치지 않아, 손대지 않은 레거시 글이 원문 그대로 남는다.
     */
    fun onBodyLoaded(editorHtml: String) {
        loadedBody = editorHtml
    }

    fun onBodyChange(html: String) {
        val value = if (html == loadedBody) _state.value.initialBody else html
        if (value == body) return
        body = value
        changed()
    }

    /** 같은 기분을 다시 누르면 푼다(FR-003). */
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
        _state.update { it.copy(placeName = name.take(MAX_PLACE)) }
        changed()
    }

    /** 오늘이 아닌 날로 바꾸면 자동으로 채운 날씨만 지운다(FR-005). */
    fun setDate(date: LocalDate) {
        val today = clock.now().toLocalDateTime(timeZone).date
        val clearWeather = weatherAutoFilled && date != today
        if (clearWeather) weatherAutoFilled = false
        _state.update { it.copy(date = date, weather = if (clearWeather) null else it.weather) }
        changed()
    }

    fun setTime(time: LocalTime) {
        _state.update { it.copy(time = time) }
        changed()
    }

    fun addPhotos(picked: List<PickedImage>) {
        if (picked.isEmpty()) return
        viewModelScope.launch {
            pending?.cancel()
            flush(force = true)
            watchPhotos()
            val notice =
                when (val result = drafts.addPhotos(key, picked)) {
                    is AddPhotosResult.Added -> if (result.failed > 0) PhotoNotice.Unreadable else null
                    is AddPhotosResult.LimitReached -> PhotoNotice.Limit(result.limit, result.canUpgrade)
                    is AddPhotosResult.NoSpace -> PhotoNotice.NoSpace
                }
            _state.update { it.copy(photoNotice = notice) }
        }
    }

    fun removePhoto(index: Int) {
        viewModelScope.launch { drafts.removePhoto(key, index) }
    }

    fun movePhoto(from: Int, to: Int) {
        val size = _state.value.photos.size
        if (from !in 0 until size || to !in 0 until size || from == to) return
        val order = (0 until size).toMutableList().apply { add(to, removeAt(from)) }
        viewModelScope.launch { drafts.reorderPhotos(key, order) }
    }

    fun photoNoticeShown() = _state.update { it.copy(photoNotice = null) }

    /** 앱이 백그라운드로 갈 때 바로 쓴다(FR-009). */
    fun onStop() {
        pending?.cancel()
        viewModelScope.launch { flush() }
    }

    fun save() {
        if (!_state.value.canSave) return
        viewModelScope.launch {
            pending?.cancel()
            flush(force = true)
            try {
                val id = drafts.commit(key)
                _events.send(WriteEvent.Saved(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 임시 저장본은 그대로 있다. 다시 누를 수 있게 알리기만 한다.
                _state.update { it.copy(saveFailed = true) }
            }
        }
    }

    fun saveFailureShown() = _state.update { it.copy(saveFailed = false) }

    /** 뒤로 가기. 쓴 것이 있으면 묻지 않고 임시 저장한다(FR-011). */
    fun close() {
        viewModelScope.launch {
            pending?.cancel()
            val current = currentDraft()
            val base = opened
            when {
                base != null && unchanged(base, current) -> {
                    drafts.discard(key)
                    _events.send(WriteEvent.Closed(keptDraft = false))
                }

                current.canSave() -> {
                    flush(force = true)
                    _events.send(WriteEvent.Closed(keptDraft = true))
                }

                else -> {
                    if (stored) drafts.discard(key)
                    _events.send(WriteEvent.Closed(keptDraft = false))
                }
            }
        }
    }

    /** [버리기]. 화면이 한 번 물은 뒤에 부른다. 고치던 일기는 그대로 남고 고친 내용만 버린다. */
    fun discard() {
        viewModelScope.launch {
            pending?.cancel()
            drafts.discard(key)
            _events.send(WriteEvent.Discarded)
        }
    }

    /** 시트의 [버리기]: 쓰던 글을 지우고 빈 화면으로 시작한다. */
    fun discardOffered() {
        viewModelScope.launch {
            drafts.latestNew()?.let { drafts.discard(it.key) }
            key = DraftKey.New()
            _state.update { it.copy(loading = false, resume = null) }
        }
    }

    private fun changed() {
        dirty = true
        refreshCanSave()
        pending?.cancel()
        pending =
            viewModelScope.launch {
                delay(SAVE_DELAY_MILLIS)
                flush()
            }
    }

    private fun refreshCanSave() = _state.update { it.copy(canSave = currentDraft().canSave()) }

    private suspend fun flush(force: Boolean = false) {
        if (!dirty && !force) return
        val draft = currentDraft()
        // 빈 글은 저장본을 만들지 않는다. 다음에 "쓰던 일기가 있어요"가 뜨지 않게 하려고다.
        if (!stored && !draft.canSave() && !force) return
        drafts.save(draft)
        stored = true
        dirty = false
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
            viewModelScope.launch {
                drafts.draft(key).collectLatest { draft ->
                    if (draft != null) {
                        _state.update { it.copy(photos = draft.photos) }
                        refreshCanSave()
                    }
                }
            }
    }

    private fun unchanged(a: Draft, b: Draft): Boolean =
        a.copy(updatedAt = b.updatedAt, placeName = a.placeName?.trim()?.ifEmpty { null }) == b

    private companion object {
        const val SAVE_DELAY_MILLIS = 1_000L
        const val MAX_PLACE = 30
    }
}
