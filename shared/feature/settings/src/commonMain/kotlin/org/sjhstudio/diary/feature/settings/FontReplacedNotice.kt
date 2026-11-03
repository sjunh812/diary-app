package org.sjhstudio.diary.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryToastResult
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.font_replaced_change
import org.sjhstudio.diary.feature.settings.resources.font_replaced_notice

/** 옛 글꼴이 새 앱에 없어 다른 글꼴로 옮겼으면 그 글꼴을 한 번만 내준다. */
class FontReplacedNoticeViewModel(private val settings: SettingsRepository) : ViewModel() {
    /** 알릴 것이 있으면 지금 글꼴을 내주고 표시를 지운다. 다시 부르면 null이다. */
    suspend fun take(): DiaryFont? {
        val current = settings.settings.first()
        if (current.fontReplacedFrom == null) return null
        settings.clearFontReplacedNotice()
        return current.font
    }
}

/**
 * "쓰던 글꼴 대신 ○○ 글꼴로 보여 드려요" 토스트(Figma 프로토타입 "H1 홈 · 글꼴이 바뀌었어요", `docs/design.md` 일기 본문 글꼴).
 * 이전 뒤 앱 틀이 처음 뜰 때 한 번만 띄우고, "바꾸기"를 누르면 [onChange]로 글꼴 화면에 간다.
 * 띄운 뒤에는 누르지 않아도 다시 띄우지 않는다.
 */
@Composable
fun FontReplacedNotice(toast: DiaryToastState, onChange: () -> Unit) {
    val viewModel = koinViewModel<FontReplacedNoticeViewModel>()
    LaunchedEffect(viewModel) {
        val font = viewModel.take() ?: return@LaunchedEffect
        val message = getString(Res.string.font_replaced_notice, getString(font.label()))
        if (toast.show(message, getString(Res.string.font_replaced_change)) == DiaryToastResult.ActionPerformed) onChange()
    }
}
