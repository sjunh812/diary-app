package org.sjhstudio.diary.core.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.EntryMenu

@Preview(name = "일기 메뉴 · 즐겨찾기 넣기 · 라이트", locale = "ko", widthDp = 390, heightDp = 500)
@Composable
private fun EntryMenuAddLight() = DiaryTheme(darkTheme = false) { EntryMenu(false, {}, {}, {}, {}, {}) }

@Preview(name = "일기 메뉴 · 즐겨찾기 넣기 · 다크", locale = "ko", widthDp = 390, heightDp = 500)
@Composable
private fun EntryMenuAddDark() = DiaryTheme(darkTheme = true) { EntryMenu(false, {}, {}, {}, {}, {}) }

@Preview(name = "일기 메뉴 · 즐겨찾기 해제 · 라이트", locale = "ko", widthDp = 390, heightDp = 500)
@Composable
private fun EntryMenuRemoveLight() = DiaryTheme(darkTheme = false) { EntryMenu(true, {}, {}, {}, {}, {}) }

@Preview(name = "일기 메뉴 · 즐겨찾기 해제 · 다크", locale = "ko", widthDp = 390, heightDp = 500)
@Composable
private fun EntryMenuRemoveDark() = DiaryTheme(darkTheme = true) { EntryMenu(true, {}, {}, {}, {}, {}) }
