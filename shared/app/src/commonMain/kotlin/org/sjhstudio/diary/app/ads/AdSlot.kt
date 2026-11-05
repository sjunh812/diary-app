package org.sjhstudio.diary.app.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_remove
import org.sjhstudio.diary.app.resources.ad_rest
import org.sjhstudio.diary.app.resources.reward_body
import org.sjhstudio.diary.app.resources.reward_close
import org.sjhstudio.diary.app.resources.reward_title
import org.sjhstudio.diary.app.resources.reward_watch
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.plus.AdFreeSheet

/** 네이티브 광고 한 자리(Figma P5). 유료이거나 광고를 못 받으면 아무것도 그리지 않아 목록이 그대로 이어진다(US1). */
@Composable
internal expect fun NativeAdCard(modifier: Modifier = Modifier)

/** 홈과 돌아보기의 광고 자리. 옆에 광고를 없애는 길을 둔다. */
@Composable
internal fun AdSlot(modifier: Modifier = Modifier) {
    val ads = koinInject<AdRepository>()
    val show by ads.showAds.collectAsStateWithLifecycle(false)
    if (!show) return
    var sheet by remember { mutableStateOf<AdSheet?>(null) }
    val scope = rememberCoroutineScope()
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxWidth().padding(vertical = DiarySpacing.x2)) {
        NativeAdCard()
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
            DiaryButton(stringResource(Res.string.ad_remove), { sheet = AdSheet.Remove }, kind = DiaryButtonKind.Text)
            DiaryButton(stringResource(Res.string.ad_rest), { sheet = AdSheet.Reward }, kind = DiaryButtonKind.Text)
        }
    }
    when (sheet) {
        AdSheet.Remove -> AdFreeSheet(onDismiss = { sheet = null })

        AdSheet.Reward ->
            DiaryBottomSheet({ sheet = null }, title = stringResource(Res.string.reward_title)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x6, vertical = DiarySpacing.x3),
                    verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
                ) {
                    BasicText(stringResource(Res.string.reward_body), style = DiaryTheme.typography.body.copy(color = colors.textSecondary))
                    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                        DiaryButton(
                            stringResource(Res.string.reward_close),
                            { sheet = null },
                            Modifier.weight(1f),
                            kind = DiaryButtonKind.Weak,
                            size = DiaryButtonSize.XLarge
                        )
                        DiaryButton(
                            stringResource(Res.string.reward_watch),
                            {
                                sheet = null
                                scope.launch { ads.watchReward() }
                            },
                            Modifier.weight(2f),
                            size = DiaryButtonSize.XLarge
                        )
                    }
                }
            }

        null -> Unit
    }
}

private enum class AdSheet { Remove, Reward }
