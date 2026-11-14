package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.settings_list_style

/**
 * 목록 보기(FR-018)
 * 고르면 바로 바뀌고 기억함
 */
@Composable
fun ListStyleSettingsScreen(selected: ListStyle, onSelect: (ListStyle) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(DiaryTheme.colors.canvas).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DiaryTopBar(title = stringResource(Res.string.settings_list_style), onBack = onBack)
        // 글자를 키우거나 가로로 두면 줄이 화면을 넘을 수 있음
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            for (style in ListStyle.entries) {
                DiarySettingsChoiceRow(stringResource(style.label()), selected = selected == style, onClick = { onSelect(style) })
            }
        }
    }
}
