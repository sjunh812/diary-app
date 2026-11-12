package org.sjhstudio.diary.app.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_close
import org.sjhstudio.diary.app.resources.reward_body
import org.sjhstudio.diary.app.resources.reward_close
import org.sjhstudio.diary.app.resources.reward_failed_body
import org.sjhstudio.diary.app.resources.reward_failed_title
import org.sjhstudio.diary.app.resources.reward_title
import org.sjhstudio.diary.app.resources.reward_watch
import org.sjhstudio.diary.core.data.AdRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.plus.AdFreeSheet

/**
 * 광고 모양
 * 얇은 줄은 글만, 카드는 왼쪽에 그림이나 영상을 둠(`docs/business.md` 광고 자리)
 */
enum class AdShape { Row, Card }

/**
 * 네이티브 광고 한 자리(Figma P5)
 * 유료이거나 광고를 못 받으면 아무것도 그리지 않아 목록이 그대로 이어짐(US1)
 * [key]가 같으면 스크롤해 다시 보여도 같은 광고를 씀
 * [frame]은 광고를 받은 뒤에만 불러 빈 자리가 남지 않음
 */
@Composable
internal expect fun NativeAdCard(key: String, shape: AdShape, frame: @Composable (ad: @Composable () -> Unit) -> Unit)

/**
 * 홈과 돌아보기의 광고 자리
 * 오른쪽 위 ×를 누르면 [onClose]. 앱 틀이 광고를 없애는 방법을 보여 줌
 * ×는 둥근 면 위에 있지만 광고 뷰 밖임. 플랫폼 쪽이 광고 뷰를 × 자리만큼 왼쪽에서 끝냄
 */
@Composable
internal fun AdSlot(key: String, shape: AdShape, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ads = koinInject<AdRepository>()
    val show by ads.showAds.collectAsStateWithLifecycle(false)
    if (!show) return
    val colors = DiaryTheme.colors
    NativeAdCard(key, shape) { ad ->
        Box(modifier.fillMaxWidth()) {
            ad()
            DiaryIconButton(
                DiaryIcons.Close,
                stringResource(Res.string.ad_close),
                onClose,
                Modifier.align(Alignment.TopEnd),
                tint = colors.textTertiary,
                iconSize = DiaryIconSize.i16
            )
        }
    }
}

/** 광고의 ×를 누른 뒤의 시트들. 광고 자리가 여럿이라 앱 틀에 한 번만 둠 */
@Composable
internal fun AdRemovalSheets(viewModel: AdRemovalViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.sheet) {
        AdRemovalSheet.Remove -> AdFreeSheet(onDismiss = viewModel::dismiss, onDayOff = viewModel::offerReward)

        AdRemovalSheet.Reward ->
            DiaryBottomSheet(
                viewModel::dismiss,
                title = stringResource(Res.string.reward_title),
                description = stringResource(Res.string.reward_body)
            ) {
                DiaryButtonPair(
                    secondary = stringResource(Res.string.reward_close),
                    onSecondary = viewModel::dismiss,
                    primary = stringResource(Res.string.reward_watch),
                    // 영상을 받는 몇 초 동안 시트를 두고 진행 점을 보임. 닫으면 눌린 줄 몰라 다시 누름
                    onPrimary = viewModel::watch,
                    size = DiaryButtonSize.XLarge,
                    primaryLoading = state.watching
                )
            }

        AdRemovalSheet.RewardFailed ->
            DiaryBottomSheet(
                viewModel::dismiss,
                title = stringResource(Res.string.reward_failed_title),
                description = stringResource(Res.string.reward_failed_body)
            ) {
                DiaryButton(
                    stringResource(Res.string.reward_close),
                    onClick = viewModel::dismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = DiarySpacing.x3),
                    size = DiaryButtonSize.XLarge
                )
            }

        null -> Unit
    }
}
