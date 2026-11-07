package org.sjhstudio.diary.app

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// iosApp의 ContentView가 이 화면을 띄운다.
// Swift에서 화면처럼 읽히도록 대문자로 시작하는 KMP 관례를 따른다.
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController(
    // 화면이 키보드 높이를 직접 비운다.
    // 기본값이면 화면 전체가 한 번 더 밀려 글이 상태 표시줄 뒤로 올라가고 도구 줄이 키보드에서 떨어진다.
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
) { DiaryApp() }
