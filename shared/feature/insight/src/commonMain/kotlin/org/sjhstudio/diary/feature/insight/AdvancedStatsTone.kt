package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryInviteCard
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySegmentedTabs
import org.sjhstudio.diary.core.designsystem.component.DiaryTextTabs
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.component.diaryTouchOutset
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCompany
import org.sjhstudio.diary.core.model.MoodTone
import org.sjhstudio.diary.core.model.MoodTrend
import org.sjhstudio.diary.core.model.RecentMood
import org.sjhstudio.diary.core.model.RecordHabit
import org.sjhstudio.diary.core.model.StatsChange
import org.sjhstudio.diary.core.model.ToneShare
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.compactCount
import org.sjhstudio.diary.core.model.tone
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.emphasis
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.core.ui.withEmphasis
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_before
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_good
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_hard
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_recent
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_top
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_year
import org.sjhstudio.diary.feature.insight.resources.insight_adv_characters_big
import org.sjhstudio.diary.feature.insight.resources.insight_adv_characters_small
import org.sjhstudio.diary.feature.insight.resources.insight_adv_company
import org.sjhstudio.diary.feature.insight.resources.insight_adv_company_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_afternoon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_dawn
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_evening
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_morning
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_characters
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_characters_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_days
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_days_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_entries
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_entries_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_part
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_photos
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_photos_value
import org.sjhstudio.diary.feature.insight.resources.insight_adv_habit_run
import org.sjhstudio.diary.feature.insight.resources.insight_adv_no_moods
import org.sjhstudio.diary.feature.insight.resources.insight_adv_no_moods_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_not_enough
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_all
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_recent
import org.sjhstudio.diary.feature.insight.resources.insight_adv_range_year
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_better
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_good_down
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_good_up
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_hard_down
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_hard_up
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_harder
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_steady
import org.sjhstudio.diary.feature.insight.resources.insight_adv_recent_steady_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_share
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_part
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_place
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weather
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weekday
import org.sjhstudio.diary.feature.insight.resources.insight_adv_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_info
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_neutral
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_ok
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_sheet_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_sheet_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_unit_big
import org.sjhstudio.diary.feature.insight.resources.insight_adv_unit_small
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_fri
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_mon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sat
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sun
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_thu
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_tue
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_wed
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_empty
import org.sjhstudio.diary.feature.insight.resources.insight_write

// 고급 통계의 상황별 기분(긍정적인·부정적인 기분 막대와 기준 시트)

internal val Weekdays: List<Pair<DayOfWeek, StringResource>> =
    listOf(
        DayOfWeek.SUNDAY to Res.string.insight_adv_wd_sun,
        DayOfWeek.MONDAY to Res.string.insight_adv_wd_mon,
        DayOfWeek.TUESDAY to Res.string.insight_adv_wd_tue,
        DayOfWeek.WEDNESDAY to Res.string.insight_adv_wd_wed,
        DayOfWeek.THURSDAY to Res.string.insight_adv_wd_thu,
        DayOfWeek.FRIDAY to Res.string.insight_adv_wd_fri,
        DayOfWeek.SATURDAY to Res.string.insight_adv_wd_sat
    )

internal val DayParts: List<Pair<DayPart, StringResource>> =
    listOf(
        DayPart.DAWN to Res.string.insight_adv_day_dawn,
        DayPart.MORNING to Res.string.insight_adv_day_morning,
        DayPart.AFTERNOON to Res.string.insight_adv_day_afternoon,
        DayPart.EVENING to Res.string.insight_adv_day_evening
    )

/**
 * 상황별 기분(`docs/design.md` 상황별 기분)
 * 한 줄에 긍정적인 기분(왼쪽부터)과 부정적인 기분(오른쪽부터)을 함께 그림
 * 어느 쪽으로 세는지는 제목 줄 오른쪽 끝 안내 단추의 시트에서 보임
 */
