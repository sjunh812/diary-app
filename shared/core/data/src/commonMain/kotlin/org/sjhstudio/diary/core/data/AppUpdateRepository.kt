package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.sjhstudio.diary.core.datastore.PromptPrefs
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.model.AppVersion
import org.sjhstudio.diary.core.model.InstallStep
import org.sjhstudio.diary.core.model.ReleaseNote
import org.sjhstudio.diary.core.model.StoreCheck
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.model.quietly

/**
 * 스토어 새 버전과 받기(스펙 009)
 * Android는 Play 앱 안 업데이트, iOS는 App Store 공개 조회와 앱 위 상품 화면
 */
interface StoreUpdates {
    /** 지금 앱. Android 버전 코드, iOS 버전 이름 */
    val installed: String

    /** 다시 물을 최소 간격. 기기 안 호출인 Android는 0, 네트워크로 묻는 iOS는 하루 */
    val checkEvery: Duration

    /** 실패하면 던짐 */
    suspend fun check(): StoreCheck

    /** 받기 진행. App Store가 받는 iOS는 비어 있음 */
    val installs: Flow<InstallStep>

    /** 지금 화면 위에 스토어 창을 띄움. 띄웠으면 참 */
    suspend fun start(): Boolean

    /** 받은 새 버전으로 다시 시작(Android) */
    fun restart()

    /** 스토어의 이 앱 화면 */
    fun openStorePage()
}

/** 원격 값(Remote Config). 받지 못했으면 앱 안 기본값인 빈 문자열 */
fun interface RemoteValues {
    fun string(key: String): String
}

/**
 * 홈 카드와 설정 줄이 보는 새 버전 상태를 한 곳에서 정함
 * 받기 상태는 앱에 하나라 어디서 시작했든 같은 상태를 봄
 * 확인 실패와 오프라인은 마지막으로 안 값, 아는 값이 없으면 알리지 않음
 */
