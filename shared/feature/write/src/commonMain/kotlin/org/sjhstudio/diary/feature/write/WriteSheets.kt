package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryTimeWheel
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_close
import org.sjhstudio.diary.feature.write.resources.write_discard
import org.sjhstudio.diary.feature.write.resources.write_done
import org.sjhstudio.diary.feature.write.resources.write_mood_title
import org.sjhstudio.diary.feature.write.resources.write_new_entry
import org.sjhstudio.diary.feature.write.resources.write_none
import org.sjhstudio.diary.feature.write.resources.write_place
import org.sjhstudio.diary.feature.write.resources.write_place_hint
import org.sjhstudio.diary.feature.write.resources.write_resume_body
import org.sjhstudio.diary.feature.write.resources.write_resume_continue
import org.sjhstudio.diary.feature.write.resources.write_resume_title
import org.sjhstudio.diary.feature.write.resources.write_time
import org.sjhstudio.diary.feature.write.resources.write_weather

/**
 * 기분 고르기 칸 9개. 두 줄이 같은 다섯 칸을 쓰고 아랫줄은 왼쪽부터 채운다.
 * 고른 칸 바탕이 얼굴보다 넓어서 그만큼 왼쪽으로 내어 얼굴을 제목 선에 맞춘다.
 * 같은 칸을 다시 누르면 풀린다.
 */
@Composable
internal fun MoodGrid(selected: Mood?, onSelect: (Mood) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.offset(x = -DiarySpacing.x2), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        for (row in Mood.entries.chunked(MOOD_COLUMNS)) {
            Row(Modifier.fillMaxWidth()) {
                for (mood in row) {
                    Box(Modifier.weight(1f)) { MoodCell(mood, mood == selected) { onSelect(mood) } }
                }
                repeat(MOOD_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MoodCell(mood: Mood, selected: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier =
            Modifier
                .width(DiaryWriteSize.moodCellWidth)
                .height(DiaryWriteSize.moodCellHeight)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .clip(DiaryShapes.r12)
                .background(if (selected) colors.selected else colors.surface)
                .semantics { this.selected = selected }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .padding(top = DiarySpacing.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
    ) {
        MoodFace(mood, DiaryWriteSize.moodFace)
        BasicText(
            moodName(mood),
            style = (if (selected) DiaryTheme.typography.captionStrong else DiaryTheme.typography.caption).copy(
                color = colors.textSecondary
            ),
            maxLines = 1
        )
    }
}

/** 날씨 고르기 칸 7개. 기분처럼 두 줄이 같은 칸을 쓰고 아이콘과 이름은 칸 왼쪽에 맞춘다. */
@Composable
internal fun WeatherGrid(selected: Weather?, onSelect: (Weather) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.offset(x = -DiarySpacing.x2), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        for (row in Weather.entries.chunked(WEATHER_COLUMNS)) {
            // 이름이 두 줄인 칸이 있어도 한 줄의 칸은 높이가 같다.
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (weather in row) WeatherCell(weather, weather == selected, Modifier.weight(1f).fillMaxHeight()) { onSelect(weather) }
                repeat(WEATHER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WeatherCell(weather: Weather, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .heightIn(min = DiaryWriteSize.weatherCellHeight)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .clip(DiaryShapes.r12)
                .background(if (selected) colors.selected else colors.surface)
                .semantics { this.selected = selected }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x2),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        WeatherIcon(weather, DiaryIconSize.i24, if (selected) colors.accentText else colors.textSecondary)
        BasicText(
            weatherName(weather),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
            maxLines = 2
        )
    }
}

private const val MOOD_COLUMNS = 5
private const val WEATHER_COLUMNS = 4

@Composable
internal fun MoodWeatherSheet(
    date: LocalDate,
    mood: Mood?,
    weather: Weather?,
    onMood: (Mood) -> Unit,
    onWeather: (Weather) -> Unit,
    onClear: () -> Unit,
    onDate: () -> Unit,
    onDismiss: () -> Unit
) {
    val dates = rememberDiaryDateFormat()
    val colors = DiaryTheme.colors
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.write_mood_title)) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onDate),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(dates.dayWithWeekday(date), style = DiaryTheme.typography.label.copy(color = colors.accentText))
            DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.accentText, size = DiaryIconSize.i16)
        }
        MoodGrid(mood, onMood)
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            BasicText(
                stringResource(Res.string.write_weather),
                style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary)
            )
            WeatherGrid(weather, onWeather)
        }
        SheetButtons(stringResource(Res.string.write_none), onClear, stringResource(Res.string.write_done), onDismiss)
    }
}