@Composable
internal fun MoodTimes(stats: AdvancedStats) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var explaining by rememberSaveable { mutableStateOf(false) }
    Section(
        Res.string.insight_adv_when,
        gap = Dp.Hairline,
        // 글자 탭은 누르는 곳 48 가운데에 글자가 있어 위 여백이 붙음
        // 제목과 탭을 따로 띄우면 한 묶음으로 안 보여 사이를 두지 않음
        titleGap = Dp.Hairline,
        onInfo = { explaining = true }
    ) {
        // 화면 전체를 거르는 기간 탭(상자)과 구분해 이 묶음 안만 바꾼다는 게 보이게 글자 탭으로
        DiaryTextTabs(
            labels =
                listOf(
                    stringResource(Res.string.insight_adv_tab_weekday),
                    stringResource(Res.string.insight_adv_tab_part),
                    stringResource(Res.string.insight_adv_tab_weather),
                    stringResource(Res.string.insight_adv_tab_place)
                ),
            selected = tab,
            onSelect = { tab = it }
        )
        val rows: List<Pair<String, ToneShare>> =
            when (tab) {
                0 -> Weekdays.mapNotNull { (day, label) -> stats.weekdays[day]?.let { stringResource(label) to it } }
                1 -> DayParts.mapNotNull { (part, label) -> stats.dayParts[part]?.let { stringResource(label) to it } }
                2 -> stats.weather.entries.sortedByDescending { it.value.total }.map { weatherName(it.key) to it.value }
                else -> stats.places
            }
        Spacer(Modifier.height(DiarySpacing.x4))
        // 범례는 막대 바로 위. 색을 읽을 곳과 뜻을 적은 곳이 붙어 있게
        if (rows.isNotEmpty()) {
            ToneLegend()
            Spacer(Modifier.height(DiarySpacing.x3))
        }
        if (rows.isEmpty()) {
            BasicText(
                stringResource(Res.string.insight_adv_when_empty),
                style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textSecondary)
            )
        }
        val good = rows.map { (_, share) -> stringResource(Res.string.insight_adv_share, share.percent) }
        val hard = rows.map { (_, share) -> stringResource(Res.string.insight_adv_share, share.hardPercent) }
        val labelWidth = widestText(rows.map { it.first }, DiaryTheme.typography.caption).coerceAtMost(DiarySpacing.x14 * 2)
        val valueWidth = widestText(good + hard, DiaryTheme.typography.captionStrong)
        val goodName = stringResource(Res.string.insight_adv_change_good)
        val hardName = stringResource(Res.string.insight_adv_change_hard)
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            rows.forEachIndexed { index, (label, share) ->
                ToneRow(
                    label = label,
                    share = share,
                    good = good[index],
                    hard = hard[index],
                    description = "$label, $goodName ${good[index]}, $hardName ${hard[index]}",
                    labelWidth = labelWidth,
                    valueWidth = valueWidth
                )
            }
        }
    }
    if (explaining) ToneSheet(onDismiss = { explaining = false })
}

/** 막대 색이 무엇인지 알려 주는 한 줄 */
@Composable
internal fun ToneLegend() {
    val colors = DiaryTheme.colors
    // 글자가 커져 한 줄에 다 들어가지 않으면 다음 줄로
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x4),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(colors.toneGood, stringResource(Res.string.insight_adv_change_good))
        LegendItem(colors.toneHard, stringResource(Res.string.insight_adv_change_hard))
    }
}

@Composable
internal fun LegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(DiarySpacing.x2).clip(DiaryShapes.full).background(color))
        BasicText(label, style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary))
    }
}

/**
 * 이름 · 막대 · 좋은 기분 % · 힘든 기분 %.
 * 좋은 기분은 왼쪽부터, 힘든 기분은 오른쪽부터 채우고 가운데 빈 곳은 그 밖의 기분
 * 둘이 맞닿으면 2 띄워 경계가 보이게
 */
@Composable
internal fun ToneRow(label: String, share: ToneShare, good: String, hard: String, description: String, labelWidth: Dp, valueWidth: Dp) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            Modifier.width(labelWidth),
            style = typography.caption.copy(color = colors.textSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ToneBar(share.rate.toFloat(), share.hardRate.toFloat(), Modifier.weight(1f))
        // 둘 중 더 많은 쪽 값을 진하게. 같으면 둘 다 진하게
        val strong = typography.captionStrong.copy(color = colors.textPrimary, textAlign = TextAlign.End)
        val weak = typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.End)
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            BasicText(good, Modifier.width(valueWidth), style = if (share.rate >= share.hardRate) strong else weak, maxLines = 1)
            BasicText(hard, Modifier.width(valueWidth), style = if (share.hardRate >= share.rate) strong else weak, maxLines = 1)
        }
    }
}

