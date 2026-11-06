package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
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
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryLoadingDots
import org.sjhstudio.diary.core.designsystem.component.DiaryTimeWheel
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.platform.PlaceLookup
import org.sjhstudio.diary.core.ui.DiaryMonthDays
import org.sjhstudio.diary.core.ui.DiaryMonthStepper
import org.sjhstudio.diary.core.ui.DiaryMoodGrid
import org.sjhstudio.diary.core.ui.DiaryWeatherGrid
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_close
import org.sjhstudio.diary.feature.write.resources.write_discard
import org.sjhstudio.diary.feature.write.resources.write_done
import org.sjhstudio.diary.feature.write.resources.write_mood_body
import org.sjhstudio.diary.feature.write.resources.write_mood_title
import org.sjhstudio.diary.feature.write.resources.write_new_entry
import org.sjhstudio.diary.feature.write.resources.write_none
import org.sjhstudio.diary.feature.write.resources.write_place
import org.sjhstudio.diary.feature.write.resources.write_place_body
import org.sjhstudio.diary.feature.write.resources.write_place_current
import org.sjhstudio.diary.feature.write.resources.write_place_denied
import org.sjhstudio.diary.feature.write.resources.write_place_finding
import org.sjhstudio.diary.feature.write.resources.write_place_hint
import org.sjhstudio.diary.feature.write.resources.write_place_not_found
import org.sjhstudio.diary.feature.write.resources.write_place_off
import org.sjhstudio.diary.feature.write.resources.write_place_recent
import org.sjhstudio.diary.feature.write.resources.write_place_settings
import org.sjhstudio.diary.feature.write.resources.write_resume_body
import org.sjhstudio.diary.feature.write.resources.write_resume_continue
import org.sjhstudio.diary.feature.write.resources.write_resume_title
import org.sjhstudio.diary.feature.write.resources.write_time
import org.sjhstudio.diary.feature.write.resources.write_weather_body
import org.sjhstudio.diary.feature.write.resources.write_weather_title

/** 같은 칸을 다시 누르면 풀린다. */
@Composable
internal fun MoodGrid(selected: Mood?, onSelect: (Mood) -> Unit, modifier: Modifier = Modifier) {
    DiaryMoodGrid({ it == selected }, onSelect, modifier)
}

@Composable
internal fun WeatherGrid(selected: Weather?, onSelect: (Weather) -> Unit, modifier: Modifier = Modifier) {
    DiaryWeatherGrid({ it == selected }, onSelect, modifier)
}

/** 이미 고른 값을 지우는 버튼. 고르기 전에는 할 일이 없어서 두지 않는다. */
@Composable
private fun ClearButton(onClick: () -> Unit) {
    DiaryButton(stringResource(Res.string.write_none), onClick, Modifier.fillMaxWidth(), kind = DiaryButtonKind.Text)
}

/**
 * 기분 칩에서 여는 시트. 날씨는 날씨 칩에서 따로 고른다(2026-11-05 사용자 지적).
 * 하나만 고르는 시트라 누르면 바로 들어가고 닫힌다. 완료를 한 번 더 누르게 하지 않는다.
 */
@Composable
internal fun MoodSheet(mood: Mood?, onMood: (Mood) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.write_mood_title),
        description = stringResource(Res.string.write_mood_body)
    ) {
        MoodGrid(mood, onMood)
        if (mood != null) ClearButton(onClear)
    }
}

/** 날씨 칩에서 여는 시트. */
@Composable
internal fun WeatherSheet(weather: Weather?, onWeather: (Weather) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.write_weather_title),
        description = stringResource(Res.string.write_weather_body)
    ) {
        WeatherGrid(weather, onWeather)
        if (weather != null) ClearButton(onClear)
    }
}

@Composable
internal fun PlaceSheet(
    initial: String,
    recent: List<String>,
    locating: PlaceLocating,
    onLocate: () -> Unit,
    onOpenSettings: (locationOff: Boolean) -> Unit,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val colors = DiaryTheme.colors
    // 찾은 이름은 입력 칸에 넣어 고칠 수 있게 둔다. 완료를 눌러야 일기에 들어간다.
    val found = ((locating as? PlaceLocating.Done)?.result as? PlaceLookup.Found)?.name
    LaunchedEffect(found) {
        if (found != null) text = found.take(MAX_PLACE)
    }
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.write_place),
        description = stringResource(Res.string.write_place_body)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            PlaceField(text, onChange = { text = it.take(MAX_PLACE) })
            CurrentPlace(locating, onLocate, onOpenSettings)
        }
        // 전에 쓴 곳은 한 번에 고른다. 적는 글자에 맞는 곳만 남긴다.
        val typed = text.trim()
        PlaceChips(
            stringResource(Res.string.write_place_recent),
            recent.filter {
                it.contains(typed, ignoreCase = true) && it != text
            },
            onDone
        )
        DiaryButton(stringResource(Res.string.write_done), { onDone(text) }, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
        if (initial.isNotEmpty()) ClearButton { onDone("") }
    }
}

