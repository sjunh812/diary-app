package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

// iOS는 보임 값을 주지 않아 키보드 높이로 봄
// 키보드가 움직이는 프레임마다 다시 그리지 않게 보임이 바뀔 때만 읽음
@Composable
internal actual fun imeVisible(): Boolean {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val visible by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    return visible
}
