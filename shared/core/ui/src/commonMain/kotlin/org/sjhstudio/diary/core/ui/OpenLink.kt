package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

/**
 * 주소 열기. 열 앱이 없으면 false
 * 안드로이드는 메일 앱이 없으면 예외로 앱이 꺼지고 iOS는 조용히 아무 일도 하지 않아서 둘 다 결과로 받음
 */
@Composable
expect fun rememberOpenLink(): (String) -> Boolean
