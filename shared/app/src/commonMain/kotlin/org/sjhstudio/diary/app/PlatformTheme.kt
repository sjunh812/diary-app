package org.sjhstudio.diary.app

import androidx.compose.runtime.Composable
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 앱 화면 모드를 시스템 화면(상태 표시줄, 시스템 창)에도 알림
 * 휴대폰은 라이트, 앱만 다크일 때 상태 표시줄 글자가 바탕에 묻히지 않게
 * [dark]는 앱이 정한 화면 모드. 앱 화면과 시스템 화면이 늘 같은 값을 보게 한 곳에서만 정함
 */
@Composable
internal expect fun ApplyPlatformTheme(mode: ThemeMode, dark: Boolean)
