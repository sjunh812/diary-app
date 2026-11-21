package org.sjhstudio.diary.app.scaffold

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.data.AppUpdateRepository
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.feature.home.UpdateCard

/**
 * 새 버전 카드와 설정 앱 버전 줄에서 누르는 일(스펙 009)
 * 언제 보일지는 [AppUpdateRepository]와 [HomeTopState]가 정함
 */
class UpdateCardViewModel(private val updates: AppUpdateRepository) : ViewModel() {
    val notice = updates.notice

    /** [업데이트]와 설정 줄. 받은 뒤라면 다시 시작 */
    fun act() {
        if (notice.value is UpdateNotice.Downloaded) {
            updates.restart()
        } else {
            viewModelScope.launchSafely { updates.start() }
        }
    }

    fun later() {
        viewModelScope.launchSafely { updates.dismiss() }
    }

    fun restart() = updates.restart()

    fun openStorePage() = updates.openStorePage()
}

/** 홈 목록 맨 위 새 버전 카드. [HomeTop.Update]일 때만 부름 */
@Composable
internal fun HomeUpdateCard(notice: UpdateNotice) {
    val viewModel = koinViewModel<UpdateCardViewModel>()
    UpdateCard(notice, onUpdate = viewModel::act, onLater = viewModel::later, onRestart = viewModel::restart)
}
