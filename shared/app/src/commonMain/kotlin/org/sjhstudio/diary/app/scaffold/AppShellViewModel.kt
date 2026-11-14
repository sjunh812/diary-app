package org.sjhstudio.diary.app.scaffold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.app.AppRootViewModel
import org.sjhstudio.diary.app.ads.AdRemovalViewModel
import org.sjhstudio.diary.app.navigation.CalendarKey
import org.sjhstudio.diary.app.navigation.EntryDetailKey
import org.sjhstudio.diary.app.navigation.HomeKey
import org.sjhstudio.diary.app.navigation.TabScreen
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.data.PlanRepository
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

    suspend fun backToList()

    /** 이번 광고 앞에 첫 안내를 띄울지. 참이면 보여 준 것으로 적음 */
    suspend fun takeNotice(): Boolean = false
}

/**
 * 앱 틀이 하던 업무 규칙(광고 차례, 화면 통계, 휴지통 되돌리기, 홈 쓰기 버튼)을 맡는 화면 모델
 * 돈과 관련된 규칙이라 화면 밖에서 시험
 */
class AppShellViewModel(
    private val entries: EntryRepository,
    plans: PlanRepository,
    private val ads: AfterSaveAds,
    private val screen: (String) -> Unit
) : ViewModel() {
    private val adTurn = AfterSaveAdTurn()

    /** 일기가 있는지. 읽기 전에는 null(그동안 홈 쓰기 버튼을 숨김) */
    val hasEntries: StateFlow<Boolean?> =
        entries.months().map { it.isNotEmpty() }.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    val plan: StateFlow<Plan> = plans.plan

    private val _adNotice = MutableStateFlow(AdNotice.None)

    /** 첫 전면 광고 앞 안내(`docs/business.md` 광고 자리) */
    val adNotice: StateFlow<AdNotice> = _adNotice.asStateFlow()

    /** 보는 화면이 바뀜. 통계에는 화면 키 이름만 */
    fun screenShown(top: NavKey, tab: TabScreen, backStack: List<NavKey>) {
        screen(top.toString())
        takeAdTurn(tab, backStack, justSavedInTwoPane = false)
    }

    /**
     * 일기를 저장함. 광고를 미리 받고, 같은 탭 목록으로 돌아오면 띄움
     * 두 칸인지는 저장한 순간의 화면으로만 정함. 폴더블을 나중에 펴거나 접어도 차례가 새로 생기지 않음
     */
    fun saved(tab: TabScreen, backStack: List<NavKey>, twoPane: Boolean) {
        adTurn.saved(tab)
        viewModelScope.launchSafely { ads.prepare() }
        takeAdTurn(tab, backStack, justSavedInTwoPane = twoPane)
    }

    /** 휴지통으로 옮긴 일기 되돌리기 */
    fun restoreTrashed(id: String) {
        viewModelScope.launchWrite { entries.restore(id) }
    }

    private fun takeAdTurn(tab: TabScreen, backStack: List<NavKey>, justSavedInTwoPane: Boolean) {
        if (!adTurn.takeTurn(tab, backStack, justSavedInTwoPane)) return
        viewModelScope.launchSafely { if (ads.takeNotice()) _adNotice.value = AdNotice.Showing else ads.backToList() }
    }

    /** 안내에서 [확인]이나 바깥. 광고를 띄움 */
    fun confirmAdNotice() = continueAdTurn(from = AdNotice.Showing)

    /** 안내에서 [광고 없애기]. 시트는 화면이 열고, 닫힐 때까지 광고를 미룸 */
    fun removeAdsFirst() {
        if (_adNotice.value == AdNotice.Showing) _adNotice.value = AdNotice.AwaitingRemoval
    }

    /**
     * 광고 제거 시트가 닫힘. 광고 차례를 이음
     * 사거나 영상을 봤으면 광고 차례가 없어 뜨지 않음
     */
    fun adRemovalClosed() = continueAdTurn(from = AdNotice.AwaitingRemoval)

    private fun continueAdTurn(from: AdNotice) {
        if (_adNotice.value != from) return
        _adNotice.value = AdNotice.None
        viewModelScope.launchSafely { ads.backToList() }
    }
}

/** 앱 틀 화면 모델 등록. 앱과 화면 시험이 함께 씀 */
val appShellModule =
    module {
        viewModel { AppRootViewModel(get(), get(), get()) }
        viewModel {
            val ads = get<AdRepository>()
            AppShellViewModel(
                entries = get(),
                plans = get(),
                ads = object : AfterSaveAds {
                    override suspend fun prepare() = ads.afterSave()

                    override suspend fun backToList() = ads.backToList()

                    override suspend fun takeNotice() = ads.takeInterstitialNotice()
                },
                screen = get<Telemetry>()::screen
            )
        }
        viewModel { AdRemovalViewModel(get<AdRepository>()::watchReward) }
    }

/**
 * 저장 뒤 전면 광고를 띄워도 되는 자리. 홈과 달력 목록으로 돌아왔을 때만
 * 두 칸에서 막 저장했으면 목록 옆에 그 일기 상세가 열려 목록이 보이는 그때가 목록으로 돌아온 때. 상세를 닫길 기다리면 거의 뜨지 않음
 * 넓은 화면은 상세를 보다 레일로 설정에 갈 수 있는데 설정에는 광고가 없어(헌법 III) 다음 목록까지 미룸
 */
internal fun interstitialMayShow(backStack: List<NavKey>, justSavedInTwoPane: Boolean): Boolean {
    val list = backStack.firstOrNull()
    if (list != HomeKey && list != CalendarKey) return false
    return backStack.size == 1 || (justSavedInTwoPane && backStack.size == 2 && backStack.last() is EntryDetailKey)
}

/**
 * 저장 뒤 전면 광고 차례. 저장한 탭을 기억했다가 같은 탭 목록으로 돌아오면 한 번
 * 다른 탭으로 가면 기회를 버림. 몇 시간 뒤 탭을 누를 때 뜨면 "저장하고 목록으로 돌아갈 때"가 아님
 */
internal class AfterSaveAdTurn {
    /** 기다리는 탭 */
    var savedOn: TabScreen? = null
        private set

    fun saved(tab: TabScreen) {
        savedOn = tab
    }

    /** 화면이 바뀔 때마다 부름. 참이면 지금 띄우고 차례는 끝 */
    fun takeTurn(tab: TabScreen, backStack: List<NavKey>, justSavedInTwoPane: Boolean): Boolean {
        val pending = savedOn
        val show = pending == tab && interstitialMayShow(backStack, justSavedInTwoPane)
        if (pending != tab || show) savedOn = null
        return show
    }
}