@Composable
internal fun ToneBar(good: Float, hard: Float, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val goodColor = colors.toneGood
    val hardColor = colors.toneHard
    val track = colors.sunken
    Canvas(modifier.height(DiarySpacing.x2).clip(DiaryShapes.full).background(track)) {
        val gap = DiarySpacing.x0_5.toPx()
        val g = size.width * good.coerceIn(0f, 1f)
        val h = size.width * hard.coerceIn(0f, 1f)
        val meet = g + h >= size.width - gap
        val goodEnd = if (meet) (g - gap / 2).coerceAtLeast(0f) else g
        val hardStart = if (meet) (size.width - h + gap / 2).coerceAtMost(size.width) else size.width - h
        val radius = CornerRadius(size.height / 2, size.height / 2)
        if (goodEnd > 0f) drawRoundRect(goodColor, size = Size(goodEnd, size.height), cornerRadius = radius)
        if (hardStart < size.width) {
            drawRoundRect(
                hardColor,
                topLeft = Offset(hardStart, 0f),
                size = Size(size.width - hardStart, size.height),
                cornerRadius = radius
            )
        }
    }
}

/**
 * 좋은 기분과 힘든 기분을 무엇으로 세는지(`docs/design.md` 상황별 기분, DECISIONS 2026-11-04 쿨은 보통)
 * 기분의 밝고 어두움(심리학의 정서가)으로 나누고, 애매한 보통과 쿨은 세지 않음
 */
@Composable
internal fun ToneSheet(onDismiss: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.insight_adv_tone_sheet_title)) {
        PhraseText(stringResource(Res.string.insight_adv_tone_sheet_body), style = typography.body.copy(color = colors.textSecondary))
        // 설명 → 첫 묶음 16, 묶음 사이 20, 마지막 묶음 → 확인 32(시트 기본 사이 12 + 8에 버튼 둘레 여백, 재서 맞춤)
        // 시트 규칙은 글과 버튼 사이 24지만, 여기는 묶음 사이가 20이라 24면 버튼이 내용에 붙어 보임
        Column(Modifier.padding(top = DiarySpacing.x1), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x5)) {
            ToneGroup(colors.toneGood, stringResource(Res.string.insight_adv_change_good), Mood.entries.filter { it.tone == MoodTone.GOOD })
            ToneGroup(colors.toneHard, stringResource(Res.string.insight_adv_change_hard), Mood.entries.filter { it.tone == MoodTone.HARD })
            ToneGroup(
                colors.lineStrong,
                stringResource(Res.string.insight_adv_tone_neutral),
                Mood.entries.filter {
                    it.tone ==
                        MoodTone.NEUTRAL
                }
            )
        }
        Spacer(Modifier.height(DiarySpacing.x2))
        DiaryButton(stringResource(Res.string.insight_adv_tone_ok), onDismiss, Modifier.fillMaxWidth(), kind = DiaryButtonKind.Secondary)
    }
}

/** 묶음 이름과 그 묶음의 기분 얼굴·이름. 순서는 기분 고르기와 같음 */
@Composable
internal fun ToneGroup(color: Color, name: String, moods: List<Mood>) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    // 얼굴 줄이 둘로 넘어가도 줄 사이(12)가 이름·얼굴 사이와 같아야 한 묶음으로 읽힘
    // 8이면 얼굴 24끼리 거의 붙어 보임
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        LegendItem(color, name)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x4),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
        ) {
            Mood.ordered.filter { it in moods }.forEach { mood ->
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
                    MoodFace(mood, DiaryIconSize.i24, contentDescription = null)
                    BasicText(moodName(mood), style = typography.bodyStrong.copy(color = colors.textPrimary))
                }
            }
        }
    }
}
