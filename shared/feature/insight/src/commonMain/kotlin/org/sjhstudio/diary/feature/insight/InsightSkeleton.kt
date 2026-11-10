package org.sjhstudio.diary.feature.insight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryInsightSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.ui.DiarySkeleton
import org.sjhstudio.diary.core.ui.SkeletonBlock
import org.sjhstudio.diary.core.ui.SkeletonLine

private const val SUMMARY_WIDTH = 0.7f
private const val WEEK_DAYS = 7
private const val MONTH_WEEKS = 5
private const val YEAR_MONTHS = 12
private const val MOOD_ROWS = 4
private const val ALL_YEARS = 6
private const val VALUE_ROWS = 3
private const val VALUE_SECTIONS = 2

/** 탭마다 칸 수와 줄 수가 달라 곧 나올 모양대로 그림(`docs/design.md` 스켈레톤) */
private fun InsightKind.flowSlots(): Int = when (this) {
    InsightKind.Week -> WEEK_DAYS
    InsightKind.Month -> MONTH_WEEKS
    InsightKind.Year -> YEAR_MONTHS
    InsightKind.All -> ALL_YEARS
}

/** 돌아보기 탭의 자리: 요약, 기분 흐름, 연간 지도(년만), 기분 막대 */
@Composable
internal fun InsightSkeleton(kind: InsightKind, modifier: Modifier = Modifier) {
    DiarySkeleton(modifier) {
        Column(Modifier.fillMaxWidth()) {
            SkeletonLine(Modifier.fillMaxWidth(SUMMARY_WIDTH), DiarySpacing.x6)
            Box(Modifier.height(DiarySpacing.x2))
            SkeletonLine(Modifier.width(DiarySpacing.x14 + DiarySpacing.x6))
            Box(Modifier.height(DiarySpacing.x6))
            SectionLine()
            Box(Modifier.height(DiarySpacing.x3))
            val wide = kind == InsightKind.Year || kind == InsightKind.All
            FlowSlots(kind.flowSlots(), if (wide) WIDE_COLUMNS else kind.flowSlots())
            Box(Modifier.height(DiarySpacing.x6))
            if (kind == InsightKind.Year) {
                SectionLine()
                Box(Modifier.height(DiarySpacing.x3))
                SkeletonBlock(Modifier.fillMaxWidth().height(DiarySpacing.x14 * 2), DiaryShapes.r8)
                Box(Modifier.height(DiarySpacing.x6))
            }
            SectionLine()
            repeat(MOOD_ROWS) { MoodRow() }
        }
    }
}

@Composable
private fun SectionLine() = SkeletonLine(Modifier.width(DiarySpacing.x14 + DiarySpacing.x10))

@Composable
private fun FlowSlots(slots: Int, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        for (row in (0 until slots).chunked(columns)) {
            Row(Modifier.fillMaxWidth()) {
                row.forEach { _ -> FlowSlot(DiaryInsightSize.flowFace, Modifier.weight(1f)) }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FlowSlot(face: Dp, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
        SkeletonBlock(Modifier.size(face), DiaryShapes.full)
        SkeletonLine(Modifier.width(DiarySpacing.x5), DiarySpacing.x2)
    }
}

@Composable
private fun MoodRow() {
    Row(
        Modifier.fillMaxWidth().heightIn(min = DiaryInsightSize.barRow),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        SkeletonBlock(Modifier.size(DiaryInsightSize.barFace), DiaryShapes.full)
        SkeletonLine(Modifier.width(DiarySpacing.x8))
        SkeletonLine(Modifier.weight(1f), DiaryInsightSize.barHeight)
        SkeletonLine(Modifier.width(DiarySpacing.x10))
    }
}

/** 고급 통계의 자리: 요즘 기분 카드와 값 줄 묶음 */
@Composable
internal fun AdvancedSkeleton(modifier: Modifier = Modifier) {
    DiarySkeleton(modifier) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
            SkeletonBlock(Modifier.fillMaxWidth().height(DiarySpacing.x14 + DiarySpacing.x10), DiaryShapes.r16)
            repeat(VALUE_SECTIONS) {
                Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
                    SkeletonLine(Modifier.width(DiarySpacing.x14 + DiarySpacing.x10), DiarySpacing.x4)
                    repeat(VALUE_ROWS) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonLine(Modifier.width(DiarySpacing.x14))
                            Box(Modifier.weight(1f))
                            SkeletonLine(Modifier.width(DiarySpacing.x10))
                        }
                    }
                }
            }
        }
    }
}
