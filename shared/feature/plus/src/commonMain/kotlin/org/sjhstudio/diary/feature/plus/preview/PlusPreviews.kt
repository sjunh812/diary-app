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
private fun ManageLight() = DiaryTheme(darkTheme = false) { PlusManageScreen({}, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun ManageDark() = DiaryTheme(darkTheme = true) { PlusManageScreen({}, {}, {}) }
