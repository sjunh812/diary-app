package org.sjhstudio.diary.app

import androidx.compose.runtime.Composable
import org.sjhstudio.diary.core.model.ThemeMode

// Android는 MainActivity가 설정을 보고 상태·내비게이션 막대 색을 맞춤
@Composable
internal actual fun ApplyPlatformTheme(mode: ThemeMode) = Unit
