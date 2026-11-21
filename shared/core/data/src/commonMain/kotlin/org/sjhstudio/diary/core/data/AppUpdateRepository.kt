package org.sjhstudio.diary.core.data

import kotlin.time.Clock
import kotlin.time.Duration
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val check = MutableStateFlow<StoreCheck>(StoreCheck.None)
    private val step = MutableStateFlow<InstallStep?>(null)
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
                .collect { step.value = it.takeUnless { it == InstallStep.Canceled } }
        }
    }

    /** 설정 줄이 보는 상태. 닫은 버전도 그대로 보임 */
    val notice: StateFlow<UpdateNotice> = combine(check, step, extras, ::noticeOf).stateIn(scope, SharingStarted.Eagerly, UpdateNotice.None)

    /**
     * 홈 맨 위 카드. 없으면 null
     * 처음 확인을 마친 뒤부터 알림. 그 전에는 백업 안내처럼 읽는 중
     * [나중에]로 닫은 버전은 꼭 필요하지 않으면 숨김. 받는 중·받음은 사용자가 시작한 일이라 닫았어도 보임
     * [notice]는 한 박자 늦게 따라와 원래 값에서 바로 정함. 늦은 값을 섞으면 빈 칸이 먼저 그려짐
     */
    val homeCard: Flow<UpdateNotice?> =
        flow {
            checked.first { it }
            emitAll(
                combine(
                    check,
                    step,
                    extras,
                    prompts.prefs.catch {
                        emit(PromptPrefs())
                    },
                    laterThisRun
                ) { check, step, extras, prefs, later ->
                    when (val notice = noticeOf(check, step, extras)) {
                        UpdateNotice.None -> null

                        is UpdateNotice.Available -> notice.takeIf {
                            notice.required ||
                                (check as? StoreCheck.Available)?.id != prefs.updateDismissed
                        }

                        UpdateNotice.Downloaded -> notice.takeUnless { later }

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
                settle(known)
                checked.value = true
                if (checkedAt != null && clock.now() - checkedAt < store.checkEvery) return@withLock
            }
            // 묻지 못하면 마지막으로 안 값을 그대로 둠
            val fresh = orElseOnFailure<StoreCheck?>(null) { store.check() } ?: return@withLock
            if (remembers) quietly { prompts.updateChecked((fresh as? StoreCheck.Available)?.id.orEmpty(), clock.now()) }
            settle(fresh)
        } finally {
            checked.value = true
        }
    }

    private fun noticeOf(check: StoreCheck, step: InstallStep?, extras: Extras): UpdateNotice = when {
        step is InstallStep.Downloading -> UpdateNotice.Downloading(step.progress)
        step == InstallStep.Downloaded -> UpdateNotice.Downloaded
        check is StoreCheck.Downloading -> UpdateNotice.Downloading(check.progress)
        check == StoreCheck.Downloaded -> UpdateNotice.Downloaded
        check is StoreCheck.Available -> UpdateNotice.Available(extras.required, extras.note, step == InstallStep.Failed)
        else -> UpdateNotice.None
    }

    private fun settle(result: StoreCheck) {
        // 받다 실패한 표시는 다시 받기를 누를 때까지 둠
        if (!(result is StoreCheck.Available && step.value == InstallStep.Failed)) step.value = null
        check.value = result
    }

    /** [업데이트]. 실패 표시를 지우고 스토어 창을 띄움 */
    suspend fun start(): Boolean {
        if (step.value == InstallStep.Failed) step.value = null
        return orElseOnFailure(false) { store.start() }
    }

    /** [다시 시작하기] */
    fun restart() = quietly { store.restart() }

    /** 복원이 새 버전을 요구할 때 */
    fun openStorePage() = quietly { store.openStorePage() }

    /** [나중에]. 그 버전은 다시 카드로 알리지 않음. 받은 뒤라면 이번 실행에서만 숨김 */
    suspend fun dismiss() {
        if (notice.value == UpdateNotice.Downloaded) {
            laterThisRun.value = true
            return
        }
        val id = (check.value as? StoreCheck.Available)?.id ?: return
        quietly { prompts.updateDismissed(id) }
    }

    private fun availableOrNone(known: String): StoreCheck =
        if (known.isNotEmpty() && AppVersion.isNewer(known, store.installed)) StoreCheck.Available(known) else StoreCheck.None

    private fun readExtras(): Extras {
        val minimum = remote.string(KEY_MIN_VERSION)
        val noteVersion = remote.string(KEY_NOTE_VERSION)
        val texts = NOTE_LANGUAGES.associateWith { remote.string(KEY_NOTE_PREFIX + it).trim() }.filterValues(String::isNotEmpty)
        return Extras(
            required = minimum.isNotBlank() && AppVersion.isNewer(minimum, versionName),
            note = ReleaseNote(texts).takeIf {
                noteVersion.isNotBlank() && AppVersion.isNewer(noteVersion, versionName) &&
                    texts.isNotEmpty()
            }
        )
    }

    private data class Extras(val required: Boolean = false, val note: ReleaseNote? = null)

    companion object {
        /** 이보다 낮은 앱은 카드를 닫을 수 없음(`docs/release.md` 새 버전 안내 원격 값) */
        const val KEY_MIN_VERSION = "update_min_version"

        /** 업데이트 요약이 가리키는 버전. 지금 앱보다 높을 때만 씀 */
        const val KEY_NOTE_VERSION = "update_note_version"
        const val KEY_NOTE_PREFIX = "update_note_"
        val NOTE_LANGUAGES = listOf("ko", "en", "ja")
    }
}

/** 화면에서 바로 쓰는 진행률 퍼센트. 모르면 null */
fun UpdateNotice.Downloading.percent(): Int? = progress?.let { (it.coerceIn(0f, 1f) * PERCENT).toInt() }

private const val PERCENT = 100
