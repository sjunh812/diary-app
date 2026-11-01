package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 진행 막대(`docs/design.md` 진행 막대, TDS ProgressBar bold 8). [progress]는 0~1. */
@Composable
fun DiaryProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(DiaryShapes.full)
                .background(DiaryTheme.colors.sunken)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) }
    ) {
        Box(Modifier.fillMaxWidth(value).fillMaxHeight().clip(DiaryShapes.full).background(DiaryTheme.colors.accent))
    }
}
