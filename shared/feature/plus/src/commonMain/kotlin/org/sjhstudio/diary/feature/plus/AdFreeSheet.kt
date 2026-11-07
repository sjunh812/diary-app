package org.sjhstudio.diary.feature.plus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.adfree_body
import org.sjhstudio.diary.feature.plus.resources.adfree_buy
import org.sjhstudio.diary.feature.plus.resources.adfree_close
import org.sjhstudio.diary.feature.plus.resources.adfree_day_off
import org.sjhstudio.diary.feature.plus.resources.adfree_price
import org.sjhstudio.diary.feature.plus.resources.adfree_title
import org.sjhstudio.diary.feature.plus.resources.plus_failed
import org.sjhstudio.diary.feature.plus.resources.plus_no_store
import org.sjhstudio.diary.feature.plus.resources.plus_retry

/**
 * 광고만 없애는 시트(Figma P2).
 * 광고의 ×가 연다.
 * 온 화면은 그대로다.
 * [onDayOff]는 영상을 보고 하루만 없애는 길이다.
 * 뷰모델이 오래 살아서 처음 오프라인이었어도 열 때마다 가격을 다시 받는다.
 */
@Composable
fun AdFreeSheet(onDismiss: () -> Unit, onDayOff: (() -> Unit)? = null) {
    val viewModel = koinViewModel<PlusViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }
    DisposableEffect(viewModel) { onDispose(viewModel::failureShown) }
    AdFreeSheetContent(
        state = state,
        onDismiss = onDismiss,
        onBuy = { viewModel.buy(ProductId.AD_FREE, onDismiss) },
        onRetry = viewModel::load,
        onDayOff = onDayOff
    )
}

@Composable
internal fun AdFreeSheetContent(
    state: PlusUiState,
    onDismiss: () -> Unit,
    onBuy: () -> Unit,
    onRetry: () -> Unit,
    onDayOff: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val price = state.price(ProductId.AD_FREE)
    // 가격을 못 받으면 결제 버튼 자리가 곧 다시 시도다.
    val noPrice = price == null && !state.loading
    DiaryBottomSheet(onDismiss, title = stringResource(Res.string.adfree_title), description = stringResource(Res.string.adfree_body)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                if (price != null) {
                    BasicText(stringResource(Res.string.adfree_price, price), style = typography.title2.copy(color = colors.textPrimary))
                } else if (noPrice) {
                    BasicText(stringResource(Res.string.plus_no_store), style = typography.body.copy(color = colors.textSecondary))
                }
                if (state.buyFailed) {
                    BasicText(stringResource(Res.string.plus_failed), style = typography.body.copy(color = colors.danger))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                DiaryButtonPair(
                    secondary = stringResource(Res.string.adfree_close),
                    onSecondary = onDismiss,
                    primary = stringResource(if (noPrice) Res.string.plus_retry else Res.string.adfree_buy),
                    onPrimary = if (noPrice) onRetry else onBuy,
                    size = DiaryButtonSize.XLarge,
                    primaryEnabled = (noPrice || price != null) && !state.busy
                )
                if (onDayOff != null) {
                    DiaryButton(
                        stringResource(Res.string.adfree_day_off),
                        onDayOff,
                        Modifier.align(Alignment.CenterHorizontally),
                        kind = DiaryButtonKind.Text
                    )
                }
            }
        }
    }
}
