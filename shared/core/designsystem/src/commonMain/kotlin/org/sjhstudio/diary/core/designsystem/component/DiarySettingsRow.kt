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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// 설정 줄(`docs/design.md` 설정 줄, TDS ListRow, M3 1줄 56).
// 최소 56 · 위아래 14 · 좌우 20 · 이름 reading. 설명이 붙으면 이름 아래 caption 두 줄짜리다.

/** 설정 묶음 머리(SEED ListHeader 14 Medium). */
@Composable
fun DiarySettingsHeader(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = DiaryTheme.typography.label.copy(color = DiaryTheme.colors.textSecondary),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = DiarySpacing.x5, end = DiarySpacing.x5, top = DiarySpacing.x5, bottom = DiarySpacing.x2)
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
        if (value != null) {
            BasicText(text = value, style = DiaryTheme.typography.body.copy(color = colors.textSecondary), maxLines = 1)
        }
        DiaryIcon(DiaryIcons.ChevronRight, contentDescription = null, tint = colors.textTertiary, size = DiaryIconSize.i20)
    }
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
                .padding(horizontal = DiarySpacing.x5, vertical = 14.dp),
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
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = DiarySpacing.x5, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) DiaryIcon(leadingIcon, contentDescription = null, tint = colors.textPrimary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
            BasicText(
                text = title,
                style = DiaryTheme.typography.reading.copy(color = colors.textPrimary),
                overflow = TextOverflow.Ellipsis
            )
            if (description != null) {
                BasicText(text = description, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
            }
        }
        trailing()
    }
}
