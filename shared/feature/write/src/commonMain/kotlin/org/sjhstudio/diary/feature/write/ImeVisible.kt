package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

/** 화면 키보드가 보이는지 */
@Composable
internal expect fun imeVisible(): Boolean

/**
 * 키보드가 화면 아래 자리를 차지하는지
 * 키보드가 움직이는 프레임마다 다시 그리지 않게 바뀔 때만 읽음
 */
@Composable
internal fun imeTakesSpace(): Boolean {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val takes by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    return takes
}
