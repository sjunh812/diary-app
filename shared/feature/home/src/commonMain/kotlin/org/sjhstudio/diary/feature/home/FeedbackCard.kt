package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.feedback_card_action
import org.sjhstudio.diary.feature.home.resources.feedback_card_body
import org.sjhstudio.diary.feature.home.resources.feedback_card_close
import org.sjhstudio.diary.feature.home.resources.feedback_card_title

/**
 * 홈 목록 맨 위 의견 카드(스펙 007, Figma 홈 상태 "S1 · 의견 카드")
 * 백업 권하기 카드와 같은 상자. 누르거나 닫으면 다시 보이지 않음
 */
@Composable
fun FeedbackCard(onSend: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    DiaryStatusBox(
        DiaryIcons.MessageCircleMore,
        stringResource(Res.string.feedback_card_title),
        stringResource(Res.string.feedback_card_body),
        // 위아래 12. 백업 카드와 같은 자리
        modifier.padding(vertical = DiarySpacing.x3),
        actionLabel = stringResource(Res.string.feedback_card_action),
        onAction = onSend,
        secondaryLabel = stringResource(Res.string.feedback_card_close),
        onSecondary = onClose,
        secondaryKind = DiaryButtonKind.Text
    )
}
