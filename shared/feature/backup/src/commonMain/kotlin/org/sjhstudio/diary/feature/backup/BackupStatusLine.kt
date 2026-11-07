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

private val LONG_FAILURE = 7.days

/**
 * 홈 머리의 백업 상태 줄(Figma B11, `docs/design.md` 홈 백업 줄).
 * 누르면 설정의 백업으로 간다.
 */
@Composable
fun BackupStatusLine(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BackupViewModel>()
    val status by viewModel.loadedStatus.collectAsStateWithLifecycle()
    val photos by viewModel.photoProgress.collectAsStateWithLifecycle()
    // 읽기 전에는 틀린 말을 하지 않고 자리만 잡아 둔다.
    status?.let { BackupStatusLine(it, onOpen, modifier, photos) }
        ?: BackupStatusLine(BackupStatus(), onOpen = {}, modifier.alpha(0f))
}

@Composable
internal fun BackupStatusLine(status: BackupStatus, onOpen: () -> Unit, modifier: Modifier = Modifier, photos: PhotoProgress? = null) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val line = lineFor(status, photos)
    val caption = DiaryTheme.typography.caption
    // 글줄 모양은 그대로 두고 누르는 곳만 48로 넓힌다.
    // 누름 면은 글자에서 위아래 6, 옆 8까지만 나와서 위 큰 제목에 닿지 않는다.
    val lineHeight = with(LocalDensity.current) { caption.lineHeight.toDp() }
    val touch = ((DiarySpacing.minTouchTarget - lineHeight) / 2).coerceAtLeast(DiarySpacing.x1_5)
    Row(
        modifier
            .padding(horizontal = LocalScreenGutter.current)
            .diaryTouchOutset(horizontal = DiarySpacing.x2, vertical = touch)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpen)
            .padding(vertical = touch - DiarySpacing.x1_5)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .padding(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x1_5),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiaryIcon(line.icon, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
        // 글이 길면 글만 말줄임해서 뒤 동작과 빨간 점이 밀려나지 않는다.
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

@Composable
private fun lineFor(status: BackupStatus, photos: PhotoProgress?): Line {
    val running = status.running
    val last = status.lastSuccessAt
    val now = Clock.System.now()
    return when {
        // 사진을 받는 동안 "사진까지 모두"라고 말하면 틀린 말이 된다.
        photos != null -> Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_receiving, photos.done, photos.total))

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

        status.findingBackup -> Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_finding_title))

        // 실패가 7일을 넘으면 마지막 성공이 얼마나 지났는지가 더 중요하다.
        last != null && now - (status.failingSince ?: now) >= LONG_FAILURE ->
            Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_days, (now - last).inWholeDays.toInt()), alert = true)

        // 글을 올린 뒤 공간 때문에 사진만 남은 것은 실패가 아니라서 아래 줄이 말한다.
        status.lastFailure != null && status.failingSince != null ->
            Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_failed))

        last == null -> Line(
            DiaryIcons.ShieldCheck,
            status.restoredBackupAt?.let { restoredBackupText(it) } ?: stringResource(Res.string.backup_first_title)
        )

        else -> {
            val detail =
                if (status.photosNotBackedUp > 0) {
                    stringResource(Res.string.backup_line_waiting, status.photosNotBackedUp)
                } else {
                    stringResource(Res.string.backup_line_all)
                }
            Line(DiaryIcons.ShieldCheck, stringResource(Res.string.backup_line_at, whenText(last), detail))
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
