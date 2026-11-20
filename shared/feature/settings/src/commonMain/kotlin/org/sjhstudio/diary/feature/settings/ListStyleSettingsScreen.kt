package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiarySheetChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.RichSpan
import org.sjhstudio.diary.core.model.RichStyle
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.richtext.PreviewText
import org.sjhstudio.diary.core.ui.DiaryEntryCard
import org.sjhstudio.diary.core.ui.DiaryEntryLine
import org.sjhstudio.diary.core.ui.DiaryGridCellContent
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.font_preview_label
import org.sjhstudio.diary.feature.settings.resources.list_formatting
import org.sjhstudio.diary.feature.settings.resources.list_formatting_body
import org.sjhstudio.diary.feature.settings.resources.list_formatting_body_free
import org.sjhstudio.diary.feature.settings.resources.list_formatting_plus_body
import org.sjhstudio.diary.feature.settings.resources.list_formatting_plus_title
import org.sjhstudio.diary.feature.settings.resources.list_group_body
import org.sjhstudio.diary.feature.settings.resources.list_group_length
import org.sjhstudio.diary.feature.settings.resources.list_group_style
import org.sjhstudio.diary.feature.settings.resources.list_plus_action
import org.sjhstudio.diary.feature.settings.resources.list_plus_later
import org.sjhstudio.diary.feature.settings.resources.list_sample_body
import org.sjhstudio.diary.feature.settings.resources.list_sample_photo_mountain
import org.sjhstudio.diary.feature.settings.resources.list_sample_photo_sea
import org.sjhstudio.diary.feature.settings.resources.list_sample_place
import org.sjhstudio.diary.feature.settings.resources.settings_list_style

/**
 * 목록 보기(FR-018, 스펙 008)
 * 맨 위 견본은 홈과 같은 일기 줄·카드·격자 칸으로 고른 대로 그림
 * 고르면 바로 바뀌고 기억함. 서식 보기는 Plus만 켤 수 있고, 아니면 Plus 안내 시트
 * [canFormat]이 null이면 요금제를 아직 모름. 산 사람에게 꺼짐이나 Plus 안내를 보이지 않게 저장한 값을 누를 수 없게 보임
 */
@Composable
fun ListStyleSettingsScreen(
    list: ListPreferences,
    canFormat: Boolean?,
    previewStyle: TextStyle,
    today: LocalDate,
    onStyle: (ListStyle) -> Unit,
    onLength: (PreviewLength) -> Unit,
    onFormatting: (Boolean) -> Unit,
    onPlus: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var askPlus by rememberSaveable { mutableStateOf(false) }
    var lengthSheet by rememberSaveable { mutableStateOf(false) }
    val formatting = list.showFormatting && canFormat != false
    Column(
        modifier.fillMaxSize().background(DiaryTheme.colors.canvas).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DiaryTopBar(title = stringResource(Res.string.settings_list_style), onBack = onBack)
        // 글자를 키우거나 가로로 두면 줄이 화면을 넘을 수 있음
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            ListPreview(
                list,
                formatting = formatting,
                previewStyle = previewStyle,
                today = today,
                // 상자 아래 8과 첫 묶음 머리 위 20(Figma G4)
                modifier = Modifier.padding(horizontal = LocalScreenGutter.current).padding(top = DiarySpacing.x4, bottom = DiarySpacing.x2)
            )
            DiarySettingsHeader(stringResource(Res.string.list_group_style))
            for (style in ListStyle.entries) {
                DiarySettingsChoiceRow(stringResource(style.label()), selected = list.listStyle == style, onClick = { onStyle(style) })
            }
            // 사진 격자는 글을 칸 크기만큼만 보여 길이와 서식이 상관없음
            if (list.listStyle != ListStyle.PHOTO_GRID) {
                DiarySettingsHeader(stringResource(Res.string.list_group_body), gapAbove = true)
                DiarySettingsRow(
                    stringResource(Res.string.list_group_length),
                    onClick = { lengthSheet = true },
                    value = stringResource(list.previewLength.label())
                )
                DiarySettingsSwitchRow(
                    stringResource(Res.string.list_formatting),
                    checked = formatting,
                    onCheckedChange = { checked -> if (canFormat == true) onFormatting(checked) else askPlus = true },
                    description = stringResource(
                        if (canFormat ==
                            false
                        ) {
                            Res.string.list_formatting_body_free
                        } else {
                            Res.string.list_formatting_body
                        }
                    ),
                    enabled = canFormat != null
                )
            }
        }
    }
    if (askPlus) FormattingPlusSheet(onPlus = onPlus, onDismiss = { askPlus = false })
    if (lengthSheet) {
        LengthSheet(list.previewLength, onDismiss = { lengthSheet = false }) {
            onLength(it)
            lengthSheet = false
        }
    }
}