class AppUpdateRepository(
    private val store: StoreUpdates,
    private val remote: RemoteValues,
    private val prompts: PromptStore,
    private val versionName: String,
    scope: CoroutineScope,
    private val clock: Clock = Clock.System
) {
    // 받기 상태는 실시간 알림(IO)과 다시 묻기(화면 스레드)가 함께 바꿈
    // 한 값에 묶어 한 번에 바꿔야 화면이 두 값 사이의 중간 상태를 보지 않음
    private val state = MutableStateFlow(InstallState())
    private val extras = MutableStateFlow(Extras())

    // 받은 뒤 [나중에]. 이번 실행에서만 숨기고 앱을 다시 켜면 다시 알림
    private val laterThisRun = MutableStateFlow(false)

    // 처음 확인을 마쳤는지. 그 전에는 홈 맨 위 칸이 다른 카드를 먼저 그렸다가 바꾸지 않게 기다림
    private val checked = MutableStateFlow(false)

    // 앱으로 돌아올 때가 겹쳐도 한 번에 하나만 물음
    private val refreshing = Mutex()

    init {
        scope.launch {
            store.installs
                .catch { if (it !is Exception) throw it }
                .collect { next -> state.update { it.on(next) } }
        }
    }

    /** 설정 줄이 보는 상태. 닫은 버전도 그대로 보임 */
    val notice: StateFlow<UpdateNotice> = combine(state, extras, ::noticeOf).stateIn(scope, SharingStarted.Eagerly, UpdateNotice.None)

    /**
     * 홈 맨 위 카드. 없으면 null
     * 처음 확인을 마친 뒤부터 알림. 그 전에는 백업 안내처럼 읽는 중
     * [나중에]로 닫은 버전은 꼭 필요하지 않으면 숨김. 받는 중·받음은 사용자가 시작한 일이라 닫았어도 보임
     * 새 버전 알림은 업데이트 요약을 넣은 버전만. 꼭 필요한 업데이트와 받기 진행은 요약 없이도 보임
     * [notice]는 한 박자 늦게 따라와 원래 값에서 바로 정함. 늦은 값을 섞으면 빈칸이 먼저 그려짐
     */
    val homeCard: Flow<UpdateNotice?> =
        flow {
            checked.first { it }
            emitAll(
                combine(state, extras, prompts.prefs.catch { emit(PromptPrefs()) }, laterThisRun) { state, extras, prefs, later ->
                    when (val notice = noticeOf(state, extras)) {
                        UpdateNotice.None -> null

                        // 받다 실패한 것도 사용자가 시작한 일이라 닫은 버전이어도 알림
                        // 그 밖에는 업데이트 요약을 넣은 버전만. 설명·이름만 바꾼 판에 기본 문장("오류를 수정")이 나가지 않게
                        is UpdateNotice.Available -> notice.takeIf {
                            val id = (state.check as? StoreCheck.Available)?.id
                            notice.required || notice.failed ||
                                (notice.note != null && id != prefs.updateDismissed && id != state.dismissed)
                        }

                        is UpdateNotice.Downloaded -> notice.takeUnless { later && !notice.required }

                        is UpdateNotice.Downloading -> notice
                    }
                }
            )
        }.distinctUntilChanged()

    /** 앱을 열거나 돌아올 때 */
    suspend fun refresh() = refreshing.withLock {
        try {
            extras.value = orElseOnFailure(Extras()) { readExtras() }
            val prefs = orElseOnFailure(PromptPrefs()) { prompts.current() }
            val known = prefs.updateKnown?.let(::availableOrNone)
            val remembers = store.checkEvery > Duration.ZERO
            val checkedAt = prefs.updateCheckedAt
            if (remembers && known != null) {
                // 기억한 값으로 먼저 정해 홈 맨 위 칸이 네트워크를 기다리지 않음
                state.update { it.settle(known) }
                checked.value = true
                if (checkedAt != null && clock.now() - checkedAt < store.checkEvery) return@withLock
            }
            // 묻지 못하거나 답이 늦으면 마지막으로 안 값을 그대로 둠. 홈 맨 위 칸이 이 확인을 기다림
            val eventsBefore = state.value.liveEvents
            val fresh = withTimeoutOrNull(CHECK_TIMEOUT) { orElseOnFailure<StoreCheck?>(null) { store.check() } } ?: return@withLock
            if (remembers) quietly { prompts.updateChecked((fresh as? StoreCheck.Available)?.id.orEmpty(), clock.now()) }
            // 묻는 사이 실시간 알림이 왔으면 답의 받기 상태는 더 오래된 소식. 모르던 버전만 배움
            state.update { if (it.liveEvents == eventsBefore) it.settle(fresh) else it.learn(fresh) }
        } finally {
            checked.value = true
        }
    }

    private fun noticeOf(state: InstallState, extras: Extras): UpdateNotice {
        val (check, step) = state
        return when {
            step is InstallStep.Downloading -> UpdateNotice.Downloading(step.progress)
            step == InstallStep.Downloaded -> UpdateNotice.Downloaded(extras.required)
            check is StoreCheck.Downloading -> UpdateNotice.Downloading(check.progress)
            check is StoreCheck.Downloaded -> UpdateNotice.Downloaded(extras.required)
            check is StoreCheck.Available -> UpdateNotice.Available(extras.required, extras.note, step == InstallStep.Failed)
            else -> UpdateNotice.None
        }
    }

    /** [업데이트]. 스토어 창을 띄움 */
    suspend fun start(): Boolean {
        // 실패 표시는 받기가 실제로 시작되면 실시간 알림이 바꿈. 미리 지우면 닫은 버전의 카드가 사라졌다 다시 생김
        // 새로 받으면 받은 뒤 [나중에]로 숨긴 것을 풀어 [다시 시작하기]를 다시 알림
        laterThisRun.value = false
        return orElseOnFailure(false) { store.start() }
    }

    /** [다시 시작하기] */
    fun restart() = quietly { store.restart() }

    /** 복원이 새 버전을 요구할 때 */
    fun openStorePage() = quietly { store.openStorePage() }

    /** [나중에]. 그 버전은 다시 카드로 알리지 않음. 받은 뒤라면 이번 실행에서만 숨김. 실패 표시도 함께 닫음 */
    suspend fun dismiss() {
        if (notice.value is UpdateNotice.Downloaded) {
            laterThisRun.value = true
            return
        }
        val id = (state.value.check as? StoreCheck.Available)?.id ?: return
        // 닫은 버전과 실패 지우기를 한 번에 바꿔 일반 카드가 한순간도 보이지 않음. 저장은 다음 실행을 위해 그 뒤에
        state.update { it.copy(step = it.step.takeUnless { step -> step == InstallStep.Failed }, dismissed = id) }
        quietly { prompts.updateDismissed(id) }
    }

    private fun availableOrNone(known: String): StoreCheck =
        if (known.isNotEmpty() && AppVersion.isNewer(known, store.installed)) StoreCheck.Available(known) else StoreCheck.None

    private fun readExtras(): Extras {
        val minimum = remote.string(KEY_MIN_VERSION)
        val noteVersion = remote.string(KEY_NOTE_VERSION)
        val texts = NOTE_LANGUAGES.associateWith { remote.string(noteKey(it)).trim() }.filterValues(String::isNotEmpty)
        return Extras(
            required = minimum.isNotBlank() && AppVersion.isNewer(minimum, versionName),
            note = ReleaseNote(texts).takeIf {
                noteVersion.isNotBlank() && AppVersion.isNewer(noteVersion, versionName) &&
                    ReleaseNote.FALLBACK_LANGUAGE in texts
            }
        )
    }

    private data class Extras(val required: Boolean = false, val note: ReleaseNote? = null)

    /**
     * 받기 상태 한 벌
     * [check]는 앱으로 돌아올 때 스토어에 물은 값, [step]은 앱을 켜 둔 동안 오는 Play 실시간 알림
     * [liveEvents]는 받은 알림 수. 묻는 사이 알림이 왔는지 가림
     * [dismissed]는 이번 실행에서 [나중에]로 닫은 버전. 저장한 값보다 먼저 화면에 반영
     */
    private data class InstallState(
        val check: StoreCheck = StoreCheck.None,
        val step: InstallStep? = null,
        val liveEvents: Int = 0,
        val dismissed: String? = null
    ) {
        // 취소·실패면 물어 둔 받는 중·받음을 그 자리에서 "받을 수 있음"으로 내림. 다시 묻기가 실패해도 남지 않음
        fun on(next: InstallStep): InstallState {
            val stale = next == InstallStep.Canceled || next == InstallStep.Failed
            val lowered =
                when {
                    !stale -> check
                    check is StoreCheck.Downloading -> StoreCheck.Available(check.id)
                    check is StoreCheck.Downloaded -> StoreCheck.Available(check.id)
                    else -> check
                }
            return copy(check = lowered, step = next.takeUnless { it == InstallStep.Canceled }, liveEvents = liveEvents + 1)
        }

        // 늦은 답의 받기 상태는 쓰지 않되 버전을 모르면 배움. 그래야 뒤이은 실패·취소를 그 버전으로 알림
        fun learn(result: StoreCheck): InstallState {
            val id =
                when (result) {
                    is StoreCheck.Available -> result.id
                    is StoreCheck.Downloading -> result.id
                    is StoreCheck.Downloaded -> result.id
                    StoreCheck.None -> null
                }
            return if (check == StoreCheck.None && id != null) copy(check = StoreCheck.Available(id)) else this
        }

        // 다시 물은 값이 받기 상태까지 알려 줌. 실시간 알림이 끊긴 채 멈췄어도 돌아오면 풀림
        // 실패는 그 버전의 일. 그 버전을 아직 받을 수 있으면 다시 받기를 누를 때까지 둠
        fun settle(result: StoreCheck): InstallState {
            val failedId = (check as? StoreCheck.Available)?.id?.takeIf { step == InstallStep.Failed }
            val keepFailure = result is StoreCheck.Available && result.id == failedId
            return copy(check = result, step = step.takeIf { keepFailure })
        }
    }

    companion object {
        /** 이보다 낮은 앱은 카드를 닫을 수 없음(`docs/release.md` 새 버전 안내 원격 값) */
        const val KEY_MIN_VERSION = "update_min_version"

        /** 업데이트 요약이 가리키는 버전. 지금 앱보다 높을 때만 씀 */
        const val KEY_NOTE_VERSION = "update_note_version"
        const val KEY_NOTE_PREFIX = "update_note_"
        val NOTE_LANGUAGES = listOf("ko", "en", "ja", "zh-Hant", "es", "pt", "de", "fr")

        /** 화면 언어의 요약 키. 원격 설정 키에 하이픈을 쓸 수 없어 밑줄로(update_note_zh_Hant) */
        fun noteKey(language: String): String = KEY_NOTE_PREFIX + language.replace('-', '_')

        private val CHECK_TIMEOUT = 10.seconds
    }
}

/** 화면에서 바로 쓰는 진행률 퍼센트. 모르면 null */
fun UpdateNotice.Downloading.percent(): Int? = progress?.let { (it.coerceIn(0f, 1f) * PERCENT).toInt() }

private const val PERCENT = 100
