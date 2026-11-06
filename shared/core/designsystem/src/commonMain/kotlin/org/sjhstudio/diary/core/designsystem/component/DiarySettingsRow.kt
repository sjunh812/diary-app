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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

// 설정 줄(`docs/design.md` 설정 줄, TDS ListRow, M3 1줄 56).
// 최소 56 · 위아래 14 · 좌우 화면 여백 · 이름 reading. 설명이 붙으면 이름 아래 caption 두 줄짜리다.

/** 오른쪽 값이 줄에서 차지할 수 있는 몫의 끝. 넘으면 값을 말줄임해 이름 자리를 남긴다. */
private const val VALUE_MAX_SHARE = 0.5f

/** 이름 줄 수의 끝. */
private const val TITLE_MAX_LINES = 2

/**
 * 설정 묶음 머리(SEED ListHeader 14 Medium). [gapAbove]면 위 여백을 20 대신 24로 넓혀 앞 묶음과 나눈다
 * (2026-11-04 사용자 결정, 여백 20은 줄 사이와 비슷해 묶음이 한 덩어리로 보였다). 화면의 첫 묶음은 20이다.
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

/** 누르면 다음 화면으로 가는 줄. 오른쪽에 지금 값과 화살표. */
@Composable
fun DiarySettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    value: String? = null,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    RowFrame(
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
        leadingIcon = leadingIcon
    ) {
        if (value != null) RowValue(value)
        DiaryIcon(DiaryIcons.ChevronRight, contentDescription = null, tint = colors.textTertiary, size = DiaryIconSize.i20)
    }
}

/** 값만 보여 주는 줄. 누를 수 없어서 화살표를 그리지 않는다. */
@Composable
fun DiarySettingsValueRow(title: String, value: String, modifier: Modifier = Modifier) {
    RowFrame(modifier = modifier, title = title, description = null, leadingIcon = null) {
        RowValue(value)
    }
}

/**
 * 줄 오른쪽 값. 값이 길어도 이름을 밀어내지 않게 남은 폭의 반까지만 쓰고 말줄임한다.
 * `weight`로 나누면 짧은 값일 때 이름 칸이 반으로 고정되어 값과 화살표가 줄 가운데에 뜬다.
 */
@Composable
private fun RowValue(value: String) {
    BasicText(
        text = value,
        style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textSecondary),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(maxWidth = (constraints.maxWidth * VALUE_MAX_SHARE).toInt()))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
    )
}

/** 오른쪽에 스위치가 있는 줄. 줄 전체가 누르는 곳이다. */
@Composable
fun DiarySettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    RowFrame(
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

/** 하나만 고르는 목록의 줄(`docs/design.md` 고르기 목록). 고른 줄 오른쪽에 체크 24. */
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
        if (selected) DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = DiaryTheme.colors.accentText)
    }
}

/**
 * 제목 자리를 직접 그리는 고르기 줄(글꼴 목록처럼 이름을 그 글꼴이나 그림으로 그릴 때).
 * [label]은 화면 읽기 프로그램이 읽는 이름이다. 오른쪽은 [trailing]이 정하고, 없으면 고른 줄에 체크를 둔다.
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
            selected -> DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.accentText)
        }
    }
}

@Composable
private fun RowFrame(
    modifier: Modifier,
    title: String,
    description: String?,
    leadingIcon: ImageVector? = null,
    trailing: @Composable RowScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = LocalScreenGutter.current, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) DiaryIcon(leadingIcon, contentDescription = null, tint = colors.textPrimary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
            BasicText(
                text = title,
                style = DiaryTheme.typography.reading.copy(color = colors.textPrimary),
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            if (description != null) {
                BasicText(text = description, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
            }
        }
        trailing()
    }
}
