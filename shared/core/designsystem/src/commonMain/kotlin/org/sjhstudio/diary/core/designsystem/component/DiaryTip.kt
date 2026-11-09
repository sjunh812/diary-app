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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize

/**
 * 맥락 팁 한 줄(Apple TipKit의 줄 안 팁).
 * 화면을 막지 않고 닫으면 다시 뜨지 않는다.
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
            .clip(DiaryShapes.r12)
            .background(colors.toast),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        // 글 앞의 안내 아이콘이 실패 토스트와 생김새를 가른다(2026-11-08 사용자 제안, Apple TipKit 팁처럼).
        // 색은 토스트 위 강조색이라 빨간 경고로 읽히지 않으면서 눈이 먼저 간다.
        DiaryIcon(
            DiaryIcons.Info,
            contentDescription = null,
            tint = colors.toastAction,
            size = DiaryIconSize.i16,
            modifier = Modifier.padding(start = DiarySpacing.x4)
        )
        // iOS는 한글 낱말 가운데서도 줄을 바꿔 "있어 / 요."처럼 끊겼다(2026-11-09, Figma에서 발견).
        // 상태 상자 머리와 같이 그릴 때만 붙임 글자를 넣고, 화면 읽기와 시험에는 원래 글을 준다.
        BasicText(
            keepWords(text),
            style = DiaryTheme.typography.caption.copy(color = colors.onToast),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = DiarySpacing.x3, bottom = DiarySpacing.x3)
                    .clearAndSetSemantics { this.text = AnnotatedString(text) }
        )
        Box(
            Modifier
                .size(DiarySpacing.minTouchTarget)
                .diaryPress(interaction, colors.onToast, DiaryShapes.full)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(DiaryIcons.Close, closeLabel, colors.onToast, size = DiaryIconSize.i16)
        }
    }
}
