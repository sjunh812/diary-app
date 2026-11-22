package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryPreviewText
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryHomeSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow

/**
 * 카드 목록의 일기 카드(Figma "홈 · 카드", `docs/design.md` 카드)
 * 홈과 목록 보기 미리보기가 같은 카드를 씀. [onClick]이 없으면 누를 수 없는 견본
 */
@Composable
fun DiaryEntryCard(
    row: EntryRow,
    previewStyle: TextStyle,
    dates: DiaryDateFormat,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onPreviewClip: ((Boolean) -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.diaryPress(interaction, colors.textPrimary, DiaryShapes.r16))
            .clip(DiaryShapes.r16)
            .background(if (selected) colors.selected else colors.surfaceCard)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onLongClick = onLongClick,
                        onClick = onClick
                    )
                }
            )
            .padding(DiarySpacing.x4)
    ) {
        // 기분 얼굴은 날짜 줄과 시각 줄을 합친 높이의 가운데. 날짜 줄에만 맞추면 위로 떠 보임
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(Modifier.fillMaxWidth().padding(end = if (row.mood != null) DiaryHomeSize.cardFace + DiarySpacing.x2 else Dp.Hairline)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)) {
                    // 카드는 달 머리가 없어 달까지 적음. 맨 위 제목만으로는 어느 달인지 헷갈림
                    BasicText(
                        dates.monthDay(row.date),
                        modifier = Modifier.alignByBaseline(),
                        style = typography.headline.copy(color = colors.textPrimary)
                    )
                    BasicText(
                        dates.weekday(row.date),
                        modifier = Modifier.alignByBaseline(),
                        style = typography.captionStrong.copy(color = colors.textSecondary)
                    )
                }
                Row(
                    Modifier.padding(top = DiarySpacing.x1),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
                ) {
                    BasicText(
                        dates.time(row.time),
                        style = typography.captionStrong.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    row.weather?.let { WeatherIcon(it, DiaryHomeSize.cardIcon, colors.textSecondary, contentDescription = weatherName(it)) }
                    row.placeName?.let {
                        // 장소가 길어도 시각과 날씨를 밀어내지 않게
                        BasicText(
                            it,
                            modifier = Modifier.weight(1f, fill = false),
                            style = typography.caption.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // 피드형과 같은 자리. 모양만 다른 같은 목록
                    if (row.isFavorite) {
                        DiaryIcon(
                            DiaryIcons.StarFilled,
                            contentDescription = null,
                            tint = colors.accentIcon,
                            size = DiaryHomeSize.cardIcon
                        )
                    }
                }
            }
            row.mood?.let { MoodFace(it, DiaryHomeSize.cardFace, contentDescription = moodName(it)) }
        }
        if (row.preview.isNotEmpty()) {
            DiaryPreviewText(
                rememberPreview(row),
                modifier = Modifier.padding(top = DiarySpacing.x2),
                style = previewStyle.copy(color = colors.textPrimary),
                maxLines = row.previewLines,
                onClip = onPreviewClip
            )
        }
        if (row.photos.isNotEmpty()) {
            // 글이 없어도 사진은 시각 줄과 같은 간격
            Row(Modifier.padding(top = DiarySpacing.x3), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                val shown = row.photos.take(CARD_PHOTOS)
                shown.forEachIndexed { index, photo ->
                    // 피드 사진 칸과 같이 마지막 칸까지 더한 수
                    val more = if (index == CARD_PHOTOS - 1 && row.photos.size > CARD_PHOTOS) row.photos.size - CARD_PHOTOS + 1 else 0
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(DiaryShapes.r12)) { DiaryPhotoCell(listOf(photo), more = more) }
                }
                repeat(CARD_PHOTOS - shown.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 사진 격자 칸 안(FR-018). 사진이 있으면 첫 사진, 없으면 날짜와 첫 글
 * 칸 바탕과 누르기는 부르는 쪽이 정함
 */
@Composable
fun DiaryGridCellContent(row: EntryRow, today: LocalDate, dates: DiaryDateFormat, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    when {
        row.photos.isNotEmpty() -> DiaryPhotoCell(row.photos.take(1), modifier)

        else ->
            Column(modifier.padding(DiarySpacing.x2), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                // 칸에 달 머리가 없어 올해가 아니면 연도까지
                BasicText(
                    if (row.date.year == today.year) dates.monthDay(row.date) else dates.fullDate(row.date),
                    style = DiaryTheme.typography.micro.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    row.preview,
                    style = DiaryTheme.typography.caption.copy(color = colors.textPrimary),
                    maxLines = GRID_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
    }
}

private const val CARD_PHOTOS = 4
private const val GRID_LINES = 4
