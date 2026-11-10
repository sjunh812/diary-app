package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryInviteCard
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsValueRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTabTopBar
import org.sjhstudio.diary.core.designsystem.component.SnapTitleOnRest
import org.sjhstudio.diary.core.designsystem.component.titleCollapse
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.OnTabReselected
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.migration_report_title
import org.sjhstudio.diary.feature.settings.resources.reminder_value_off
import org.sjhstudio.diary.feature.settings.resources.settings_backup
import org.sjhstudio.diary.feature.settings.resources.settings_dev_mode
import org.sjhstudio.diary.feature.settings.resources.settings_feedback
import org.sjhstudio.diary.feature.settings.resources.settings_font
import org.sjhstudio.diary.feature.settings.resources.settings_group_diary
import org.sjhstudio.diary.feature.settings.resources.settings_group_feedback
import org.sjhstudio.diary.feature.settings.resources.settings_group_info
import org.sjhstudio.diary.feature.settings.resources.settings_group_notification_security
import org.sjhstudio.diary.feature.settings.resources.settings_group_style
import org.sjhstudio.diary.feature.settings.resources.settings_letter
import org.sjhstudio.diary.feature.settings.resources.settings_licenses
import org.sjhstudio.diary.feature.settings.resources.settings_list_style
import org.sjhstudio.diary.feature.settings.resources.settings_lock
import org.sjhstudio.diary.feature.settings.resources.settings_plus
import org.sjhstudio.diary.feature.settings.resources.settings_plus_action
import org.sjhstudio.diary.feature.settings.resources.settings_plus_badge
import org.sjhstudio.diary.feature.settings.resources.settings_plus_body
import org.sjhstudio.diary.feature.settings.resources.settings_plus_title
import org.sjhstudio.diary.feature.settings.resources.settings_plus_title_ad_free
import org.sjhstudio.diary.feature.settings.resources.settings_privacy
import org.sjhstudio.diary.feature.settings.resources.settings_rate
import org.sjhstudio.diary.feature.settings.resources.settings_reminder
import org.sjhstudio.diary.feature.settings.resources.settings_terms
import org.sjhstudio.diary.feature.settings.resources.settings_text_size
import org.sjhstudio.diary.feature.settings.resources.settings_theme
import org.sjhstudio.diary.feature.settings.resources.settings_title
import org.sjhstudio.diary.feature.settings.resources.settings_trash
import org.sjhstudio.diary.feature.settings.resources.settings_trash_count
import org.sjhstudio.diary.feature.settings.resources.settings_version

/**
 * 설정 목록(Figma "설정 (S6)")
 * 앱 틀 맨 위 화면이라 상단 바 대신 큰 제목을 씀
 * 백업(스펙 003)과 목록 모양 줄은 그 화면과 함께 추가
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
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onPlus: () -> Unit = {},
    plusActive: Boolean = false,
    adsRemoved: Boolean = false,
    onDevMode: (() -> Unit)? = null,
    backupValue: String? = null,
    appVersion: String = "",
    onFeedback: () -> Unit = {},
    onRate: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    // 폰은 탭 바가 시스템 내비게이션 높이까지 맡고, 앱 틀이 다 썼다고 알려 아래가 두 번 뜨지 않음
    // 탭 바가 없는 넓은 화면에서만 마지막 줄 아래를 시스템 내비게이션 높이만큼 비움
    val scroll = rememberScrollState()
    OnTabReselected { scroll.animateScrollTo(0) }
    var titleHeight by remember { mutableIntStateOf(0) }
    SnapTitleOnRest(scroll, titleHeight)
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        // 큰 제목이 스크롤되어 바 밑으로 들어가는 만큼 바에 작은 제목이 나타남(`docs/design.md` 탭 상단 바)
        // 구독 중이면 바 오른쪽에 뱃지
        DiaryTabTopBar(
            title = stringResource(Res.string.settings_title),
            collapsed = { titleCollapse(scroll.value, titleHeight) },
            contentMaxWidth = DiaryWriteSize.settingsMax,
            actions = {
                if (plusActive) {
                    DiaryButton(
                        stringResource(Res.string.settings_plus_badge),
                        onPlus,
                        // 바 끝은 아이콘 단추 자리로 1만 비워 글자 단추를 화면 여백에 맞춤
                        modifier = Modifier.padding(end = LocalScreenGutter.current - DiarySpacing.x1),
                        size = DiaryButtonSize.Small,
                        kind = DiaryButtonKind.Weak,
                        leadingIcon = DiaryIcons.SparklesSolid
                    )
                }
            }
        )
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.settingsMax)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(bottom = DiarySpacing.x6)
        ) {
            BasicText(
                stringResource(Res.string.settings_title),
                style = DiaryTheme.typography.display.copy(color = colors.textPrimary),
                modifier =
                    Modifier
                        .padding(horizontal = LocalScreenGutter.current)
                        .onSizeChanged { titleHeight = it.height }
                        .semantics { heading() }
            )
            if (!plusActive) {
                // 설정은 살 마음이 생겼을 때 찾아오는 곳이라 첫자리에 혜택을 말하는 카드
                DiaryInviteCard(
                    stringResource(if (adsRemoved) Res.string.settings_plus_title_ad_free else Res.string.settings_plus_title),
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
                value = if (state.loaded) stringResource(state.settings.font.label()) else null
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_text_size),
                onClick = onTextSize,
                value = if (state.loaded) stringResource(state.settings.bodyTextSize.label()) else null
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_theme),
                onClick = onTheme,
                value = if (state.loaded) stringResource(state.settings.themeMode.label()) else null
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_list_style),
                onClick = onListStyle,
                value = if (state.loaded) stringResource(state.settings.list.listStyle.label()) else null
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_notification_security), gapAbove = true)
            val reminderValue =
                if (state.settings.reminderEnabled) {
                    val minute = state.settings.reminderMinuteOfDay
                    rememberDiaryDateFormat().time(LocalTime(minute / 60, minute % 60))
                } else {
                    stringResource(Res.string.reminder_value_off)
                }
            DiarySettingsRow(
                stringResource(Res.string.settings_reminder),
                onClick = onReminder,
                value = reminderValue.takeIf {
                    state.loaded
                }
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_lock),
                onClick = onLock,
                value = if (state.loaded) stringResource(state.lock.label()) else null
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_feedback), gapAbove = true)
            DiarySettingsRow(stringResource(Res.string.settings_feedback), onClick = onFeedback)
            onRate?.let { DiarySettingsRow(stringResource(Res.string.settings_rate), onClick = it) }
            DiarySettingsHeader(stringResource(Res.string.settings_group_info), gapAbove = true)
            DiarySettingsValueRow(stringResource(Res.string.settings_version), appVersion)
            DiarySettingsRow(stringResource(Res.string.settings_licenses), onClick = onLicenses)
            DiarySettingsRow(stringResource(Res.string.settings_terms), onClick = onTerms)
            DiarySettingsRow(stringResource(Res.string.settings_privacy), onClick = onPrivacy)
            if (state.showsMigrationReport) {
                DiarySettingsRow(stringResource(Res.string.migration_report_title), onClick = onMigrationReport)
            }
            DiarySettingsRow(stringResource(Res.string.settings_letter), onClick = onLetter)
            onDevMode?.let { DiarySettingsRow(stringResource(Res.string.settings_dev_mode), onClick = it) }
        }
    }
}
