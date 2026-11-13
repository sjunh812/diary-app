package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.component.diaryTouchOutset
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.loadFailedRetryLabel
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_finding_title
import org.sjhstudio.diary.feature.backup.resources.backup_first_title
import org.sjhstudio.diary.feature.backup.resources.backup_line_all
import org.sjhstudio.diary.feature.backup.resources.backup_line_at
import org.sjhstudio.diary.feature.backup.resources.backup_line_days
import org.sjhstudio.diary.feature.backup.resources.backup_line_failed
import org.sjhstudio.diary.feature.backup.resources.backup_line_off
import org.sjhstudio.diary.feature.backup.resources.backup_line_on
import org.sjhstudio.diary.feature.backup.resources.backup_line_receiving
import org.sjhstudio.diary.feature.backup.resources.backup_line_reconnect
import org.sjhstudio.diary.feature.backup.resources.backup_line_running
import org.sjhstudio.diary.feature.backup.resources.backup_line_running_plain
import org.sjhstudio.diary.feature.backup.resources.backup_line_today
import org.sjhstudio.diary.feature.backup.resources.backup_line_waiting
import org.sjhstudio.diary.feature.backup.resources.backup_line_yesterday
import org.sjhstudio.diary.feature.backup.resources.backup_status_failed
import org.sjhstudio.diary.feature.backup.resources.backup_when_day_time

private val LONG_FAILURE = 7.days

/**
 * 홈 머리의 백업 상태 줄(Figma B11, `docs/design.md` 홈 백업 줄)
 * 누르면 설정의 백업으로 감
 */
@Composable
fun BackupStatusLine(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BackupViewModel>()
    val status by viewModel.loadedStatus.collectAsStateWithLifecycle()
    val failed by viewModel.statusFailed.collectAsStateWithLifecycle()
    val photos by viewModel.photoProgress.collectAsStateWithLifecycle()
    val current = status
    when {
        // 읽지 못한 것을 숨기면 백업이 도는지 사용자가 알 수 없음
        // 꺼짐이라고 하지 않고 읽지 못했다고 말한 뒤 그 자리에서 다시 읽음
        failed -> BackupStatusFailedLine(viewModel::retryStatus, modifier)

        current != null -> BackupStatusLine(current, onOpen, modifier, photos)

        // 읽기 전에는 틀린 말을 하지 않고 자리만 잡아 둠
        else -> BackupStatusLine(BackupStatus(), onOpen = {}, modifier.alpha(0f))
    }
}

@Composable
fun BackupStatusLine(status: BackupStatus, onOpen: () -> Unit, modifier: Modifier = Modifier, photos: PhotoProgress? = null) {
    StatusLineRow(lineFor(status, photos), onOpen, modifier)
}

/** 상태를 읽지 못했을 때의 홈 줄. 누르면 그 자리에서 다시 읽음 */
@Composable
internal fun BackupStatusFailedLine(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    StatusLineRow(
        Line(DiaryIcons.CircleAlert, stringResource(Res.string.backup_status_failed), action = loadFailedRetryLabel()),
        onRetry,
        modifier
    )
}

