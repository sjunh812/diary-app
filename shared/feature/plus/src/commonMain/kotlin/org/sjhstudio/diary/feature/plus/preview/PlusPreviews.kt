package org.sjhstudio.diary.feature.plus.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.model.StoreProduct
import org.sjhstudio.diary.feature.plus.AdFreeSheetContent
import org.sjhstudio.diary.feature.plus.PaywallScreen
import org.sjhstudio.diary.feature.plus.PlusManageScreen
import org.sjhstudio.diary.feature.plus.PlusMessage
import org.sjhstudio.diary.feature.plus.PlusUiState

@Preview(locale = "ko")
@Composable
private fun PaywallLight() = DiaryTheme(darkTheme = false) { PaywallScreen("₩14,900", false, false, false, {}, {}, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun PaywallDark() = DiaryTheme(darkTheme = true) { PaywallScreen("₩14,900", false, false, false, {}, {}, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun PaywallNoStoreLight() = DiaryTheme(darkTheme = false) { PaywallScreen(null, false, false, false, {}, {}, {}, {}) }

@Preview(name = "결제 화면 · 가격 읽는 중", locale = "ko")
@Composable
private fun PaywallLoadingLight() = DiaryTheme(darkTheme = false) { PaywallScreen(null, true, false, false, {}, {}, {}, {}) }

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

private val AdFreePriced = PlusUiState(products = listOf(StoreProduct(ProductId.AD_FREE, "₩9,900")), loading = false)

@Preview(name = "광고 제거 시트", locale = "ko")
@Composable
private fun AdFreeLight() = DiaryTheme(darkTheme = false) { AdFreeSheetContent(AdFreePriced, {}, {}, {}, {}) }

@Preview(name = "광고 제거 시트 · 다크", locale = "ko")
@Composable
private fun AdFreeDark() = DiaryTheme(darkTheme = true) { AdFreeSheetContent(AdFreePriced, {}, {}, {}, {}) }

@Preview(name = "광고 제거 시트 · 가격 없음", locale = "ko")
@Composable
private fun AdFreeNoPrice() = DiaryTheme(darkTheme = false) { AdFreeSheetContent(PlusUiState(loading = false), {}, {}, {}, {}) }

@Preview(name = "광고 제거 시트 · 결제 실패", locale = "ko")
@Composable
private fun AdFreeFailed() = DiaryTheme(darkTheme = false) { AdFreeSheetContent(AdFreePriced.copy(buyFailed = true), {}, {}, {}) }
