package org.sjhstudio.diary.feature.plus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
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
import org.sjhstudio.diary.feature.plus.resources.plus_no_store

/** 광고만 없애는 시트(Figma P2). 광고의 ×가 연다. 온 화면은 그대로다. [onDayOff]는 영상을 보고 하루만 없애는 길이다. */
@Composable
fun AdFreeSheet(onDismiss: () -> Unit, onDayOff: (() -> Unit)? = null) {
    val viewModel = koinViewModel<PlusViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val price = state.price(ProductId.AD_FREE)
    DiaryBottomSheet(onDismiss, title = stringResource(Res.string.adfree_title)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                BasicText(stringResource(Res.string.adfree_body), style = typography.body.copy(color = colors.textSecondary))
                if (price != null) {
                    BasicText(stringResource(Res.string.adfree_price, price), style = typography.title2.copy(color = colors.textPrimary))
                } else if (!state.loading) {
                    BasicText(stringResource(Res.string.plus_no_store), style = typography.body.copy(color = colors.textSecondary))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                    DiaryButton(
                        stringResource(Res.string.adfree_close),
                        onDismiss,
                        Modifier.weight(1f),
                        kind = DiaryButtonKind.Weak,
                        size = DiaryButtonSize.XLarge
                    )
                    DiaryButton(
                        stringResource(Res.string.adfree_buy),
                        { viewModel.buy(ProductId.AD_FREE, onDismiss) },
                        Modifier.weight(2f),
                        size = DiaryButtonSize.XLarge,
                        enabled = price != null && !state.busy
                    )
                }
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