@Composable
private fun PlaceField(text: String, onChange: (String) -> Unit) {
    val colors = DiaryTheme.colors
    Row(
        // 아래 칩과 아이콘·글자 시작이 같은 세로줄에 오게 칩의 왼쪽 여백(10)과 틈(4)을 쓴다.
        Modifier
            .fillMaxWidth()
            .clip(DiaryShapes.r12)
            .background(colors.sunken)
            .padding(start = DiarySpacing.x2 + DiarySpacing.x0_5, end = DiarySpacing.x3, top = DiarySpacing.x3, bottom = DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        DiaryIcon(DiaryIcons.Pin, contentDescription = null, tint = colors.textPrimary, size = DiaryIconSize.i16)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                BasicText(
                    stringResource(Res.string.write_place_hint),
                    style = DiaryTheme.typography.body.copy(color = colors.placeholder)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = DiaryTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent)
            )
        }
    }
}

/** 누를 때만 위치를 읽는다. 찾지 못해도 쓰기는 그대로고 한 줄로 알린다. */
@Composable
private fun CurrentPlace(locating: PlaceLocating, onLocate: () -> Unit, onOpenSettings: (locationOff: Boolean) -> Unit) {
    val finding = locating == PlaceLocating.Finding
    val result = (locating as? PlaceLocating.Done)?.result
    Column {
        DiaryChip(
            stringResource(if (finding) Res.string.write_place_finding else Res.string.write_place_current),
            selected = false,
            onClick = onLocate,
            leadingIcon = DiaryIcons.Locate,
            trailing = if (finding) {
                { DiaryLoadingDots(DiaryTheme.colors.textSecondary, animated = !rememberReduceMotion()) }
            } else {
                null
            }
        )
        when (result) {
            PlaceLookup.Denied -> PlaceNote(stringResource(Res.string.write_place_denied)) { onOpenSettings(false) }
            PlaceLookup.LocationOff -> PlaceNote(stringResource(Res.string.write_place_off)) { onOpenSettings(true) }
            PlaceLookup.NotFound -> PlaceNote(stringResource(Res.string.write_place_not_found), onOpenSettings = null)
            is PlaceLookup.Found, null -> Unit
        }
    }
}

@Composable
private fun PlaceNote(text: String, onOpenSettings: (() -> Unit)?) {
    val colors = DiaryTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = DiarySpacing.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        BasicText(text, Modifier.weight(1f), style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        if (onOpenSettings != null) {
            // 글자 끝을 입력 칸 끝에 맞추고 누르는 영역만 줄 높이로 넓힌다.
            Box(
                Modifier.height(DiarySpacing.minTouchTarget).clickable(role = Role.Button, onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    stringResource(Res.string.write_place_settings),
                    style = DiaryTheme.typography.captionStrong.copy(color = colors.accentText)
                )
            }
        }
    }
}

@Composable
private fun PlaceChips(title: String, places: List<String>, onPick: (String) -> Unit) {
    if (places.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        BasicText(title, style = DiaryTheme.typography.label.copy(color = DiaryTheme.colors.textSecondary))
        // 칩 줄 높이가 누르는 곳 48이라 줄 사이를 더 띄우지 않는다. 보이는 틈은 16이다.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            for (place in places) DiaryChip(place, selected = false, onClick = { onPick(place) })
        }
    }
}

@Composable
private fun SheetButtons(secondary: String, onSecondary: () -> Unit, primary: String, onPrimary: () -> Unit) {
    DiaryButtonPair(secondary, onSecondary, primary, onPrimary, size = DiaryButtonSize.XLarge)
}

/** "쓰던 일기가 있어요"(Figma "S2 · 쓰던 일기"). 버리기는 한 번 더 묻는다(FR-011). */
@Composable
internal fun ResumeSheet(offer: ResumeOffer, onContinue: () -> Unit, onFresh: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.write_resume_title),
        description = stringResource(Res.string.write_resume_body, dates.fullDate(offer.date), dates.time(offer.time))
    ) {
        // 기분이나 사진만 남긴 글은 미리 보여 줄 글자가 없다.
        if (offer.preview.isNotBlank()) {
            BasicText(
                Checklist.forPreview(offer.preview),
                modifier = Modifier.fillMaxWidth().clip(DiaryShapes.r12).background(colors.sunken).padding(DiarySpacing.x4),
                style = DiaryTheme.typography.body.copy(color = colors.textPrimary),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
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
        DiaryMonthStepper(month, onChange = { month = it })
        DiaryMonthDays(month, isPicked = { it == picked }, onPick = { picked = it })
        BasicText(stringResource(Res.string.write_time), style = DiaryTheme.typography.label.copy(color = colors.textSecondary))
        DiaryTimeWheel(minuteOfDay = minute, onChange = { minute = it }, is24Hour = dates.is24Hour)
        SheetButtons(
            stringResource(Res.string.write_close),
            onDismiss,
            stringResource(Res.string.write_done),
            { onDone(picked, LocalTime(minute / 60, minute % 60)) }
        )
    }
}
