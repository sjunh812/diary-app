package org.sjhstudio.diary.app.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.app.scaffold.FirstAdNoticeDialog
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

@Preview(name = "첫 전면 광고 안내 · 라이트", locale = "ko")
@Composable
private fun FirstAdNoticeLight() = DiaryTheme(darkTheme = false) { FirstAdNoticeDialog({}, {}) }

@Preview(name = "첫 전면 광고 안내 · 다크", locale = "ko")
@Composable
private fun FirstAdNoticeDark() = DiaryTheme(darkTheme = true) { FirstAdNoticeDialog({}, {}) }
