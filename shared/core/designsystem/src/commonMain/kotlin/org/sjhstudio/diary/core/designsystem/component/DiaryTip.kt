package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
 * 맥락 팁 한 줄(Apple TipKit의 줄 안 팁)
 * 화면을 막지 않고, 닫으면 다시 뜨지 않음
 * 토스트와 같은 색이라 강조를 늘리지 않음
 */
@Composable
fun DiaryTip(
    text: String,
    closeLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .widthIn(max = DiaryWriteSize.bodyMax)
            .fillMaxWidth()
            .clip(DiaryShapes.r12)
            .background(colors.toast),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 글 앞의 안내 아이콘으로 실패 토스트와 생김새를 가름(Apple TipKit 팁처럼)
        // 색은 토스트 위 강조색이라 빨간 경고로 읽히지 않으면서 눈이 먼저 감
        DiaryIcon(
            DiaryIcons.Info,
            contentDescription = null,
            tint = colors.toastAction,
            size = DiaryIconSize.i16,
            modifier = Modifier.padding(start = DiarySpacing.x4, end = DiarySpacing.x2)
        )
        // iOS는 한글 낱말 가운데서도 줄을 바꿔 "있어 / 요."처럼 끊김
        // 상태 상자 머리처럼 그릴 때만 붙임 글자를 넣고, 화면 읽기와 시험에는 원래 글을 줌
        BasicText(
            keepWords(text),
            style = DiaryTheme.typography.caption.copy(color = colors.onToast),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = DiarySpacing.x3, bottom = DiarySpacing.x3)
                    .clearAndSetSemantics { this.text = AnnotatedString(text) }
        )
        // 팁이 가리키는 곳으로 바로 가는 글자 버튼(누르는 칸 높이 48, 보이는 면 위아래 6·좌우 8·모서리 10)
        // 글 칸을 넓히려고 보이는 면 좌우와 글과의 사이를 좁힘. 큰 글자에서 글이 서너 글자씩 끊겼음
        // 닫기는 누르는 칸 48을 두고 보이는 자리만 32로 좁혀 버튼과 한 묶음으로 보이게 함
        Row(Modifier.padding(start = DiarySpacing.x1, end = DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
            if (action != null) {
                val actionPress = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .heightIn(min = DiarySpacing.minTouchTarget)
                        .clickable(interactionSource = actionPress, indication = null, role = Role.Button, onClick = onAction),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        action,
                        style = DiaryTheme.typography.captionStrong.copy(color = colors.toastAction),
                        maxLines = 1,
                        modifier =
                            Modifier
                                .diaryPress(actionPress, colors.onToast, DiaryShapes.r10)
                                .padding(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x1_5)
                    )
                }
            }
            Box(
                Modifier
                    .diaryTouchOutset(horizontal = DiarySpacing.x2)
                    .size(DiarySpacing.minTouchTarget)
                    .diaryPress(interaction, colors.onToast, DiaryShapes.full)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Close, closeLabel, colors.onToast, size = DiaryIconSize.i16)
            }
        }
    }
}
