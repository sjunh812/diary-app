package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichTextState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.ui.icon
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_add_photo
import org.sjhstudio.diary.feature.write.resources.write_camera
import org.sjhstudio.diary.feature.write.resources.write_color_blue
import org.sjhstudio.diary.feature.write.resources.write_color_gray
import org.sjhstudio.diary.feature.write.resources.write_color_green
import org.sjhstudio.diary.feature.write.resources.write_color_orange
import org.sjhstudio.diary.feature.write.resources.write_color_pink
import org.sjhstudio.diary.feature.write.resources.write_color_purple
import org.sjhstudio.diary.feature.write.resources.write_color_red
import org.sjhstudio.diary.feature.write.resources.write_color_yellow
import org.sjhstudio.diary.feature.write.resources.write_format_bold
import org.sjhstudio.diary.feature.write.resources.write_format_check
import org.sjhstudio.diary.feature.write.resources.write_format_color
import org.sjhstudio.diary.feature.write.resources.write_format_list
import org.sjhstudio.diary.feature.write.resources.write_format_mark
import org.sjhstudio.diary.feature.write.resources.write_format_underline
import org.sjhstudio.diary.feature.write.resources.write_ink_default
import org.sjhstudio.diary.feature.write.resources.write_mark_none
import org.sjhstudio.diary.feature.write.resources.write_palette_back

// 쓰기 화면의 꾸미기 도구 막대와 색 고르기

/** 도구 줄이 바뀌는 팔레트. 글자색과 형광펜은 쓰임이 달라 각자의 색만 보임 */
internal enum class PaletteKind { Ink, Mark }

/**
 * 키보드 위 서식 도구(Figma "서식 도구 줄")
 * 글자색·형광펜을 누르면 줄이 그 팔레트로 바뀜
 */
internal class WriteTools(
    val rich: RichTextState,
    val colors: EditorColors,
    val checklistOpen: () -> Boolean,
    val onChecklistLocked: () -> Unit
) {
    /** 서식 없는 본문. 글이 바뀔 때만 다시 만듦 */
    private val plain = derivedStateOf { rich.annotatedString.let { rich.toText() } }

    @Composable
    fun Toolbar(onPhoto: (() -> Unit)?, onCamera: (() -> Unit)?, modifier: Modifier = Modifier) {
        var palette by remember { mutableStateOf<PaletteKind?>(null) }
        val theme = DiaryTheme.colors
        Column(modifier.fillMaxWidth().background(theme.canvas), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(DiarySpacing.hairline).background(theme.line))
            Row(
                Modifier
                    .widthIn(max = DiaryWriteSize.bodyMax)
                    .fillMaxWidth()
                    .height(DiaryWriteSize.toolbar)
                    .padding(horizontal = DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)
            ) {
                val open = palette
                if (open != null) Palette(open) { palette = null } else Tools(onPhoto, onCamera) { palette = it }
            }
        }
    }

    @Composable
    private fun RowScope.Tools(onPhoto: (() -> Unit)?, onCamera: (() -> Unit)?, openPalette: (PaletteKind) -> Unit) {
        val theme = DiaryTheme.colors
        val style = rich.currentSpanStyle
        // 좁은 폰에서는 칸을 고르게 줄여 도구가 모두 보이게
        val slot = Modifier.weight(1f, fill = false)
        val checked = Checklist.isItemAt(plain.value, rich.selection.min)
        fun tint(active: Boolean) = if (active) theme.accentText else theme.textPrimary
        onPhoto?.let {
            ToolButton(DiaryIcons.Image, stringResource(Res.string.write_add_photo), theme.textPrimary, modifier = slot, onClick = it)
        }
        onCamera?.let {
            ToolButton(DiaryIcons.Camera, stringResource(Res.string.write_camera), theme.textPrimary, modifier = slot, onClick = it)
        }
        val bold = style.fontWeight == FontWeight.Bold
        ToolButton(DiaryIcons.Bold, stringResource(Res.string.write_format_bold), tint(bold), modifier = slot, active = bold, onClick = {
            rich.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        })
        val underline = style.textDecoration?.contains(TextDecoration.Underline) == true
        ToolButton(
            DiaryIcons.Underline,
            stringResource(
                Res.string.write_format_underline
            ),
            tint(underline),
            modifier = slot,
            active = underline,
            onClick = {
                rich.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            }
        )
        // 글자색 도구는 지금 고른 색을 그대로 보임
        ToolButton(
            DiaryIcons.TextColor,
            stringResource(Res.string.write_format_color),
            style.color.takeOrElse {
                theme.textPrimary
            },
            modifier = slot,
            onClick = {
                openPalette(PaletteKind.Ink)
            }
        )
        val marked = style.background.isSpecified
        ToolButton(
            DiaryIcons.Highlight,
            stringResource(
                Res.string.write_format_mark
            ),
            tint(marked),
            modifier = slot,
            active = marked,
            onClick = {
                openPalette(PaletteKind.Mark)
            }
        )
        ToolButton(
            DiaryIcons.BulletList,
            stringResource(Res.string.write_format_list),
            tint(rich.isUnorderedList),
            modifier = slot,
            active = rich.isUnorderedList,
            onClick = {
                if (checked) toggleChecklist()
                rich.toggleUnorderedList()
            }
        )
        ToolButton(
            DiaryIcons.Checklist,
            stringResource(
                Res.string.write_format_check
            ),
            tint(checked),
            modifier = slot,
            active = checked,
            onClick = {
                // 이미 있는 항목을 푸는 것은 언제나 됨. 새로 만드는 것만 Plus
                if (!checked && !checklistOpen()) {
                    onChecklistLocked()
                    return@ToolButton
                }
                if (rich.isUnorderedList) rich.toggleUnorderedList()
                toggleChecklist()
            }
        )
    }

    private fun toggleChecklist() {
        Checklist.toggleLines(rich.toText(), rich.selection.min, rich.selection.max).forEach { rich.applyEdit(it) }
    }

    @Composable
    private fun RowScope.Palette(kind: PaletteKind, close: () -> Unit) {
        val theme = DiaryTheme.colors
        val style = rich.currentSpanStyle
        ToolButton(DiaryIcons.ChevronLeft, stringResource(Res.string.write_palette_back), theme.textPrimary, onClick = close)
        // 고른 색이 몇 개든 한 번에 풀어 여러 색이 섞인 글도 기본으로 돌아감
        fun clearInk() = colors.ink.values.forEach { rich.removeSpanStyle(SpanStyle(color = it)) }
        fun clearMark() = colors.mark.values.forEach { rich.removeSpanStyle(SpanStyle(background = it)) }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            when (kind) {
                PaletteKind.Ink -> {
                    Swatch(stringResource(Res.string.write_ink_default), selected = style.color.isUnspecified, onClick = ::clearInk) {
                        ColorChip(theme.textPrimary, DiaryShapes.full)
                    }
                    for (ink in InkColor.entries) {
                        val color = colors.ink.getValue(ink)
                        Swatch(stringResource(ink.label()), selected = style.color == color, onClick = {
                            clearInk()
                            rich.addSpanStyle(SpanStyle(color = color))
                        }) { ColorChip(color, DiaryShapes.full) }
                    }
                }

                PaletteKind.Mark -> {
                    Swatch(
                        stringResource(Res.string.write_mark_none),
                        selected = style.background.isUnspecified,
                        ring = DiaryShapes.r12,
                        onClick = ::clearMark
                    ) {
                        Box(
                            Modifier.size(DiaryWriteSize.swatch).border(DiarySpacing.hairline, theme.lineStrong, DiaryShapes.r8),
                            contentAlignment = Alignment.Center
                        ) {
                            DiaryIcon(DiaryIcons.Close, contentDescription = null, tint = theme.textTertiary, size = DiaryIconSize.i16)
                        }
                    }
                    for (mark in MarkColor.entries) {
                        val color = colors.mark.getValue(mark)
                        Swatch(stringResource(mark.label()), selected = style.background == color, ring = DiaryShapes.r12, onClick = {
                            clearMark()
                            rich.addSpanStyle(SpanStyle(background = color))
                        }) { ColorChip(color, DiaryShapes.r8) }
                    }
                }
            }
        }
    }
}