@Composable
internal fun PlaceSheet(initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val colors = DiaryTheme.colors
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.write_place)) {
        Box(
            Modifier.fillMaxWidth().clip(DiaryShapes.r12).background(colors.sunken).padding(DiarySpacing.x3),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                BasicText(
                    stringResource(Res.string.write_place_hint),
                    style = DiaryTheme.typography.body.copy(color = colors.textSecondary)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(30) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = DiaryTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent)
            )
        }
        SheetButtons(stringResource(Res.string.write_close), onDismiss, stringResource(Res.string.write_done), { onDone(text) })
    }
}

@Composable
private fun SheetButtons(secondary: String, onSecondary: () -> Unit, primary: String, onPrimary: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        DiaryButton(secondary, onSecondary, Modifier.weight(1f), kind = DiaryButtonKind.Secondary)
        DiaryButton(primary, onPrimary, Modifier.weight(1f))
    }
}

/** "쓰던 일기가 있어요"(Figma "S2 · 쓰던 일기"). 버리기는 한 번 더 묻는다(FR-011). */
@Composable
internal fun ResumeSheet(offer: ResumeOffer, onContinue: () -> Unit, onFresh: () -> Unit, onDiscard: () -> Unit) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    DiaryBottomSheet(onDismiss = onContinue, title = stringResource(Res.string.write_resume_title)) {
        BasicText(
            stringResource(Res.string.write_resume_body, dates.fullDate(offer.date), dates.time(offer.time)),
            style = DiaryTheme.typography.body.copy(color = colors.textSecondary)
        )
        BasicText(
            offer.preview,
            modifier = Modifier.fillMaxWidth().clip(DiaryShapes.r12).background(colors.sunken).padding(DiarySpacing.x4),
            style = DiaryTheme.typography.body.copy(color = colors.textPrimary),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        SheetButtons(stringResource(Res.string.write_new_entry), onFresh, stringResource(Res.string.write_resume_continue), onContinue)
        DiaryButton(
            stringResource(Res.string.write_discard),
            onDiscard,
            Modifier.fillMaxWidth(),
            kind = DiaryButtonKind.Text
        )
    }
}

/** 날짜와 시각 고르기. 과거·미래 어느 날이든 고를 수 있다(FR-004). */
@Composable
internal fun DateTimeSheet(date: LocalDate, time: LocalTime, onDone: (LocalDate, LocalTime) -> Unit, onDismiss: () -> Unit) {
    val dates = rememberDiaryDateFormat()
    val colors = DiaryTheme.colors
    var picked by remember { mutableStateOf(date) }
    var month by remember { mutableStateOf(YearMonth(date.year, date.month.number)) }
    var minute by remember { mutableIntStateOf(time.hour * 60 + time.minute) }
    DiaryBottomSheet(onDismiss = onDismiss, title = dates.dayWithWeekday(picked)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(DiaryIcons.ChevronLeft) { month = month.shifted(-1) }
            BasicText(
                dates.yearMonth(month.onDay(1)),
                modifier = Modifier.weight(1f),
                style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
            )
            MonthArrow(DiaryIcons.ChevronRight) { month = month.shifted(1) }
        }
        MonthGrid(month, picked) { picked = it }
        BasicText(stringResource(Res.string.write_time), style = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary))
        DiaryTimeWheel(minuteOfDay = minute, onChange = { minute = it }, is24Hour = dates.is24Hour)
        SheetButtons(
            stringResource(Res.string.write_close),
            onDismiss,
            stringResource(Res.string.write_done),
            { onDone(picked, LocalTime(minute / 60, minute % 60)) }
        )
    }
}

private fun YearMonth.shifted(by: Int): YearMonth {
    val total = year * 12 + (month.number - 1) + by
    return YearMonth(total.floorDiv(12), total.mod(12) + 1)
}

@Composable
private fun MonthArrow(icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.full)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = null, tint = DiaryTheme.colors.textPrimary, size = DiaryIconSize.i20)
    }
}

@Composable
private fun MonthGrid(month: YearMonth, picked: LocalDate, onPick: (LocalDate) -> Unit) {
    val first = month.onDay(1)
    val start = first.minus(DatePeriod(days = (first.dayOfWeek.ordinal + 1) % 7))
    Column(Modifier.fillMaxWidth()) {
        var day = start
        while (day <= month.onDay(month.numberOfDays)) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) {
                    val current = day
                    DayCell(current, current == picked, inMonth = current.month == first.month, Modifier.weight(1f)) { onPick(current) }
                    day = day.plus(DatePeriod(days = 1))
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, inMonth: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val color =
        when {
            !inMonth -> colors.placeholder
            selected -> colors.onAccent
            date.dayOfWeek == DayOfWeek.SUNDAY -> colors.danger
            else -> colors.textPrimary
        }
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.height(DiaryWriteSize.dayCell), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(DiaryWriteSize.dayCell)
                .clip(DiaryShapes.full)
                .background(if (selected) colors.accent else colors.surface)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            BasicText(date.day.toString(), style = DiaryTheme.typography.body.copy(color = color))
        }
    }
}
