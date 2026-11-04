package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 날짜 열 폭(요일 + 날짜 숫자). */
private val DATE_COLUMN = 36.dp

/** 날짜 열과 글 사이. */
private val DATE_GAP = 14.dp

/** 같은 날 두 번째 줄부터 구분선이 시작하는 자리(좌우 20 + 날짜 열 36 + 사이 14). */
private val TEXT_START = 70.dp

/** 사진 칸 크기. */
val DiaryPhotoCellSize = 64.dp

/**
 * 일기 줄(피드, `docs/design.md` 일기 줄·구분선, Day One 목록).
 * 위아래 16 · 좌우 20 · 날짜 열 36 · 사이 14 · 메타 한 줄 · 미리보기 `body` 3줄 · 사진 칸 64.
 * 날짜 열은 그날 첫 줄에만 보인다. 구분선은 날짜가 바뀌면 좌우 20부터, 같은 날이면 글 시작점부터 긋는다.
 *
 * @param weekday 요일 한 글자. 같은 날 두 번째 줄부터 null.
 * @param day 날짜 숫자. 같은 날 두 번째 줄부터 null.
 * @param meta 기분 얼굴, 시각, 날씨, 장소 같은 덧붙임. 사이 6으로 한 줄에 놓인다.
 * @param dividerAbove 위에 구분선을 그을지. 달 머리 바로 뒤와 첫 줄은 false.
 */
@Composable
fun DiaryEntryRow(
    weekday: String?,
    day: String?,
    preview: String,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    dividerAbove: Boolean = true,
    photo: (@Composable () -> Unit)? = null,
    previewStyle: TextStyle = DiaryTheme.typography.body,
    meta: @Composable RowScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxWidth()) {
        if (dividerAbove) {
            Box(
                Modifier
                    .padding(start = if (day != null) DiarySpacing.screenCompact else TEXT_START, end = DiarySpacing.screenCompact)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x4),
            horizontalArrangement = Arrangement.spacedBy(DATE_GAP)
        ) {
            Column(Modifier.width(DATE_COLUMN), horizontalAlignment = Alignment.CenterHorizontally) {
                if (weekday != null) {
                    BasicText(
                        weekday,
                        style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
                    )
                }
                if (day != null) {
                    BasicText(day, style = DiaryTheme.typography.numeral.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
                    meta()
                    if (favorite) {
                        DiaryIcon(
                            org.sjhstudio.diary.core.designsystem.icon.DiaryIcons.Star,
                            contentDescription = null,
                            tint = colors.accent,
                            size = org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize.i16
                        )
                    }
                }
                if (preview.isNotEmpty()) {
                    BasicText(
                        preview,
                        style = previewStyle.copy(color = colors.textPrimary),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (photo != null) Box(Modifier.size(DiaryPhotoCellSize).clip(DiaryShapes.r12)) { photo() }
        }
    }
}

/**
 * 달 머리(`docs/design.md` 달 머리, Apple 저널). 띠 없이 글자만 · 위 28 · 아래 4 · `headline`.
 * 같은 해면 연도를 빼는 일은 부르는 쪽이 문구로 정한다.
 */
@Composable
fun DiaryMonthHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = DiarySpacing.screenCompact)) {
        Spacer(Modifier.height(28.dp))
        BasicText(
            text,
            style = DiaryTheme.typography.headline.copy(color = DiaryTheme.colors.textPrimary),
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(DiarySpacing.x1))
    }
}
