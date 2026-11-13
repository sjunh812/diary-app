package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.settings_text_size
import org.sjhstudio.diary.feature.settings.resources.text_size_note
import org.sjhstudio.diary.feature.settings.resources.text_size_sample_body
import org.sjhstudio.diary.feature.settings.resources.text_size_sample_glyph

/**
 * 본문 글자 크기(Figma "본문 글자 크기 (G2)", research R17)
 * 위 견본 글이 고른 크기로 바로 바뀌고 고른 본문 글꼴로 그림
 * 일기 본문에만 걸리고 앱 틀 글자는 기기 글자 크기를 따름
 */
@Composable
fun TextSizeSettingsScreen(
    selected: BodyTextSize,
    sampleDate: LocalDate,
    look: BodyLook,
    onSelect: (BodyTextSize) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val installed = rememberFileFontFamily(look.fontPath)
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.settings_text_size), onBack = onBack)
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.settingsMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalScreenGutter.current),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)
        ) {
            PreviewBox(
                // 일기 상세 제목과 같은 꼴로 보여 줌
                dates.dayWithWeekday(sampleDate),
                stringResource(Res.string.text_size_sample_body),
                diaryBodyTextStyle(look.font, selected) { installed },
                Modifier.padding(top = DiarySpacing.x4)
            )
            Row(Modifier.fillMaxWidth()) {
                for (size in BodyTextSize.entries) SizeStep(size, selected == size, Modifier.weight(1f)) { onSelect(size) }
            }
            PhraseText(stringResource(Res.string.text_size_note), style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        }
    }
}

/**
 * 단계 하나: 원 56 안의 견본 글자와 아래 이름
 * 고른 단계는 `selected` 바탕에 `accentText`
 * 칸은 다섯이 폭을 똑같이 나누고, 누름 면이 원과 이름에 붙지 않게 위아래 8.
 * 폰 폭에서 칸은 70이라 원 둘레 좌우는 7(`docs/design.md` 누름)
 */
@Composable
private fun SizeStep(size: BodyTextSize, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val tint = if (selected) colors.accentText else colors.textPrimary
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ).padding(vertical = DiarySpacing.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
    ) {
        Box(
            Modifier.size(DiarySpacing.x14).clip(DiaryShapes.full).background(if (selected) colors.selected else colors.sunken),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                stringResource(Res.string.text_size_sample_glyph),
                style = DiaryTheme.typography.reading.copy(color = tint, fontSize = size.fontSizeSp.sp, lineHeight = size.fontSizeSp.sp)
            )
        }
        BasicText(
            stringResource(size.label()),
            style = DiaryTheme.typography.micro.copy(color = if (selected) colors.accentText else colors.textSecondary)
        )
    }
}
