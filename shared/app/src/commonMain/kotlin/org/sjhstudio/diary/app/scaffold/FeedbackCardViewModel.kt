package org.sjhstudio.diary.app.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.platform.deviceInfo
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.rememberOpenLink
import org.sjhstudio.diary.core.ui.stateInSafely
import org.sjhstudio.diary.feature.home.FeedbackCard
import org.sjhstudio.diary.feature.settings.FEATURE_SURVEY_URL
import org.sjhstudio.diary.feature.settings.rememberFeedbackLink

/** 홈 의견 카드 상태(스펙 007). 언제 보일지는 [PromptRepository]가 정함 */
class FeedbackCardViewModel(private val prompts: PromptRepository) : ViewModel() {
    val visible: StateFlow<Boolean> = prompts.feedbackCard.stateInSafely(viewModelScope, WhileUiSubscribed, false)

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
    if (!visible) return
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