internal fun InkColor.label(): StringResource = when (this) {
    InkColor.GRAY -> Res.string.write_color_gray
    InkColor.RED -> Res.string.write_color_red
    InkColor.ORANGE -> Res.string.write_color_orange
    InkColor.GREEN -> Res.string.write_color_green
    InkColor.BLUE -> Res.string.write_color_blue
    InkColor.PURPLE -> Res.string.write_color_purple
    InkColor.PINK -> Res.string.write_color_pink
}

internal fun MarkColor.label(): StringResource = when (this) {
    MarkColor.YELLOW -> Res.string.write_color_yellow
    MarkColor.GREEN -> Res.string.write_color_green
    MarkColor.BLUE -> Res.string.write_color_blue
    MarkColor.PURPLE -> Res.string.write_color_purple
    MarkColor.PINK -> Res.string.write_color_pink
}

@Composable
internal fun ColorChip(color: Color, shape: Shape) {
    Box(Modifier.size(DiaryWriteSize.swatch).background(color, shape))
}

/** 고른 색은 액센트 고리로 보임. 고리와 색 사이를 띄워 보라 칸에서도 고리가 구별됨 */
@Composable
internal fun Swatch(
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    ring: Shape = DiaryShapes.full,
    chip: @Composable () -> Unit
) {
    val theme = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(width = DiaryWriteSize.swatch + DiarySpacing.x3, height = DiarySpacing.minTouchTarget)
            .selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(DiaryWriteSize.swatch + DiarySpacing.x2)
                .diaryPress(interaction, theme.textPrimary, ring)
                .then(if (selected) Modifier.border(DiarySpacing.x0_5, theme.accent, ring) else Modifier),
            contentAlignment = Alignment.Center
        ) { chip() }
    }
}

@Composable
internal fun ToolButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean? = null
) {
    val theme = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, theme.textPrimary, DiaryShapes.full)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            // 켜고 끄는 도구만 화면 읽기에 켜짐 상태를 알림
            .then(if (active != null) Modifier.semantics { selected = active } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 켜진 도구는 색만이 아니라 바탕으로도 알림
        if (active == true) Box(Modifier.size(DiaryWriteSize.swatch + DiarySpacing.x2).background(theme.selected, DiaryShapes.r12))
        DiaryIcon(icon, contentDescription = description, tint = tint)
    }
}

/** 저장이 이보다 오래 걸리면 진행 점을 보임 */
internal const val SAVE_PROGRESS_DELAY_MS = 300L
