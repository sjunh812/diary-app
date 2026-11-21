package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 진행 막대 높이(`docs/design.md` 진행 막대)
 * 화면의 주 내용인 진행은 [Regular](TDS ProgressBar bold 8), 상태 상자 안 곁 진행은 [Compact](Material 3 선형 진행 기본 4)
 */
enum class DiaryProgressSize(val height: Dp) {
    Regular(8.dp),
    Compact(4.dp)
}
