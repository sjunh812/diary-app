package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

// 설정 줄(`docs/design.md` 설정 줄, TDS ListRow, M3 1줄 56)
// 최소 56 · 위아래 14 · 좌우 화면 여백 · 이름 reading. 설명이 붙으면 이름 아래 caption 두 줄짜리

/** 오른쪽 값이 줄에서 차지할 수 있는 최대 몫. 넘으면 값을 말줄임해 이름 자리를 남김 */
private const val VALUE_MAX_SHARE = 0.5f

/** 오른쪽 값 최대 줄 수 */
private const val VALUE_MAX_LINES = 2

// 값과 할 일 사이
private const val VALUE_ACTION_SEPARATOR = " · "

/**
 * 이름 최대 줄 수
 * 영어처럼 긴 이름이 글자 200%에서 오른쪽 값과 나란히 서면 두 줄에 다 들어가지 않음
 */
private const val TITLE_MAX_LINES = 3

/**
 * 설정 묶음 머리(SEED ListHeader 14 Medium)
 * [gapAbove]면 위 여백을 20 대신 24로 넓혀 앞 묶음과 나눔
 * 여백 20은 줄 사이와 비슷해 묶음이 한 덩어리로 보임. 화면의 첫 묶음은 20.
 */
@Composable
fun DiarySettingsHeader(text: String, modifier: Modifier = Modifier, gapAbove: Boolean = false) {
    BasicText(
        text = text,
        style = DiaryTheme.typography.label.copy(color = DiaryTheme.colors.textSecondary),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = LocalScreenGutter.current,
                    end = LocalScreenGutter.current,
                    top = if (gapAbove) DiarySpacing.x6 else DiarySpacing.x5,
                    bottom = DiarySpacing.x2
                )
                .semantics { heading() }
    )
}

/**
 * 누르면 다음 화면으로 가는 줄. 오른쪽에 지금 값과 화살표
 * [valueAction]은 값 뒤 " · "에 붙는 할 일(예: "2.0.1 · 업데이트"). 그 말만 `accentText`
 */
@Composable
fun DiarySettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    value: String? = null,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
    valueAction: String? = null
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    ValueRowFrame(
        modifier =
            modifier
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .background(if (selected) colors.selected else Color.Transparent)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                ),
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        value = if (value != null || valueAction != null) valueText(value, valueAction, colors.accentText) else null,
        chevron = true
    )
}

/**
 * 값만 보여 주는 줄. 누를 수 없어 화살표가 없음
 * [valueNote]는 값 뒤 " · "에 붙는 지금 상태(예: "2.0.1 · 받는 중 42%"). 누를 수 없어 같은 회색
 */
@Composable
fun DiarySettingsValueRow(title: String, value: String, modifier: Modifier = Modifier, valueNote: String? = null) {
    ValueRowFrame(
        modifier = modifier,
        title = title,
        description = null,
        leadingIcon = null,
        value = valueText(value, valueNote, DiaryTheme.colors.textSecondary),
        chevron = false
    )
}

private fun valueText(value: String?, action: String?, actionColor: Color): AnnotatedString = buildAnnotatedString {
    if (value != null) append(value)
    if (action != null) {
        if (value != null) append(VALUE_ACTION_SEPARATOR)
        withStyle(SpanStyle(color = actionColor)) { append(action) }
    }
}

/**
 * 값이 있는 줄(이름 · 값 · 화살표)
 * 값은 남은 폭의 반까지 두 줄 안에 들어가면 화살표 옆에 두고, 넘치면 이름 아래 온 폭으로 내림
 * 큰 글자에서 값을 말줄임하지 않으려는 것(HIG 값 셀이 큰 글자에서 아래로 내려가는 것과 같음)
 * `weight`로 나누면 짧은 값일 때 이름 칸이 반으로 고정돼 값과 화살표가 줄 가운데에 뜸
 */
