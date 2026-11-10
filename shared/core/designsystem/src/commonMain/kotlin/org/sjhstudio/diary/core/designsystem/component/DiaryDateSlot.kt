package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 기간 고르기에서 다음에 채울 칸. */
enum class DiaryDateSlot { Start, End }

/**
 * 기간 달력 위의 시작일·종료일 두 칸.
 * 다음에 채울 칸에 액센트 테두리를 둬, 날짜를 두 번 누른다는 것을 안내 글 없이 알림.
 * Material 3 기간 달력의 머리, 숙소 앱의 체크인·체크아웃 칸과 같은 방식.
 * 고른 날짜는 그 칸에 바로 들어감.
 * [onEnd]가 null이면 종료일 칸을 누를 수 없음(시작일을 고르기 전).
 */
@Composable
fun DiaryDateSlots(
    startLabel: String,
    start: String?,
    endLabel: String,
    end: String?,
    placeholder: String,
    active: DiaryDateSlot?,
    onStart: () -> Unit,
    onEnd: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        Slot(startLabel, start, placeholder, active == DiaryDateSlot.Start, onStart, Modifier.weight(1f))
        Slot(endLabel, end, placeholder, active == DiaryDateSlot.End, onEnd, Modifier.weight(1f))
    }
}

@Composable
private fun Slot(
    label: String,
    value: String?,
    placeholder: String,
    active: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val interaction = remember { MutableInteractionSource() }
    // 고르는 칸은 바탕을 비우고 테두리만 둬 입력창에 초점이 간 것처럼 보이게 함.
    // 테두리는 숫자 키패드 빈 칸과 같은 1.5. 1이면 옅은 보라가 바탕에 묻힘
    val shape = DiaryShapes.r12
    Column(
        modifier
            .clip(shape)
            .background(if (active) colors.surface else colors.sunken)
            .then(if (active) Modifier.border(ACTIVE_BORDER, colors.accent, shape) else Modifier)
            .diaryPress(interaction, colors.textPrimary, shape)
            .semantics { selected = active }
            .clickable(enabled = onClick != null, interactionSource = interaction, indication = null, role = Role.Button) {
                onClick?.invoke()
            }
            .padding(horizontal = DiarySpacing.x4, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)
    ) {
        BasicText(label, style = typography.caption.copy(color = colors.textSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        val valueStyle = if (value !=
            null
        ) {
            typography.bodyStrong.copy(color = colors.textPrimary)
        } else {
            typography.body.copy(color = colors.placeholder)
        }
        BasicText(value ?: placeholder, style = valueStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val ACTIVE_BORDER = 1.5.dp
