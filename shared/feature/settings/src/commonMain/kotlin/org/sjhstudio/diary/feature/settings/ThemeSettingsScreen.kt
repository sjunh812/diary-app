package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.settings_theme
import org.sjhstudio.diary.feature.settings.resources.theme_note

/**
 * 화면 모드(Figma "화면 모드 (G3)")
 * 고르면 바로 바뀜
 */
@Composable
fun ThemeSettingsScreen(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.settings_theme), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            for (mode in ThemeMode.entries) {
                DiarySettingsChoiceRow(stringResource(mode.label()), selected = selected == mode, onClick = { onSelect(mode) })
            }
            BasicText(
                stringResource(Res.string.theme_note),
                style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
                modifier = Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2)
            )
        }
    }
}
