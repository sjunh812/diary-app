package org.sjhstudio.diary.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 앱 밖에서 들어온 길(스펙 001 FR-022)
 * 플랫폼 쪽이 요청을 남기고, 앱 틀이 그려질 때 가져감
 * 앱 틀은 잠금과 기록 이전 관문을 지난 뒤에만 그려져 잠금을 풀어야 쓰기가 열림
 */
class AppLaunch {
    private val requested = MutableStateFlow(false)
    val writeRequested: StateFlow<Boolean> = requested.asStateFlow()

    /** "한 줄만 남겨 보세요" 알림을 누름 */
    fun requestWrite() {
        requested.value = true
    }

    internal fun writeHandled() {
        requested.value = false
    }
}

/** 들어온 요청대로 화면 열기 */
@Composable
internal fun OpenRequestedScreens(launch: AppLaunch, nav: TabNavigation) {
    val write by launch.writeRequested.collectAsStateWithLifecycle()
    LaunchedEffect(write) {
        if (!write) return@LaunchedEffect
        launch.writeHandled()
        // 쓰던 글이 있으면 쌓기가 그 글로 돌려보냄
        nav.push(WriteKey())
    }
}
