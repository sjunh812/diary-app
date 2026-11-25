package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable

// iOS는 보임 값을 주지 않아 키보드가 차지하는 자리로 봄
@Composable
internal actual fun imeVisible(): Boolean = imeTakesSpace()
