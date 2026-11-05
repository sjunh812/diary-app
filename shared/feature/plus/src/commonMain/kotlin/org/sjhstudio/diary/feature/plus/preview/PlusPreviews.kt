package org.sjhstudio.diary.feature.plus.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.plus.PaywallScreen
import org.sjhstudio.diary.feature.plus.PlusManageScreen

@Preview(locale = "ko")
@Composable
private fun PaywallLight() = DiaryTheme(darkTheme = false) { PaywallScreen("₩14,900", false, false, false, {}, {}, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun PaywallDark() = DiaryTheme(darkTheme = true) { PaywallScreen("₩14,900", false, false, false, {}, {}, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun PaywallNoStoreLight() = DiaryTheme(darkTheme = false) { PaywallScreen(null, false, false, false, {}, {}, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun ManageLight() = DiaryTheme(darkTheme = false) { PlusManageScreen("₩14,900", {}, {}) }

@Preview(locale = "ko")
@Composable
private fun ManageDark() = DiaryTheme(darkTheme = true) { PlusManageScreen("₩14,900", {}, {}) }

@Preview(name = "결제 화면 · 넓은 화면", locale = "ko", widthDp = 1194, heightDp = 834)
@Composable
private fun PaywallWide() = DiaryTheme(darkTheme = false) { PaywallScreen("₩14,900", false, false, false, {}, {}, {}, {}) }

@Preview(name = "구독 중 · 넓은 화면", locale = "ko", widthDp = 1194, heightDp = 834)
@Composable
private fun ManageWide() = DiaryTheme(darkTheme = false) { PlusManageScreen("₩14,900", {}, {}) }
