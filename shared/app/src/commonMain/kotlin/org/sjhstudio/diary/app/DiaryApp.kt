package org.sjhstudio.diary.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 안드로이드와 iOS가 같이 띄우는 앱의 뿌리.
// 테마, 시작 관문, 앱 틀은 다음 단계(스펙 001 T029)에서 채운다.
@Composable
fun DiaryApp() {
    Box(modifier = Modifier.fillMaxSize())
}
