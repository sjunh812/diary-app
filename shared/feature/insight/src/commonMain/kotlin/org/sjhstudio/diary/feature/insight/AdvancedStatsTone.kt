package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryTextTabs
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayPart
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodTone
import org.sjhstudio.diary.core.model.ToneLeaders
import org.sjhstudio.diary.core.model.ToneShare
import org.sjhstudio.diary.core.model.tone
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.insight.resources.Res
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_good
import org.sjhstudio.diary.feature.insight.resources.insight_adv_change_hard
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_afternoon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_dawn
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_evening
import org.sjhstudio.diary.feature.insight.resources.insight_adv_day_morning
import org.sjhstudio.diary.feature.insight.resources.insight_adv_legend_neutral
import org.sjhstudio.diary.feature.insight.resources.insight_adv_share
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_part
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_place
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weather
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tab_weekday
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_neutral
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_ok
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_sheet_body
import org.sjhstudio.diary.feature.insight.resources.insight_adv_tone_sheet_title
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_fri
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_mon
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sat
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_sun
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_thu
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_tue
import org.sjhstudio.diary.feature.insight.resources.insight_adv_wd_wed
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_empty
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_good_place
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_good_time
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_good_weather
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_hard_place
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_hard_time
import org.sjhstudio.diary.feature.insight.resources.insight_adv_when_hard_weather

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
        val leaders = ToneLeaders.of(rows.map { it.second })
        // 범례는 막대 바로 위. 색을 읽을 곳과 뜻을 적은 곳이 붙어 있게
        if (rows.isNotEmpty()) {
            ToneSummary(
                good = leaders.good?.let { rows[it].first },
                hard = leaders.hard?.let { rows[it].first },
                phrases = summaryPhrases(tab)
            )
            ToneLegend()
            Spacer(Modifier.height(DiarySpacing.x4))
        }
        if (rows.isEmpty()) {
            PhraseText(
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
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
            rows.forEachIndexed { index, (label, share) ->
                ToneRow(
                    label = label,
                    share = share,
                    good = good[index],
                    hard = hard[index],
                    description = "$label, $goodName ${good[index]}, $hardName ${hard[index]}",
                    labelWidth = labelWidth,
                    valueWidth = valueWidth,
                    strongGood = index == leaders.good,
                    strongHard = index == leaders.hard
                )
            }
        }
    }
    if (explaining) ToneSheet(onDismiss = { explaining = false })
}

/** 결론 줄 문구. 조사 없이 이어지게 탭마다 꼴을 나눔 */
private class SummaryPhrases(val good: StringResource, val hard: StringResource)

private fun summaryPhrases(tab: Int): SummaryPhrases = when (tab) {
    0, 1 -> SummaryPhrases(Res.string.insight_adv_when_good_time, Res.string.insight_adv_when_hard_time)
    2 -> SummaryPhrases(Res.string.insight_adv_when_good_weather, Res.string.insight_adv_when_hard_weather)
    else -> SummaryPhrases(Res.string.insight_adv_when_good_place, Res.string.insight_adv_when_hard_place)
}

/**
 * 막대 위 한 줄 결론(`docs/design.md` 상황별 기분)
 * 긍정적인 기분이 가장 많은 칸은 이름을 굵게, 부정적인 기분은 아래 작은 줄로
 * 1등이 없으면 그 줄은 그리지 않음
 */
@Composable
private fun ToneSummary(good: String?, hard: String?, phrases: SummaryPhrases) {
    if (good == null && hard == null) return
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    // 결론은 범례·막대와 다른 묶음이라 위아래를 20으로 띄움. 위는 탭 뒤 16에 4를 더함
    Column(
        Modifier.padding(top = DiarySpacing.x1, bottom = DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
    ) {
        if (good != null) {
            // 이름과 뒤 조사가 다른 줄로 갈리지 않게 단어 연결 문자
            val line = stringResource(phrases.good, "$NAME_START$good$WORD_JOINER$NAME_END")
            PhraseText(
                strongName(line, typography.bodyStrong.toSpanStyle()),
                style = typography.body.copy(color = colors.textPrimary)
            )
        }
        if (hard != null) {
            PhraseText(
                stringResource(phrases.hard, "$hard$WORD_JOINER"),
                style = typography.caption.copy(color = colors.textSecondary)
            )
        }
    }
}

private const val NAME_START = '\uE000'
private const val NAME_END = '\uE001'
private const val WORD_JOINER = '\u2060'

/** 자리 표시로 감싼 이름을 굵게. 어순이 언어마다 달라도 이름만 굵어짐 */
private fun strongName(line: String, strong: SpanStyle): AnnotatedString {
    val start = line.indexOf(NAME_START)
    val end = line.indexOf(NAME_END)
    if (start < 0 || end < start) return AnnotatedString(line)
    return buildAnnotatedString {
        append(line, 0, start)
        withStyle(strong) { append(line, start + 1, end) }
        append(line, end + 1, line.length)
    }
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
        // 가운데 빈칸(트랙)의 뜻. 트랙 색은 바탕과 비슷해 테두리로 보이게
        LegendItem(
            colors.sunken,
            stringResource(Res.string.insight_adv_legend_neutral, moodName(Mood.COOL), moodName(Mood.MEH), moodName(Mood.BORED)),
            outline = colors.textTertiary
        )
    }
}

@Composable
internal fun LegendItem(color: Color, label: String, outline: Color? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(DiarySpacing.x2)
                .clip(DiaryShapes.full)
                .background(color)
                .then(if (outline != null) Modifier.border(DiarySpacing.hairline, outline, DiaryShapes.full) else Modifier)
        )
        BasicText(label, style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary))
    }
}

/**
 * 이름 · 막대 · 좋은 기분 % · 힘든 기분 %.
 * 좋은 기분은 왼쪽부터, 힘든 기분은 오른쪽부터 채우고 가운데 빈 곳은 그 밖의 기분
 * 둘이 맞닿으면 2 띄워 경계가 보이게
 * 결론 줄에 나온 칸만 이름과 그 값을 진하게([strongGood], [strongHard])
 */
@Composable
internal fun ToneRow(
    label: String,
    share: ToneShare,
    good: String,
    hard: String,
    description: String,
    labelWidth: Dp,
    valueWidth: Dp,
    strongGood: Boolean = false,
    strongHard: Boolean = false
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val strong = typography.captionStrong.copy(color = colors.textPrimary)
        val weak = typography.caption.copy(color = colors.textSecondary)
        BasicText(
            label,
            Modifier.width(labelWidth),
            style = if (strongGood || strongHard) strong else weak,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ToneBar(share.rate.toFloat(), share.hardRate.toFloat(), Modifier.weight(1f))
        val end = TextAlign.End
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            BasicText(good, Modifier.width(valueWidth), style = (if (strongGood) strong else weak).copy(textAlign = end), maxLines = 1)
            BasicText(hard, Modifier.width(valueWidth), style = (if (strongHard) strong else weak).copy(textAlign = end), maxLines = 1)
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
 * 좋은 기분과 힘든 기분을 무엇으로 세는지(`docs/design.md` 상황별 기분)
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
