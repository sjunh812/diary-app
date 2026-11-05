package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.TopMood
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.NoMoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_afternoon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_dawn
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_evening
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_morning
import org.sjhstudio.diary.feature.insight.resources.insight_adv_dayparts
import org.sjhstudio.diary.feature.insight.resources.insight_adv_days
import org.sjhstudio.diary.feature.insight.resources.insight_adv_not_enough
import org.sjhstudio.diary.feature.insight.resources.insight_adv_places
import org.sjhstudio.diary.feature.insight.resources.insight_adv_preview_action
import org.sjhstudio.diary.feature.insight.resources.insight_adv_preview_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_preview_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_fri
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_mon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sat
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sun
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_thu
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_tue
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_wed
import org.sjhstudio.diary.feature.insight.resources.insight_adv_weather
import org.sjhstudio.diary.feature.insight.resources.insight_adv_weekdays

private val Weekdays: List<Pair<DayOfWeek, StringResource>> =
    listOf(
        DayOfWeek.SUNDAY to Res.string.insight_adv_wd_sun,
        DayOfWeek.MONDAY to Res.string.insight_adv_wd_mon,
        DayOfWeek.TUESDAY to Res.string.insight_adv_wd_tue,
        DayOfWeek.WEDNESDAY to Res.string.insight_adv_wd_wed,
        DayOfWeek.THURSDAY to Res.string.insight_adv_wd_thu,
        DayOfWeek.FRIDAY to Res.string.insight_adv_wd_fri,
        DayOfWeek.SATURDAY to Res.string.insight_adv_wd_sat
    )

private val DayParts: List<Pair<DayPart, StringResource>> =
    listOf(
        DayPart.DAWN to Res.string.insight_adv_day_dawn,
        DayPart.MORNING to Res.string.insight_adv_day_morning,
        DayPart.AFTERNOON to Res.string.insight_adv_day_afternoon,
        DayPart.EVENING to Res.string.insight_adv_day_evening
    )

/** 무료 사용자의 미리보기 칸(Figma P6). 기존 통계는 그대로 두고 아래에 한 칸만 둔다(FR-012). */
@Composable
internal fun AdvancedPreview(onPlus: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DiaryShapes.r16)
            .background(colors.selected)
            .clickable(role = Role.Button, onClick = onPlus)
            .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(stringResource(Res.string.insight_adv_preview_title), style = typography.bodyStrong.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.insight_adv_preview_body), style = typography.caption.copy(color = colors.textSecondary))
        BasicText(
            stringResource(Res.string.insight_adv_preview_action),
            Modifier.padding(top = DiarySpacing.x2),
            style = typography.captionStrong.copy(color = colors.accentText)
        )
    }
}

/** 고급 통계. 기간 탭을 그대로 따른다(US5). */
@Composable
internal fun AdvancedSection(stats: AdvancedStats?) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
        BasicText(stringResource(Res.string.insight_adv_title), style = typography.title2.copy(color = colors.textPrimary))
        when {
            stats == null -> Unit

            !stats.enough ->
                BasicText(stringResource(Res.string.insight_adv_not_enough), style = typography.body.copy(color = colors.textSecondary))

            else -> {
                BasicText(
                    pluralStringResource(Res.plurals.insight_adv_days, stats.daysWritten, stats.daysWritten),
                    style = typography.body.copy(color = colors.textSecondary)
                )
                Group(Res.string.insight_adv_weekdays) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Weekdays.forEach { (day, label) -> MoodCell(label, stats.weekdays[day]) }
                    }
                }
                Group(Res.string.insight_adv_dayparts) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DayParts.forEach { (part, label) -> MoodCell(label, stats.dayParts[part]) }
                    }
                }
                if (stats.weather.isNotEmpty()) {
                    Group(Res.string.insight_adv_weather) {
                        stats.weather.entries.sortedByDescending { it.value.entries }.forEach { (weather, top) ->
                            Line(top) {
                                WeatherIcon(weather, DiarySpacing.x5, colors.textSecondary)
                                BasicText(weatherName(weather), style = typography.body.copy(color = colors.textPrimary))
                            }
                        }
                    }
                }
                if (stats.places.isNotEmpty()) {
                    Group(Res.string.insight_adv_places) {
                        stats.places.forEach { (place, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BasicText(place, style = typography.body.copy(color = colors.textPrimary))
                                BasicText("$count", style = typography.body.copy(color = colors.textSecondary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(title: StringResource, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        BasicText(stringResource(title), style = DiaryTheme.typography.bodyStrong.copy(color = DiaryTheme.colors.textPrimary))
        content()
    }
}

@Composable
private fun MoodCell(label: StringResource, top: TopMood?) {
    val colors = DiaryTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        val mood = top?.mood
        when {
            mood != null -> MoodFace(mood, DiarySpacing.x8, contentDescription = moodName(mood))
            top != null -> NoMoodFace(DiarySpacing.x8)
            else -> EmptyMoodSlot(DiarySpacing.x8)
        }
        BasicText(
            stringResource(label),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun Line(top: TopMood, name: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) { name() }
        val mood = top.mood
        if (mood != null) MoodFace(mood, DiarySpacing.x6, contentDescription = moodName(mood)) else NoMoodFace(DiarySpacing.x6)
    }
}
