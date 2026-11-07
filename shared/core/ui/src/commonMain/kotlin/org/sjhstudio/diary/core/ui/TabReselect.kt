package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * 지금 탭을 다시 누른 횟수.
 * 앱 틀이 올리고, 탭 첫 화면이 듣고 맨 위로 간다.
 */
@Suppress("CompositionLocalAllowlist")
val LocalTabReselect = compositionLocalOf { 0 }

/** 지금 탭을 다시 누르면 [action]을 한다. 화면을 처음 열 때는 하지 않는다. */
@Composable
fun OnTabReselected(action: suspend () -> Unit) {
    val count = LocalTabReselect.current
    val opened = remember { count }
    val latest by rememberUpdatedState(action)
    LaunchedEffect(count) { if (count != opened) latest() }
}
