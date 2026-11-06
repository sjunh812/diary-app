package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.background
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

/**
 * 잠금 배지 위 여백.
 * 상단 바가 없는 화면은 배지가 상태 표시줄 아래 92에 온다(Figma L1·L2).
 */
internal val BadgeTop = DiarySpacing.x14 + DiarySpacing.x8 + DiarySpacing.x1

/**
 * 잠금 화면(L1)과 비밀번호 정하기(L2)가 같이 쓰는 틀.
 * 위에는 배지·제목·점, 아래에는 숫자판을 붙인다.
 * 화면이 낮으면 통째로 굴린다.
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
            Spacer(Modifier.height(DiarySpacing.x10))
            DiaryPinDots(filled = entered, wrongCount = wrongCount)
            // 틀림·기다림 문구는 점 아래에 둔다.
            // 점 아래는 숫자판까지 빈 자리라 문구가 생겨도 점과 숫자판이 움직이지 않는다
            // (제목과 점 사이를 벌리지 않는다, 2026-11-03 사용자 지적).
            // 문구가 없어도 한 줄을 비워 두어 아래 안내 상자가 들썩이지 않는다.
            Spacer(Modifier.height(DiarySpacing.x4))
            BasicText(
                line?.text.orEmpty(),
                style = DiaryTheme.typography.body.copy(color = line?.color ?: colors.textPrimary, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = DiarySpacing.screenCompact).semantics { liveRegion = LiveRegionMode.Polite }
            )
            middle()
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(horizontal = DiarySpacing.screenCompact), horizontalAlignment = Alignment.CenterHorizontally) {
                bottom()
            }
        }
    }
}
