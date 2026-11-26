package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
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
import org.sjhstudio.diary.core.data.percent
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
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.ui.OnTabReselected
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.shownColor
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.list_value
import org.sjhstudio.diary.feature.settings.resources.migration_report_title
import org.sjhstudio.diary.feature.settings.resources.reminder_value_blocked
import org.sjhstudio.diary.feature.settings.resources.reminder_value_off
import org.sjhstudio.diary.feature.settings.resources.settings_ad_privacy
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
import org.sjhstudio.diary.feature.settings.resources.settings_version_downloading
import org.sjhstudio.diary.feature.settings.resources.settings_version_downloading_unknown
import org.sjhstudio.diary.feature.settings.resources.settings_version_restart
import org.sjhstudio.diary.feature.settings.resources.settings_version_update
import org.sjhstudio.diary.feature.settings.resources.theme_row_value

/**
 * 설정 목록(Figma "설정 (S6)")
 * 앱 틀 맨 위 화면이라 상단 바 대신 큰 제목을 씀
 * 백업(스펙 003)과 목록 보기 줄은 그 화면과 함께 추가
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
    onBackup: () -> Unit = {},
    onLetter: () -> Unit = {},
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onAdPrivacy: () -> Unit = {},
    onPlus: () -> Unit = {},
    onDevMode: (() -> Unit)? = null,
    backupValue: String? = null,
    appVersion: String = "",
    update: UpdateNotice = UpdateNotice.None,
    onUpdate: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onRate: (() -> Unit)? = null,
    selected: SettingsPane? = null
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
                if (state.plan == Plan.PLUS) {
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
            // 설정은 살 마음이 생겼을 때 찾아오는 곳이라 첫자리에 혜택을 말하는 카드
            // 요금제를 안 뒤에만 그림. 재설치 직후 무료 문구가 먼저 보였다가 바뀌지 않게
            val inviting = state.plan != null && state.plan != Plan.PLUS
            if (inviting) {
                val inviteTitle = if (state.plan == Plan.AD_FREE) Res.string.settings_plus_title_ad_free else Res.string.settings_plus_title
                DiaryInviteCard(
                    stringResource(inviteTitle),
                    stringResource(Res.string.settings_plus_body),
                    stringResource(Res.string.settings_plus_action),
                    onPlus,
                    // 카드 아래를 띄워 첫 머리가 카드가 아니라 아래 줄에 붙어 보이게
                    Modifier.padding(
                        start = LocalScreenGutter.current,
                        end = LocalScreenGutter.current,
                        top = DiarySpacing.x4,
                        bottom = DiarySpacing.x4
                    )
                )
            }
            DiarySettingsHeader(stringResource(Res.string.settings_group_diary), gapAbove = inviting)
            DiarySettingsRow(
                stringResource(Res.string.settings_backup),
                onClick = onBackup,
                value = backupValue,
                selected =
                    selected == SettingsPane.BACKUP
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_trash),
                onClick = onTrash,
                selected = selected == SettingsPane.TRASH,
                value = if (state.trashCount >
                    0
                ) {
                    pluralStringResource(Res.plurals.settings_trash_count, state.trashCount, state.trashCount.grouped())
                } else {
                    null
                }
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_style), gapAbove = true)
            DiarySettingsRow(
                stringResource(Res.string.settings_font),
                onClick = onFont,
                selected = selected == SettingsPane.FONT,
                value = if (state.loaded) stringResource(state.settings.font.label()) else null
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_text_size),
                onClick = onTextSize,
                selected = selected == SettingsPane.TEXT_SIZE,
                value = if (state.loaded) stringResource(state.settings.bodyTextSize.label()) else null
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_theme),
                onClick = onTheme,
                selected = selected == SettingsPane.THEME,
                value =
                    if (state.loaded) {
                        stringResource(
                            Res.string.theme_row_value,
                            stringResource(state.settings.theme.shownColor(customAllowed = state.canCustomTheme == true).label()),
                            stringResource(state.settings.themeMode.label())
                        )
                    } else {
                        null
                    }
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_list_style),
                onClick = onListStyle,
                selected = selected == SettingsPane.LIST_STYLE,
                value = if (state.loaded) listValue(state.settings.list) else null
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_notification_security), gapAbove = true)
            val reminderValue =
                if (state.reminderBlocked == true) {
                    stringResource(Res.string.reminder_value_blocked)
                } else if (state.settings.reminderEnabled) {
                    val minute = state.settings.reminderMinuteOfDay
                    rememberDiaryDateFormat().time(LocalTime(minute / 60, minute % 60))
                } else {
                    stringResource(Res.string.reminder_value_off)
                }
            DiarySettingsRow(
                stringResource(Res.string.settings_reminder),
                onClick = onReminder,
                selected = selected == SettingsPane.REMINDER,
                value = reminderValue.takeIf { state.loaded && state.reminderBlocked != null }
            )
            DiarySettingsRow(
                stringResource(Res.string.settings_lock),
                onClick = onLock,
                selected = selected == SettingsPane.LOCK,
                value = if (state.loaded) stringResource(state.lock.label()) else null
            )
            DiarySettingsHeader(stringResource(Res.string.settings_group_feedback), gapAbove = true)
            DiarySettingsRow(stringResource(Res.string.settings_letter), onClick = onLetter)
            DiarySettingsRow(stringResource(Res.string.settings_feedback), onClick = onFeedback)
            onRate?.let { DiarySettingsRow(stringResource(Res.string.settings_rate), onClick = it) }
            DiarySettingsHeader(stringResource(Res.string.settings_group_info), gapAbove = true)
            // 새 버전이 있으면 누를 수 있는 줄(스펙 009). 설정 위에서 스토어 창이 뜸
            // 받는 중에는 할 일이 없어 회색 상태만 보임
            val version = stringResource(Res.string.settings_version)
            when (update) {
                UpdateNotice.None -> DiarySettingsValueRow(version, appVersion)

                is UpdateNotice.Downloading -> DiarySettingsValueRow(version, appVersion, valueNote = update.progressLabel())

                is UpdateNotice.Available, is UpdateNotice.Downloaded ->
                    DiarySettingsRow(version, onClick = onUpdate, value = appVersion, valueAction = update.actionLabel())
            }
            DiarySettingsRow(
                stringResource(Res.string.settings_licenses),
                onClick = onLicenses,
                selected =
                    selected == SettingsPane.LICENSES
            )
            DiarySettingsRow(stringResource(Res.string.settings_terms), onClick = onTerms)
            DiarySettingsRow(stringResource(Res.string.settings_privacy), onClick = onPrivacy)
            if (state.showsAdPrivacy) DiarySettingsRow(stringResource(Res.string.settings_ad_privacy), onClick = onAdPrivacy)
            if (state.showsMigrationReport) {
                DiarySettingsRow(
                    stringResource(Res.string.migration_report_title),
                    onClick = onMigrationReport,
                    selected =
                        selected == SettingsPane.MIGRATION_REPORT
                )
            }
            onDevMode?.let { DiarySettingsRow(stringResource(Res.string.settings_dev_mode), onClick = it) }
        }
    }
}

/** "피드 · 전체". 사진 격자는 길이와 상관없어 모양만(스펙 008 FR-006) */
@Composable
private fun listValue(list: ListPreferences): String {
    val style = stringResource(list.listStyle.label())
    return if (list.listStyle ==
        ListStyle.PHOTO_GRID
    ) {
        style
    } else {
        stringResource(Res.string.list_value, style, stringResource(list.previewLength.label()))
    }
}

/** 앱 버전 줄 값 뒤 할 일 */
@Composable
private fun UpdateNotice.actionLabel(): String = stringResource(
    if (this is UpdateNotice.Downloaded) Res.string.settings_version_restart else Res.string.settings_version_update
)

/** 받는 중 진행률. 모르면 "받는 중"만 */
@Composable
private fun UpdateNotice.Downloading.progressLabel(): String =
    percent()?.let { stringResource(Res.string.settings_version_downloading, it) }
        ?: stringResource(Res.string.settings_version_downloading_unknown)
