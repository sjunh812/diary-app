package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.limitFontScale

/**
 * Plus로 쓰는 기능 표시 칩(테마 견본, 설정 줄)
 * [separator]를 주면 그 색 테두리로 아래에 겹친 것과 떨어져 보임
 */
@Composable
fun DiaryPlusChip(text: String, modifier: Modifier = Modifier, separator: Color? = null) {
    val colors = DiaryTheme.colors
    val ring = if (separator == null) Modifier else Modifier.border(CHIP_BORDER, separator, DiaryShapes.full).padding(CHIP_BORDER)
    Box(
        modifier
            .then(ring)
            .background(colors.selected, DiaryShapes.full)
            // 고른 글꼴이 넓어도 글자가 칩 밖으로 나가지 않게 폭은 글자를 따름
            .defaultMinSize(minWidth = CHIP_WIDTH, minHeight = CHIP_HEIGHT)
            .padding(horizontal = DiarySpacing.x1),
        contentAlignment = Alignment.Center
    ) {
        // 높이가 정해진 배지라 글자 크기는 1.3배까지만. 더 키우면 견본·설정 줄 밖으로 넘침
        BasicText(text, style = DiaryTheme.typography.micro.limitFontScale().copy(color = colors.accentText))
    }
}

private val CHIP_WIDTH = 30.dp
private val CHIP_HEIGHT = 14.dp
private val CHIP_BORDER = DiarySpacing.x0_5

/**
 * 한 가지를 짚어 주는 회색 꼬리표(클레이 미리보기의 꾸미기)
 * 강조색은 Plus 칩과 버튼에만 두어 꼬리표는 바탕에 가깝게
 */
@Composable
fun DiaryTag(text: String, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Box(
        modifier
            .background(colors.sunken, DiaryShapes.full)
            .padding(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x1)
    ) {
        BasicText(text, style = DiaryTheme.typography.micro.copy(color = colors.textSecondary))
    }
}
