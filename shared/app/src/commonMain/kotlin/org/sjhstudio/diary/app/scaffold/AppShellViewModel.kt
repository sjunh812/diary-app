package org.sjhstudio.diary.app.scaffold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.app.AppRootViewModel
import org.sjhstudio.diary.app.ads.AdRemovalViewModel
import org.sjhstudio.diary.app.lock.LockController
import org.sjhstudio.diary.app.lock.LockState
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.WriteKey
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/** 첫 전면 광고 앞 안내 단계 */
enum class AdNotice {
    None,
    Showing,

    /** [광고 없애기]로 연 시트가 닫히길 기다림. 닫히면 광고 차례를 이음 */
    AwaitingRemoval
}

/** 저장 뒤 전면 광고 준비·띄우기. 하루 한 번은 [org.sjhstudio.diary.core.data.AdRepository]가 봄 */
interface AfterSaveAds {
    suspend fun prepare()

    /** 전면 광고 차례였으면 참 */
    suspend fun show(): Boolean

    /** 이번 광고 앞에 첫 안내를 띄울지 */
    suspend fun noticeDue(): Boolean = false

    /** 안내에 답함. 그 뒤로 안내를 띄우지 않음 */
    suspend fun noticeAnswered() = Unit
}

/**
 * 앱 틀이 하던 업무 규칙(광고 차례, 리뷰 창, 화면 통계, 휴지통 되돌리기, 홈 쓰기 버튼)을 맡는 화면 모델
 * 돈과 관련된 규칙이라 화면 밖에서 시험
 */
class AppShellViewModel(
    private val entries: EntryRepository,
    plans: PlanRepository,
    private val ads: AfterSaveAds,
    locked: Flow<Boolean>,
    private val askReview: suspend () -> Unit,
    private val screen: (String) -> Unit
) : ViewModel() {
    /** 일기가 있는지. 읽기 전에는 null(그동안 홈 쓰기 버튼을 숨김) */
    val hasEntries: StateFlow<Boolean?> =
        entries.months().map { it.isNotEmpty() }.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    /** 확정된 요금제. 재설치 직후 스토어 답이 오기 전에는 null */
    val plan: StateFlow<Plan?> = plans.confirmed.stateInSafely(viewModelScope, SharingStarted.Eagerly, plans.confirmedNow)

    private val _adNotice = MutableStateFlow(AdNotice.None)

    init {
        // 안내나 광고 제거 시트를 둔 채 잠기면 그 차례는 광고 없이 끝냄. 잠금을 풀자마자 광고가 뜨지 않게
        viewModelScope.launchSafely { locked.filter { it }.collect { _adNotice.value = AdNotice.None } }
    }

    /** 저장 뒤 차례. 저장한 일기 상세를 떠나면 리뷰 요청만 거둠 */
    private var afterSave: Job? = null
    private var savedEntry: NavKey? = null

    /** 첫 전면 광고 앞 안내(`docs/business.md` 광고 자리) */
    val adNotice: StateFlow<AdNotice> = _adNotice.asStateFlow()

    /** 보는 화면이 바뀜. 통계에는 화면 키 이름만 */
    fun screenShown(top: NavKey) {
        screen(top.toString())
        // 쓰는 동안 받아 둠. 저장한 순간 바로 띄워야 하고, 받아 둔 광고는 1시간이 지나면 버려짐
        if (top is WriteKey) viewModelScope.launchSafely { ads.prepare() }
        // 저장한 일기 상세를 떠나면 리뷰를 묻지 않음(스펙 007 SC-003, FR-009)
        if (top != savedEntry) afterSave?.cancel()
    }

    /**
     * 일기를 저장해 작성 화면이 닫히고 [entryId] 상세가 열림. 새 일기와 수정 모두
     * 오늘 차례면 전면 광고, 아니면 리뷰 창(스펙 007 FR-007)
     */
    fun saved(entryId: String) {
        savedEntry = EntryDetailKey(entryId)
        afterSave =
            viewModelScope.launchSafely {
                // 광고 차례는 화면을 옮겨도 끝까지 감. 하루 한 번 기록이 걸림
                val adTurn =
                    withContext(NonCancellable) {
                        if (ads.noticeDue()) {
                            _adNotice.value = AdNotice.Showing
                            true
                        } else {
                            ads.show()
                        }
                    }
                if (!adTurn) {
                    ensureActive()
                    askReview()
                }
            }
    }

    /** 휴지통으로 옮긴 일기 되돌리기 */
    fun restoreTrashed(id: String) {
        viewModelScope.launchWrite { entries.restore(id) }
    }

    /** 안내에서 [확인]이나 바깥. 광고를 띄움 */
    fun confirmAdNotice() {
        if (!answerNotice(next = AdNotice.None)) return
        viewModelScope.launchSafely { ads.show() }
    }

    /** 안내에서 [광고 없애기]. 시트는 화면이 열고, 닫힐 때까지 광고를 미룸 */
    fun removeAdsFirst() {
        answerNotice(next = AdNotice.AwaitingRemoval)
    }

    /**
     * 광고 제거 시트가 닫힘. 광고 차례를 이음
     * 사거나 영상을 봤으면 광고 차례가 없어 뜨지 않음
     */
    fun adRemovalClosed() {
        if (_adNotice.value != AdNotice.AwaitingRemoval) return
        _adNotice.value = AdNotice.None
        viewModelScope.launchSafely { ads.show() }
    }

    // 버튼을 눌렀을 때만 안내를 본 것으로 적음. 답하기 전에 잠기면 다음 저장에 다시 띄움
    private fun answerNotice(next: AdNotice): Boolean {
        if (_adNotice.value != AdNotice.Showing) return false
        _adNotice.value = next
        viewModelScope.launchSafely { ads.noticeAnswered() }
        return true
    }
}

/** 앱 틀 화면 모델 등록. 앱과 화면 시험이 함께 씀 */
val appShellModule =
    module {
        viewModel { AppRootViewModel(get(), get(), get(), get()) }
        viewModel {
            val ads = get<AdRepository>()
            AppShellViewModel(
                entries = get(),
                plans = get(),
                locked = get<LockController>().appLocked,
                ads = object : AfterSaveAds {
                    override suspend fun prepare() = ads.prepareAfterSave()

                    override suspend fun show() = ads.showAfterSave()

                    override suspend fun noticeDue() = ads.interstitialNoticeDue()

                    override suspend fun noticeAnswered() = ads.interstitialNoticeAnswered()
                },
                askReview = get<PromptRepository>()::askReviewIfDue,
                screen = get<Telemetry>()::screen
            )
        }
        viewModel {
            AdRemovalViewModel(get<LockController>().appLocked, get<AdRepository>()::watchReward)
        }
        viewModel { FeedbackCardViewModel(get(), get()) }
    }

/** 잠금 화면이 앱을 덮은 동안 참 */
private val LockController.appLocked: Flow<Boolean>
    get() = state.map { it == LockState.Locked || it == LockState.NeedsNewPin }
