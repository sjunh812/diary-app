package org.sjhstudio.diary.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// iosApp의 ContentView가 이 화면을 띄운다.
// Swift에서 화면처럼 읽히도록 대문자로 시작하는 KMP 관례를 따른다.
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { DiaryApp() }
