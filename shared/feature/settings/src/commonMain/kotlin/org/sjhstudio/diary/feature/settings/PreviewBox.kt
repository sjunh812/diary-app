package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 글꼴, 글자 크기, 목록 보기 화면의 견본 상자
 * 두 화면이 같은 상자를 써야 오가도 견본 자리가 움직이지 않음
 * 견본은 앱이 보이는 글이라 안내 글처럼 낱말 단위로 줄을 바꿈(Android 12 이하·iOS)
 */
@Composable
internal fun PreviewBox(label: String, sample: String, sampleStyle: TextStyle, modifier: Modifier = Modifier) =
    PreviewBox(label, modifier) { PhraseText(sample, style = sampleStyle.copy(color = DiaryTheme.colors.textPrimary)) }

/** 견본 자리에 일기 줄처럼 다른 것을 그리는 상자(목록 보기, 스펙 008) */
@Composable
internal fun PreviewBox(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = DiaryTheme.colors
    Column(
        modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken).padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(label, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        content()
    }
}
