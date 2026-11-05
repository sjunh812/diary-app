package org.sjhstudio.diary.app.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_close
import org.sjhstudio.diary.app.resources.reward_body
import org.sjhstudio.diary.app.resources.reward_close
import org.sjhstudio.diary.app.resources.reward_title
import org.sjhstudio.diary.app.resources.reward_watch
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.plus.AdFreeSheet

/** 광고 모양. 얇은 줄은 글만, 카드는 왼쪽에 그림이나 영상을 둔다(`docs/business.md` 광고 자리). */
enum class AdShape { Row, Card }

/**
 * 네이티브 광고 한 자리(Figma P5). 유료이거나 광고를 못 받으면 아무것도 그리지 않아 목록이 그대로 이어진다(US1).
 * [key]가 같으면 스크롤해 다시 보여도 같은 광고를 쓴다. [frame]은 광고를 받은 뒤에만 불러 빈 자리가 남지 않는다.
 */
@Composable
internal expect fun NativeAdCard(key: String, shape: AdShape, frame: @Composable (ad: @Composable () -> Unit) -> Unit)

/** 홈과 돌아보기의 광고 자리. 오른쪽 위 ×를 누르면 광고를 없애는 길을 보여 준다. */
@Composable
internal fun AdSlot(key: String, shape: AdShape, modifier: Modifier = Modifier) {
    val ads = koinInject<AdRepository>()
    val show by ads.showAds.collectAsStateWithLifecycle(false)
    if (!show) return
    var sheet by remember { mutableStateOf<AdSheet?>(null) }
    val scope = rememberCoroutineScope()
    val colors = DiaryTheme.colors
    NativeAdCard(key, shape) { ad ->
        Box(modifier.fillMaxWidth().padding(vertical = DiarySpacing.x2)) {
            ad()
            DiaryIconButton(
                DiaryIcons.Close,
                stringResource(Res.string.ad_close),
                { sheet = AdSheet.Remove },
                Modifier.align(Alignment.TopEnd),
                tint = colors.textTertiary,
                iconSize = DiaryIconSize.i16
            )
        }
    }
    when (sheet) {
        AdSheet.Remove -> AdFreeSheet(onDismiss = { sheet = null }, onDayOff = { sheet = AdSheet.Reward })

        AdSheet.Reward ->
            DiaryBottomSheet(
                { sheet = null },
                title = stringResource(Res.string.reward_title),
                description = stringResource(Res.string.reward_body)
            ) {
                DiaryButtonPair(
                    secondary = stringResource(Res.string.reward_close),
                    onSecondary = { sheet = null },
                    primary = stringResource(Res.string.reward_watch),
                    onPrimary = {
                        sheet = null
                        scope.launch { ads.watchReward() }
                    },
                    size = DiaryButtonSize.XLarge
                )
            }

        null -> Unit
    }
}

private enum class AdSheet { Remove, Reward }