@Composable
private fun ValueRowFrame(
    modifier: Modifier,
    title: String,
    description: String?,
    leadingIcon: ImageVector?,
    value: AnnotatedString?,
    chevron: Boolean
) {
    val colors = DiaryTheme.colors
    val valueStyle = DiaryTheme.typography.body.copy(color = colors.textSecondary)
    val measurer = rememberTextMeasurer(cacheSize = 2)
    Layout(
        content = {
            if (leadingIcon !=
                null
            ) {
                DiaryIcon(leadingIcon, contentDescription = null, tint = colors.textPrimary, Modifier.layoutId(Slot.Leading))
            }
            TitleBlock(title, description, badge = null, Modifier.layoutId(Slot.Title))
            if (value != null) {
                BasicText(
                    text = value,
                    modifier = Modifier.layoutId(Slot.Value),
                    style = valueStyle,
                    maxLines = VALUE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chevron) {
                DiaryIcon(
                    DiaryIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.layoutId(Slot.Chevron),
                    size = DiaryIconSize.i20
                )
            }
        },
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = LocalScreenGutter.current, vertical = 14.dp)
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val gap = DiarySpacing.x3.roundToPx()
        // 값은 화살표에 붙여 둠(Figma 설정 줄 4)
        val valueGap = DiarySpacing.x1.roundToPx()
        val lineGap = DiarySpacing.x0_5.roundToPx()
        val leading = measurables.firstOrNull { it.layoutId == Slot.Leading }?.measure(loose)
        val arrow = measurables.firstOrNull { it.layoutId == Slot.Chevron }?.measure(loose)
        val start = leading?.let { it.width + gap } ?: 0
        val textWidth = (constraints.maxWidth - start).coerceAtLeast(0)
        val arrowWidth = arrow?.width ?: 0
        val sideMax = ((textWidth - gap - arrowWidth - (if (arrow != null) valueGap else 0)) * VALUE_MAX_SHARE).toInt().coerceAtLeast(0)
        val stacked =
            value != null &&
                measurer
                    .measure(
                        value,
                        valueStyle,
                        maxLines = VALUE_MAX_LINES,
                        constraints = Constraints(maxWidth = sideMax),
                        layoutDirection = layoutDirection
                    )
                    .hasVisualOverflow
        val valueMeasurable = measurables.firstOrNull { it.layoutId == Slot.Value }
        val titleMeasurable = measurables.first { it.layoutId == Slot.Title }
        val arrowSpace = if (arrow != null) gap + arrowWidth else 0
        val placeValue: Placeable?
        val title: Placeable
        if (stacked) {
            val blockWidth = (textWidth - arrowSpace).coerceAtLeast(0)
            title = titleMeasurable.measure(loose.copy(maxWidth = blockWidth))
            placeValue = valueMeasurable?.measure(loose.copy(maxWidth = blockWidth))
        } else {
            placeValue = valueMeasurable?.measure(loose.copy(maxWidth = sideMax))
            val trailing = (placeValue?.width?.plus(if (arrow != null) valueGap else 0) ?: 0) + arrowWidth
            title = titleMeasurable.measure(loose.copy(maxWidth = (textWidth - (if (trailing > 0) gap + trailing else 0)).coerceAtLeast(0)))
        }
        val block = if (stacked) title.height + lineGap + (placeValue?.height ?: 0) else title.height
        val height =
            maxOf(block, leading?.height ?: 0, arrow?.height ?: 0, if (stacked) 0 else placeValue?.height ?: 0, constraints.minHeight)
        val width = constraints.maxWidth
        layout(width, height) {
            leading?.placeRelative(0, (height - leading.height) / 2)
            arrow?.placeRelative(width - arrow.width, (height - arrow.height) / 2)
            val top = (height - block) / 2
            title.placeRelative(start, top)
            if (placeValue != null) {
                if (stacked) {
                    placeValue.placeRelative(start, top + title.height + lineGap)
                } else {
                    val end = width - (if (arrow != null) arrowWidth + valueGap else 0)
                    placeValue.placeRelative(end - placeValue.width, (height - placeValue.height) / 2)
                }
            }
        }
    }
}

private enum class Slot { Leading, Title, Value, Chevron }

/** 오른쪽에 스위치가 있는 줄. 줄 전체가 누르는 곳 */
@Composable
fun DiarySettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    badge: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    RowFrame(
        badge = badge,
        modifier =
            modifier.diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.r12).toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = onCheckedChange
            ),
        title = title,
        description = description
    ) {
        DiarySwitchMark(checked = checked, enabled = enabled)
    }
}

/**
 * 하나만 고르는 목록의 줄(`docs/design.md` 고르기 목록)
 * 고른 줄 오른쪽에 체크 24.
 */
@Composable
fun DiarySettingsChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    RowFrame(
        modifier =
            modifier.diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.r12).selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        title = title,
        description = description
    ) {
        if (selected) {
            DiaryIcon(
                DiaryIcons.CheckSelected,
                contentDescription = null,
                tint = DiaryTheme.colors.accentText,
                size = DiaryIconSize.i20
            )
        }
    }
}

/**
 * 제목 자리를 직접 그리는 고르기 줄(글꼴 목록처럼 이름을 그 글꼴이나 그림으로 그릴 때)
 * [label]은 스크린 리더이 읽는 이름
 * 오른쪽은 [trailing]이 정하고, 없으면 고른 줄에 체크를 둠
 */
@Composable
fun DiarySettingsChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = DiaryTheme.colors
    Row(
        modifier =
            modifier
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .semantics(mergeDescendants = true) { contentDescription = label }
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = LocalScreenGutter.current, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).clearAndSetSemantics {}) { title() }
        when {
            trailing != null -> trailing()
            selected -> DiaryIcon(DiaryIcons.CheckSelected, contentDescription = null, tint = colors.accentText, size = DiaryIconSize.i20)
        }
    }
}

@Composable
private fun RowFrame(
    modifier: Modifier,
    title: String,
    description: String?,
    leadingIcon: ImageVector? = null,
    badge: String? = null,
    trailing: @Composable RowScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = LocalScreenGutter.current, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) DiaryIcon(leadingIcon, contentDescription = null, tint = colors.textPrimary)
        TitleBlock(title, description, badge, Modifier.weight(1f))
        trailing()
    }
}

/** 줄 이름과 그 아래 설명. 이름 옆 Plus 칩은 이름이 길어도 밀려나지 않음 */
@Composable
private fun TitleBlock(title: String, description: String?, badge: String?, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = DiaryTheme.typography.reading.copy(color = colors.textPrimary),
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            if (badge != null) DiaryPlusChip(badge)
        }
        if (description != null) {
            PhraseText(text = description, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        }
    }
}

/**
 * 시트 안 고르기 목록 줄(`docs/design.md` 고르기 목록)
 * 글은 시트 안쪽 선에 맞추고 누름 면만 양옆으로 12 나옴
 */
@Composable
fun DiarySheetChoiceRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    CompositionLocalProvider(LocalScreenGutter provides DiarySpacing.x3) {
        DiarySettingsChoiceRow(
            title = title,
            selected = selected,
            onClick = onClick,
            description = description,
            modifier =
                modifier.layout { measurable, constraints ->
                    val outset = DiarySpacing.x3.roundToPx()
                    val width = constraints.maxWidth + outset * 2
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-outset, 0) }
                }
        )
    }
}
