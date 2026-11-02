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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.migration_report_title
import org.sjhstudio.diary.feature.settings.resources.settings_design_catalog
import org.sjhstudio.diary.feature.settings.resources.settings_group_developer
import org.sjhstudio.diary.feature.settings.resources.settings_group_info
import org.sjhstudio.diary.feature.settings.resources.settings_group_security
import org.sjhstudio.diary.feature.settings.resources.settings_group_style
import org.sjhstudio.diary.feature.settings.resources.settings_lock
import org.sjhstudio.diary.feature.settings.resources.settings_text_size
import org.sjhstudio.diary.feature.settings.resources.settings_theme
import org.sjhstudio.diary.feature.settings.resources.settings_title

/**
 * 설정 목록(Figma "설정 (S6)"). 앱 틀 맨 위 화면이라 상단 바 대신 큰 제목을 쓴다.
 * 스펙 001이 만든 줄만 둔다. 백업(스펙 003), 휴지통·목록 모양(스펙 002), 글꼴·매일 알림·고지(스펙 001 T068·T069)는 그 화면과 함께 더한다.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTextSize: () -> Unit,
    onTheme: () -> Unit,
    onLock: () -> Unit,
    onMigrationReport: () -> Unit,
    modifier: Modifier = Modifier,
    /** 개발 빌드에만 있는 디자인 시스템 견본(FR-029). 출시 빌드는 null이라 줄이 없다. */
    onDesignCatalog: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = DiarySpacing.x14 * 8 + DiarySpacing.x8).fillMaxWidth().verticalScroll(rememberScrollState())) {
            BasicText(
                stringResource(Res.string.settings_title),
                style = DiaryTheme.typography.display.copy(color = colors.textPrimary),
                modifier =
                    Modifier
                        .padding(
                            start = DiarySpacing.screenCompact,
                            end = DiarySpacing.screenCompact,
                            top =
                                DiarySpacing.x14 + DiarySpacing.x2
                        )
                        .semantics { heading() }
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_style))
            DiarySettingsRow(
                stringResource(Res.string.settings_text_size),
                onClick = onTextSize,
                value = stringResource(state.settings.bodyTextSize.label())
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_theme),
                onClick = onTheme,
                value = stringResource(state.settings.themeMode.label())
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_security))
            DiarySettingsRow(stringResource(Res.string.settings_lock), onClick = onLock, value = stringResource(state.lock.label()))
            if (state.showsMigrationReport) {
                DiarySettingsHeader(stringResource(Res.string.settings_group_info))
                DiarySettingsRow(stringResource(Res.string.migration_report_title), onClick = onMigrationReport)
            }
            if (onDesignCatalog != null) {
                DiarySettingsHeader(stringResource(Res.string.settings_group_developer))
                DiarySettingsRow(stringResource(Res.string.settings_design_catalog), onClick = onDesignCatalog)
            }
        }
    }
}
