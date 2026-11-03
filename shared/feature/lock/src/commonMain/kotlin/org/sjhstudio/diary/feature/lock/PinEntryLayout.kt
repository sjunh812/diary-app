package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.component.DiaryPinDots
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 제목 아래 한 줄(틀림, 기다림, 서로 다름). */
internal class PinLine(val text: String, val color: Color)

/** 잠금 배지 위 여백. 상단 바가 없는 화면은 배지가 상태 표시줄 아래 92에 온다(Figma L1·L2). */
internal val BadgeTop = DiarySpacing.x14 + DiarySpacing.x8 + DiarySpacing.x1

/**
 * 잠금 화면(L1)과 비밀번호 정하기(L2)가 같이 쓰는 틀.
 * 위에는 배지·제목·점, 아래에는 숫자판을 붙인다. 화면이 낮으면 통째로 굴린다.
 */
@Composable
internal fun PinEntryLayout(
    title: String,
    line: PinLine?,
    entered: Int,
    wrongCount: Int,
    modifier: Modifier = Modifier,
    top: @Composable ColumnScope.() -> Unit = { Spacer(Modifier.height(BadgeTop)) },
    middle: @Composable ColumnScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.canvas).safeDrawingPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            top()
            DiaryHeroBadge(DiaryIcons.Lock)
            Spacer(Modifier.height(DiarySpacing.x5))
            BasicText(
                title,
                style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = DiarySpacing.screenCompact).semantics { heading() }
            )
            // 안내 줄 자리는 늘 비워 둔다. 틀림·기다림 문구가 생겨도 점과 숫자판이 밀리지 않는다(AOSP 잠금 화면의 안내 자리).
            Spacer(Modifier.height(DiarySpacing.x2))
            val lineStyle = DiaryTheme.typography.body
            val lineHeight = with(LocalDensity.current) { lineStyle.lineHeight.toDp() }
            Box(
                Modifier.fillMaxWidth().heightIn(min = lineHeight).padding(horizontal = DiarySpacing.screenCompact),
                contentAlignment = Alignment.Center
            ) {
                if (line != null) {
                    BasicText(
                        line.text,
                        style = lineStyle.copy(color = line.color, textAlign = TextAlign.Center),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
            Spacer(Modifier.height(DiarySpacing.x6 + DiarySpacing.x1_5))
            DiaryPinDots(filled = entered, wrongCount = wrongCount)
            middle()
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(horizontal = DiarySpacing.screenCompact), horizontalAlignment = Alignment.CenterHorizontally) {
                bottom()
            }
        }
    }
}
