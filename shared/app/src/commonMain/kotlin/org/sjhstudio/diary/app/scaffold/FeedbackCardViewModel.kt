package org.sjhstudio.diary.app.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.platform.deviceInfo
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.rememberOpenLink
import org.sjhstudio.diary.feature.home.FeedbackCard
import org.sjhstudio.diary.feature.settings.FEATURE_SURVEY_URL
import org.sjhstudio.diary.feature.settings.rememberFeedbackLink

/**
 * 홈 맨 위 의견 카드를 보일지. 읽기 전 null
 * 앱 범위에 두어 탭을 오가도 다시 null이 되지 않음. 화면마다 새로 읽으면 목록 보기 팁이 먼저 보였다가 카드로 바뀜
 * 홈이 처음 읽을 때 시작. 일기 DB를 세므로 기록 이전 관문 전에 열면 이전이 실패함
 */
class FeedbackCardState(prompts: PromptRepository, scope: CoroutineScope) {
    val visible: StateFlow<Boolean?> = prompts.feedbackCard.stateIn(scope, SharingStarted.Lazily, null)
}

/** 홈 의견 카드 상태(스펙 007). 언제 보일지는 [PromptRepository]가 정함 */
class FeedbackCardViewModel(private val prompts: PromptRepository, state: FeedbackCardState) : ViewModel() {
    val visible: StateFlow<Boolean?> = state.visible

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
 * 홈 목록 맨 위 의견 카드. 조건이 맞을 때만 그림
 * 설문을 못 열면 문의 메일, 그것도 못 열면 [onNoMail]로 메일 주소를 알림(설정 편지와 같은 순서)
 */
@Composable
internal fun HomeFeedbackCard(onNoMail: (String) -> Unit) {
    val viewModel = koinViewModel<FeedbackCardViewModel>()
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    if (visible != true) return
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
