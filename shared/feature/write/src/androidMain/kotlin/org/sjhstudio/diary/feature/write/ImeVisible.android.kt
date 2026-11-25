package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

// 높이가 아니라 보임으로 봐 떠 있는 키보드도 보이는 것
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun imeVisible(): Boolean = WindowInsets.isImeVisible