@Composable
private fun StatusLineRow(line: Line, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val caption = DiaryTheme.typography.caption
    // 글줄 모양은 그대로 두고 누르는 곳만 48로 넓힘
    // 누름 면은 글자에서 위아래 6, 옆 8까지만 나와 위 큰 제목에 닿지 않음
    val lineHeight = with(LocalDensity.current) { caption.lineHeight.toDp() }
    val touch = ((DiarySpacing.minTouchTarget - lineHeight) / 2).coerceAtLeast(DiarySpacing.x1_5)
    Row(
        modifier
            .padding(horizontal = LocalScreenGutter.current)
            .diaryTouchOutset(horizontal = DiarySpacing.x2, vertical = touch)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = touch - DiarySpacing.x1_5)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .padding(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x1_5),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiaryIcon(line.icon, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
        // 글이 길면 글만 말줄임해 뒤 동작과 빨간 점이 밀려나지 않음
        BasicText(
            line.text,
            modifier = Modifier.weight(1f, fill = false),
            style = caption.copy(color = colors.textSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        line.action?.let { BasicText(it, style = caption.copy(color = colors.accentText), maxLines = 1) }
        if (line.alert) Box(Modifier.size(DiarySpacing.x1_5).clip(DiaryShapes.full).background(colors.danger))
    }
}

private class Line(val icon: ImageVector, val text: String, val action: String? = null, val alert: Boolean = false)

/** 사용자가 손봐야 하는 백업 상태. 홈 줄의 빨간 점과 접힌 바의 경고 단추가 같은 기준을 씀 */
internal enum class BackupAttention { Reconnect, LongFailure }

/**
 * 손봐야 할 백업 상태 고르기
 * [lineFor]와 같은 순서로 따짐. 줄이 다른 말을 하는 동안(사진 받는 중, 꺼짐, 올리는 중, 찾는 중)은 경고하지 않음
 */
internal fun attentionOf(status: BackupStatus, receivingPhotos: Boolean, now: Instant): BackupAttention? {
    val last = status.lastSuccessAt
    return when {
        receivingPhotos || status.connection == BackupConnection.NotConnected -> null
        status.connection == BackupConnection.NeedsReconnect -> BackupAttention.Reconnect
        status.running != null || status.findingBackup -> null
        last != null && now - (status.failingSince ?: now) >= LONG_FAILURE -> BackupAttention.LongFailure
        else -> null
    }
}

/**
 * 홈 머리가 접혔을 때 바의 작은 제목 옆에 두는 백업 경고(`docs/design.md` 탭 상단 바)
 * 손봐야 할 때만 보이고, 누르면 홈 백업 줄처럼 설정의 백업으로 감
 * 평소 상태는 맨 위로 올리면 백업 줄이 말하므로 바에 두지 않음
 */
@Composable
fun BackupAlertButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BackupViewModel>()
    val status by viewModel.loadedStatus.collectAsStateWithLifecycle()
    val photos by viewModel.photoProgress.collectAsStateWithLifecycle()
    val attention = status?.let { attentionOf(it, photos != null, Clock.System.now()) } ?: return
    val label =
        when (attention) {
            BackupAttention.Reconnect -> stringResource(Res.string.backup_line_reconnect)
            BackupAttention.LongFailure -> stringResource(Res.string.backup_line_failed)
        }
    DiaryIconButton(
        if (attention == BackupAttention.Reconnect) DiaryIcons.LinkOff else DiaryIcons.CloudOff,
        contentDescription = label,
        onClick = onOpen,
        modifier = modifier.diaryTouchOutset(horizontal = DiarySpacing.x3, vertical = DiarySpacing.x3),
        tint = DiaryTheme.colors.danger,
        iconSize = DiaryIconSize.i20
    )
}

@Composable
private fun lineFor(status: BackupStatus, photos: PhotoProgress?): Line {
    val running = status.running
    val last = status.lastSuccessAt
    val now = Clock.System.now()
    // 아이콘이 상태를 말함(Figma B11 상태들)
    // 방패는 사진까지 다 올라간 때만 씀
    return when {
        // 사진을 받는 동안 "사진까지 모두"라고 하면 틀린 말
        photos != null -> Line(
            DiaryIcons.Download,
            stringResource(Res.string.backup_line_receiving, photos.done.grouped(), photos.total.grouped())
        )

        status.connection == BackupConnection.NotConnected ->
            Line(DiaryIcons.CloudOff, stringResource(Res.string.backup_line_off), action = stringResource(Res.string.backup_line_on))

        status.connection == BackupConnection.NeedsReconnect ->
            Line(DiaryIcons.LinkOff, stringResource(Res.string.backup_line_reconnect), alert = true)

        running != null -> Line(
            DiaryIcons.CloudUpload,
            // 글을 먼저 올리는 동안은 사진 수를 모름
            // 0/0은 보이지 않음
            if (running.photosTotal > 0) {
                stringResource(Res.string.backup_line_running, running.photosDone.grouped(), running.photosTotal.grouped())
            } else {
                stringResource(Res.string.backup_line_running_plain)
            }
        )

        status.findingBackup -> Line(DiaryIcons.Cloud, stringResource(Res.string.backup_finding_title))

        // 실패가 7일을 넘으면 마지막 성공이 얼마나 지났는지가 더 중요함
        last != null && now - (status.failingSince ?: now) >= LONG_FAILURE ->
            Line(DiaryIcons.CloudOff, stringResource(Res.string.backup_line_days, (now - last).inWholeDays.toInt()), alert = true)

        // 글을 올린 뒤 공간 때문에 사진만 남은 것은 실패가 아니라 아래 줄이 말함
        status.lastFailure != null && status.failingSince != null ->
            Line(DiaryIcons.CircleAlert, stringResource(Res.string.backup_line_failed))

        last == null -> Line(
            DiaryIcons.Cloud,
            status.restoredBackupAt?.let { restoredBackupText(it) } ?: stringResource(Res.string.backup_first_title)
        )

        else -> {
            val detail =
                if (status.photosNotBackedUp > 0) {
                    stringResource(Res.string.backup_line_waiting, status.photosNotBackedUp.grouped())
                } else {
                    stringResource(Res.string.backup_line_all)
                }
            // 방패는 사진까지 다 올라가 지킬 것이 없을 때만 씀
            val icon = if (status.photosNotBackedUp > 0) DiaryIcons.Cloud else DiaryIcons.ShieldCheck
            Line(icon, stringResource(Res.string.backup_line_at, backupWhen(last), detail))
        }
    }
}

/**
 * 백업 시각. 오늘·어제는 "오늘 오후 9:30"처럼, 그 전은 날짜만
 * [withTime]이면 그 전 날짜에도 시각을 붙임(백업 화면 상태 상자)
 */
@Composable
internal fun backupWhen(at: Instant, withTime: Boolean = false): String {
    val dates = rememberDiaryDateFormat()
    val zone = TimeZone.currentSystemDefault()
    val local = at.toLocalDateTime(zone)
    val today = currentDate(zone = zone)
    val time = dates.time(local.time)
    return when (local.date) {
        today -> stringResource(Res.string.backup_line_today, time)

        today.minus(DatePeriod(days = 1)) -> stringResource(Res.string.backup_line_yesterday, time)

        else -> if (withTime) {
            stringResource(
                Res.string.backup_when_day_time,
                dates.dayWithWeekday(local.date),
                time
            )
        } else {
            dates.dayWithWeekday(local.date)
        }
    }
}

/** 설정의 [백업과 복원] 줄 값. 홈 백업 줄과 같은 말 */
@Composable
fun settingsBackupValue(at: Instant): String = backupWhen(at)
