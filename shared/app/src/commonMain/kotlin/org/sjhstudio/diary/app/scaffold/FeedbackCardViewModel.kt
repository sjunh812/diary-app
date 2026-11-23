package org.sjhstudio.diary.app.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.data.AppUpdateRepository
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.platform.deviceInfo
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.rememberOpenLink
import org.sjhstudio.diary.feature.home.FeedbackCard
import org.sjhstudio.diary.feature.settings.FEATURE_SURVEY_URL
import org.sjhstudio.diary.feature.settings.rememberFeedbackLink

/** 홈 맨 위 칸. 한 번에 한 장(스펙 007 FR-014) */
sealed interface HomeTop {
    /** 아직 읽는 중. 아무것도 그리지 않음 */
    data object Reading : HomeTop

    data class Backup(val nudge: BackupNudge) : HomeTop

    /** 새 버전(스펙 009). 받는 중에는 같은 카드가 바뀜 */
    data class Update(val notice: UpdateNotice) : HomeTop

    data object Feedback : HomeTop

    /** 카드 없음. 목록 보기 팁이 이 자리를 씀 */
    data object Empty : HomeTop

    /** 백업 안내를 닫았는데 다음 카드를 아직 모름. 그리지는 않지만 정해진 칸이라 다음 카드가 움직이며 들어옴 */
    data object Waiting : HomeTop
}

/**
 * 홈 맨 위 칸을 한 곳에서 정함. 기록을 지키는 백업 안내 > 새 버전 > 의견
 * 앞 순위를 다 읽은 뒤에 정해서 읽는 동안 다른 카드가 잠깐 그려지지 않음
 * 잠깐 그려진 카드도 본 것으로 적혀 리뷰 창을 막음
 * 앱 범위에 두어 탭을 오가도 다시 읽는 중이 되지 않음
 */
class HomeTopState(
    private val backup: BackupRepository,
    private val prompts: PromptRepository,
    private val updates: AppUpdateRepository,
    private val scope: CoroutineScope,
    /** 복원한 사진을 받는 중. 그동안 연결은 받기가 끝날 때까지 기다려 백업 안내를 띄우지 않음 */
    private val receivingPhotos: Flow<Boolean> = flowOf(false)
) {
    private val read = MutableStateFlow<HomeTop>(HomeTop.Reading)
    val top: StateFlow<HomeTop> = read.asStateFlow()
    private var reading: Job? = null

    /**
     * 기록 이전 관문을 지난 뒤 한 번 읽기 시작. 홈보다 먼저라 첫 화면에 맞음
     * 일기 DB를 세므로 관문 전에 부르면 이전이 실패함
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (reading != null) return
        reading =
            scope.launch {
                // 새 버전과 의견은 한 번 읽어 두고 함께 씀. 백업 안내를 닫으면 다시 읽지 않고 바로 다음 카드. 모르면 null
                val next =
                    combine(updates.homeCard, prompts.feedbackCard) { update, card ->
                        when {
                            update != null -> HomeTop.Update(update)
                            card -> HomeTop.Feedback
                            else -> HomeTop.Empty
                        }
                    }.stateIn(this, SharingStarted.Eagerly, null)
                // 백업 안내는 새 버전 확인(iOS는 네트워크)을 기다리지 않음
                combine(backup.nudge, receivingPhotos) { nudge, receiving -> nudge.takeUnless { receiving } }
                    .flatMapLatest { nudge -> if (nudge != null) flowOf(HomeTop.Backup(nudge)) else next.map { it ?: pending() } }
                    .collect { read.value = it }
            }
    }

    // 앱을 켠 뒤 처음이면 읽는 중, 백업 안내를 닫은 뒤면 닫은 카드를 남기지 않고 정해진 빈칸
    private fun pending(): HomeTop = if (read.value == HomeTop.Reading) HomeTop.Reading else HomeTop.Waiting
}

/** 홈 의견 카드에서 누르는 일(스펙 007). 언제 보일지는 [HomeTopState]가 정함 */
class FeedbackCardViewModel(private val prompts: PromptRepository) : ViewModel() {
    /** 화면에 나옴. 같은 날 리뷰 창을 막음 */
    fun shown() {
        viewModelScope.launchSafely { prompts.feedbackShown() }
    }

    fun close() {
        viewModelScope.launchSafely { prompts.feedbackClosed() }
    }

    fun surveyOpened() {
        viewModelScope.launchSafely { prompts.surveyOpened() }
    }
}

/**
 * 홈 목록 맨 위 의견 카드. [HomeTop.Feedback]일 때만 부름
 * 설문을 못 열면 문의 메일, 그것도 못 열면 [onNoMail]로 메일 주소를 알림(설정 편지와 같은 순서)
 */
@Composable
internal fun HomeFeedbackCard(onNoMail: (String) -> Unit) {
    val viewModel = koinViewModel<FeedbackCardViewModel>()
    val openLink = rememberOpenLink()
    val app = koinInject<BackupAppInfo>()
    val device = remember { deviceInfo() }
    val feedback = rememberFeedbackLink(app.versionName, device.os, device.model)
    LaunchedEffect(Unit) { viewModel.shown() }
    FeedbackCard(
        onSend = {
            viewModel.surveyOpened()
            if (!openLink(FEATURE_SURVEY_URL) && !openLink(feedback.url)) onNoMail(feedback.noMail)
        },
        onClose = viewModel::close
    )
}
