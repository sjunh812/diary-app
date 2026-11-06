package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryInviteCard
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.migration_report_title
import org.sjhstudio.diary.feature.settings.resources.reminder_value_off
import org.sjhstudio.diary.feature.settings.resources.settings_backup
import org.sjhstudio.diary.feature.settings.resources.settings_font
import org.sjhstudio.diary.feature.settings.resources.settings_group_diary
import org.sjhstudio.diary.feature.settings.resources.settings_group_info
import org.sjhstudio.diary.feature.settings.resources.settings_group_notification_security
import org.sjhstudio.diary.feature.settings.resources.settings_group_style
import org.sjhstudio.diary.feature.settings.resources.settings_letter
import org.sjhstudio.diary.feature.settings.resources.settings_licenses
import org.sjhstudio.diary.feature.settings.resources.settings_list_style
import org.sjhstudio.diary.feature.settings.resources.settings_lock
import org.sjhstudio.diary.feature.settings.resources.settings_plus
import org.sjhstudio.diary.feature.settings.resources.settings_plus_action
import org.sjhstudio.diary.feature.settings.resources.settings_plus_active
import org.sjhstudio.diary.feature.settings.resources.settings_plus_body
import org.sjhstudio.diary.feature.settings.resources.settings_plus_title
import org.sjhstudio.diary.feature.settings.resources.settings_reminder
import org.sjhstudio.diary.feature.settings.resources.settings_text_size
import org.sjhstudio.diary.feature.settings.resources.settings_theme
import org.sjhstudio.diary.feature.settings.resources.settings_title
import org.sjhstudio.diary.feature.settings.resources.settings_trash
import org.sjhstudio.diary.feature.settings.resources.settings_trash_count

/**
 * 설정 목록(Figma "설정 (S6)"). 앱 틀 맨 위 화면이라 상단 바 대신 큰 제목을 쓴다.
 * 백업(스펙 003)과 목록 모양은 그 화면과 함께 더한다.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onFont: () -> Unit,
    onTextSize: () -> Unit,
    onTheme: () -> Unit,
    onReminder: () -> Unit,
    onLock: () -> Unit,
    onMigrationReport: () -> Unit,
    onLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    onListStyle: () -> Unit = {},
    onTrash: () -> Unit = {},
    trashCount: Int = 0,
    onBackup: () -> Unit = {},
    onLetter: () -> Unit = {},
    onPlus: () -> Unit = {},
    plusActive: Boolean = false,
    backupValue: String? = null,
    /** 개발 빌드에만 있는 디자인 시스템 견본(FR-029). 출시 빌드는 null이라 줄이 없다. */
    onDesignCatalog: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    // 폰은 탭 바가 시스템 내비게이션 높이까지 맡고, 앱 틀이 그만큼 다 썼다고 알려서 아래가 두 번 뜨지 않는다.
    // 탭 바가 없는 넓은 화면에서만 마지막 줄 아래를 시스템 내비게이션 높이만큼 비운다.
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.settingsMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = DiarySpacing.x6)
        ) {
            BasicText(
                stringResource(Res.string.settings_title),
                style = DiaryTheme.typography.display.copy(color = colors.textPrimary),
                modifier =
                    Modifier
                        .padding(
                            start = DiarySpacing.screenCompact,
                            end = DiarySpacing.screenCompact,
                            top = DiarySpacing.tabTitleTop
                        )
                        .semantics { heading() }
            )
            if (plusActive) {
                DiarySettingsRow(
                    stringResource(Res.string.settings_plus),
                    onClick = onPlus,
                    value = stringResource(Res.string.settings_plus_active),
                    leadingIcon = DiaryIcons.Sparkles
                )
            } else {
                // 설정은 살 마음이 생겼을 때 찾아오는 곳이라 첫자리에 혜택을 말하는 카드로 둔다.
                DiaryInviteCard(
                    stringResource(Res.string.settings_plus_title),
                    stringResource(Res.string.settings_plus_body),
                    stringResource(Res.string.settings_plus_action),
                    onPlus,
                    Modifier.padding(start = LocalScreenGutter.current, end = LocalScreenGutter.current, top = DiarySpacing.x4)
                )
            }
            DiarySettingsHeader(stringResource(Res.string.settings_group_diary))
            DiarySettingsRow(stringResource(Res.string.settings_backup), onClick = onBackup, value = backupValue)
            DiarySettingsRow(
                stringResource(Res.string.settings_trash),
                onClick = onTrash,
                value = if (trashCount > 0) pluralStringResource(Res.plurals.settings_trash_count, trashCount, trashCount) else null
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_style), gapAbove = true)
            DiarySettingsRow(
                stringResource(Res.string.settings_font),
                onClick = onFont,
                value = stringResource(state.settings.font.label())
            )
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
            DiarySettingsRow(
                stringResource(Res.string.settings_list_style),
                onClick = onListStyle,
                value = stringResource(state.settings.list.listStyle.label())
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_notification_security), gapAbove = true)
            val reminderValue =
                if (state.settings.reminderEnabled) {
                    val minute = state.settings.reminderMinuteOfDay
                    rememberDiaryDateFormat().time(LocalTime(minute / 60, minute % 60))
                } else {
                    stringResource(Res.string.reminder_value_off)
                }
            DiarySettingsRow(stringResource(Res.string.settings_reminder), onClick = onReminder, value = reminderValue)
            DiarySettingsRow(stringResource(Res.string.settings_lock), onClick = onLock, value = stringResource(state.lock.label()))
            DiarySettingsHeader(stringResource(Res.string.settings_group_info), gapAbove = true)
            if (state.showsMigrationReport) {
                DiarySettingsRow(stringResource(Res.string.migration_report_title), onClick = onMigrationReport)
            }
            DiarySettingsRow(stringResource(Res.string.settings_letter), onClick = onLetter)
            DiarySettingsRow(stringResource(Res.string.settings_licenses), onClick = onLicenses)
        }
    }
}
