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
import androidx.compose.ui.semantics.Role
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
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_first_title
import org.sjhstudio.diary.feature.backup.resources.backup_line_all
import org.sjhstudio.diary.feature.backup.resources.backup_line_at
import org.sjhstudio.diary.feature.backup.resources.backup_line_days
import org.sjhstudio.diary.feature.backup.resources.backup_line_off
import org.sjhstudio.diary.feature.backup.resources.backup_line_on
import org.sjhstudio.diary.feature.backup.resources.backup_line_reconnect
import org.sjhstudio.diary.feature.backup.resources.backup_line_running
import org.sjhstudio.diary.feature.backup.resources.backup_line_running_plain
import org.sjhstudio.diary.feature.backup.resources.backup_line_today
import org.sjhstudio.diary.feature.backup.resources.backup_line_waiting
import org.sjhstudio.diary.feature.backup.resources.backup_line_yesterday

private val LONG_FAILURE = 7.days

/**
 * 홈 머리의 백업 상태 줄(Figma B11, `docs/design.md` 홈 백업 줄).
 * 누르면 설정의 백업으로 간다.
 */
@Composable
fun BackupStatusLine(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BackupViewModel>()
    val status by viewModel.loadedStatus.collectAsStateWithLifecycle()
    // 읽기 전에는 틀린 말을 하지 않고 자리만 잡아 둔다.
    status?.let { BackupStatusLine(it, onOpen, modifier) }
        ?: BackupStatusLine(BackupStatus(), onOpen = {}, modifier.alpha(0f))
}

@Composable
internal fun BackupStatusLine(status: BackupStatus, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val line = lineFor(status)
    Row(
        modifier
            .padding(horizontal = DiarySpacing.screenCompact)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiaryIcon(line.icon, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
        BasicText(line.text, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary), maxLines = 1)
        line.action?.let { BasicText(it, style = DiaryTheme.typography.caption.copy(color = colors.accentText), maxLines = 1) }
        if (line.alert) Box(Modifier.size(DiarySpacing.x1_5).clip(DiaryShapes.full).background(colors.danger))
    }
}

private class Line(val icon: ImageVector, val text: String, val action: String? = null, val alert: Boolean = false)

@Composable
private fun lineFor(status: BackupStatus): Line {
    val running = status.running
    val last = status.lastSuccessAt
    return when {
        status.connection == BackupConnection.NotConnected ->
            Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_off), action = stringResource(Res.string.backup_line_on))

        status.connection == BackupConnection.NeedsReconnect ->
            Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_reconnect), alert = true)

        running != null -> Line(
            DiaryIcons.ShieldCheck,
            // 글을 먼저 올리는 동안은 사진 수를 모른다.
            // 0/0을 보이지 않는다.
            if (running.photosTotal > 0) {
                stringResource(Res.string.backup_line_running, running.photosDone, running.photosTotal)
            } else {
                stringResource(Res.string.backup_line_running_plain)
            }
        )

        last == null -> Line(
            DiaryIcons.ShieldCheck,
            status.restoredBackupAt?.let { restoredBackupText(it) } ?: stringResource(Res.string.backup_first_title)
        )

        else -> {
            val now = Clock.System.now()
            val failing = status.failingSince?.let { now - it >= LONG_FAILURE } == true
            val detail =
                if (status.photosNotBackedUp > 0) {
                    stringResource(Res.string.backup_line_waiting, status.photosNotBackedUp)
                } else {
                    stringResource(Res.string.backup_line_all)
                }
            if (failing) {
                Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_days, (now - last).inWholeDays.toInt()), alert = true)
            } else {
                Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_at, whenText(last), detail))
            }
        }
    }
}

@Composable
private fun whenText(at: Instant): String {
    val dates = rememberDiaryDateFormat()
    val zone = TimeZone.currentSystemDefault()
    val local = at.toLocalDateTime(zone)
    val today = currentDate(zone = zone)
    val time = dates.time(local.time)
    return when (local.date) {
        today -> stringResource(Res.string.backup_line_today, time)
        today.minus(DatePeriod(days = 1)) -> stringResource(Res.string.backup_line_yesterday, time)
        else -> dates.dayWithWeekday(local.date)
    }
}
