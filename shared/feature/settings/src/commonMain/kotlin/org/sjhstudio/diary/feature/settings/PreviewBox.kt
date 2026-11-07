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
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 글꼴과 글자 크기 화면의 견본 상자.
 * 두 화면이 같은 상자를 써야 오가도 견본 자리가 움직이지 않는다.
 */
@Composable
internal fun PreviewBox(label: String, sample: String, sampleStyle: TextStyle, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(
        modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken).padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(label, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        BasicText(sample, style = sampleStyle.copy(color = colors.textPrimary))
    }
}
