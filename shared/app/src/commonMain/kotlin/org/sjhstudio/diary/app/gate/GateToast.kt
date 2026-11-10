package org.sjhstudio.diary.app.gate

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import org.sjhstudio.diary.core.designsystem.component.DiaryRailMinWidth
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing

/**
 * 하단 탭 높이(`docs/design.md` 하단 탭).
 * 시스템 내비게이션 높이는 뺀 값.
 */
private val TabBarHeight = DiarySpacing.x14 + DiarySpacing.x2

/**
 * 관문을 지난 뒤 앱 틀 위에 뜨는 토스트의 아래 여백(`docs/design.md` 토스트).
 * 폰은 탭 위 16, 레일을 쓰는 넓은 화면은 화면 아래 16.
 * 시스템 내비게이션 높이는 쓰는 쪽에서 더함.
 */
@Composable
internal fun toastBottomOverApp(): Dp {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return if (width < DiaryRailMinWidth) TabBarHeight + DiarySpacing.x4 else DiarySpacing.x4
}
