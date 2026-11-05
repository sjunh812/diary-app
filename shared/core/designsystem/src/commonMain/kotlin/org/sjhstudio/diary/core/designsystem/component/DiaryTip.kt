package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

/**
 * 맥락 팁 한 줄(Apple TipKit의 줄 안 팁). 화면을 막지 않고 닫으면 다시 뜨지 않는다.
 * 토스트와 같은 색이라 강조를 늘리지 않는다.
 */
@Composable
fun DiaryTip(text: String, closeLabel: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .widthIn(max = DiaryWriteSize.bodyMax)
            .fillMaxWidth()
            .padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2)
            .clip(DiaryShapes.r12)
            .background(colors.toast),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        BasicText(
            text,
            style = DiaryTheme.typography.caption.copy(color = colors.onToast),
            modifier = Modifier.weight(1f).padding(start = DiarySpacing.x4, top = DiarySpacing.x3, bottom = DiarySpacing.x3)
        )
        Box(
            Modifier
                .size(DiarySpacing.minTouchTarget)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(DiaryIcons.Close, closeLabel, colors.onToast, size = DiaryIconSize.i16)
        }
    }
}