/** 홈과 같은 부품으로 견본 일기 한 편(사진 격자는 세 칸) */
@Composable
private fun ListPreview(
    list: ListPreferences,
    formatting: Boolean,
    previewStyle: TextStyle,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val dates = rememberDiaryDateFormat()
    val sample = sampleRow(today, list.previewLength, formatting)
    PreviewBox(stringResource(Res.string.font_preview_label), modifier) {
        when (list.listStyle) {
            // 상자 안쪽 여백이 있어 줄의 화면 여백은 빼고, 위는 카드처럼 이름표와 8, 아래는 상자 안쪽만
            ListStyle.FEED ->
                CompositionLocalProvider(LocalScreenGutter provides Dp.Hairline) {
                    DiaryEntryLine(
                        sample,
                        showDate = true,
                        dividerAbove = false,
                        modifier = Modifier.padding(top = DiarySpacing.x2),
                        previewStyle = previewStyle,
                        verticalPadding = Dp.Hairline
                    )
                }

            ListStyle.CARD -> DiaryEntryCard(sample, previewStyle, dates, Modifier.padding(top = DiarySpacing.x2))

            // 사진 칸 둘 사이에 글 칸 하나. 홈 사진 격자처럼 사진이 먼저 보임
            ListStyle.PHOTO_GRID ->
                Row(Modifier.padding(top = DiarySpacing.x2), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
                    SamplePhoto(Res.drawable.list_sample_photo_mountain, Modifier.weight(1f))
                    Box(Modifier.weight(1f).aspectRatio(1f).background(DiaryTheme.colors.canvas)) {
                        DiaryGridCellContent(sample, today, dates)
                    }
                    SamplePhoto(Res.drawable.list_sample_photo_sea, Modifier.weight(1f))
                }
        }
    }
}

/**
 * 예시 일기. 기분·날씨·장소가 다 있음
 * 다섯 줄이 넘고 가운데 빈 줄이 있어 3줄·5줄(빈 줄을 빼고 이음)과 전체(쓴 그대로)가 다르게 보임
 */
@Composable
private fun sampleRow(today: LocalDate, length: PreviewLength, formatting: Boolean): EntryRow {
    val body = stringResource(Res.string.list_sample_body)
    val place = stringResource(Res.string.list_sample_place)
    return remember(body, place, today, length, formatting) {
        val text = PreviewText.shape(body, length)
        EntryRow(
            id = "sample",
            date = today,
            time = SAMPLE_TIME,
            preview = text,
            mood = Mood.GOOD,
            weather = Weather.SUNNY,
            placeName = place,
            isFavorite = false,
            photos = emptyList(),
            previewLines = PreviewText.lines(length),
            previewSpans = if (formatting) sampleSpans(text) else emptyList()
        )
    }
}

// 첫 줄 굵게, 셋째 줄 형광펜. 언어마다 글이 달라 줄로 정함
private fun sampleSpans(text: String): List<RichSpan> {
    val lines = text.split('\n')
    val spans = ArrayList<RichSpan>()
    var start = 0
    lines.forEachIndexed { index, line ->
        when (index) {
            0 -> spans += RichSpan(start, start + line.length, RichStyle.Bold)
            2 -> spans += RichSpan(start, start + line.length, RichStyle.Mark(MarkColor.YELLOW))
        }
        start += line.length + 1
    }
    return spans.filter { it.start < it.end }
}

/** 서식 보기는 Plus 기능. 체크리스트 시트와 같은 꼴 */
@Composable
private fun FormattingPlusSheet(onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss,
        title = stringResource(Res.string.list_formatting_plus_title),
        description = stringResource(Res.string.list_formatting_plus_body)
    ) {
        DiaryButtonPair(
            secondary = stringResource(Res.string.list_plus_later),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.list_plus_action),
            onPrimary = {
                onDismiss()
                onPlus()
            },
            size = DiaryButtonSize.XLarge,
            secondaryKind = DiaryButtonKind.Weak
        )
    }
}

private val SAMPLE_TIME = LocalTime(9, 0)

/** 본문 길이 고르기. 고르면 닫히고 위 미리보기가 바뀜 */
@Composable
private fun LengthSheet(selected: PreviewLength, onDismiss: () -> Unit, onSelect: (PreviewLength) -> Unit) {
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.list_group_length)) {
        Column(Modifier.fillMaxWidth()) {
            for (length in PreviewLength.entries) {
                DiarySheetChoiceRow(stringResource(length.label()), selected = length == selected, onClick = { onSelect(length) })
            }
        }
    }
}

/** 견본 사진 칸. 앱이 그린 그림이라 사용자 사진이 아님 */
@Composable
private fun SamplePhoto(photo: DrawableResource, modifier: Modifier = Modifier) {
    Image(painterResource(photo), contentDescription = null, modifier = modifier.aspectRatio(1f), contentScale = ContentScale.Crop)
}
