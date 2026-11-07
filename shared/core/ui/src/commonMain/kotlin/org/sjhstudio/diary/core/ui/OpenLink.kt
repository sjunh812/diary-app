package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

/**
 * 주소를 연다. 열 앱이 없으면 거짓이다.
 * 안드로이드는 메일 앱이 없으면 예외로 앱이 꺼지고, iOS는 조용히 아무 일도 하지 않아서 둘 다 결과로 받는다.
 */
@Composable
expect fun rememberOpenLink(): (String) -> Boolean
