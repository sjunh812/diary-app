package org.sjhstudio.diary.app.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_label
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.designsystem.theme.DiaryRadius
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import platform.UIKit.UIView

/**
 * Android와 같은 꼴의 네이티브 광고(NativeAdCard.android.kt)
 * `sunken` 면과 "광고" 표시로 일기 줄과 구별함(AdMob 네이티브 정책)
 * 받기 전이나 못 받으면 그리지 않음
 * UIKit 뷰 위에는 Compose 바탕과 자르기가 닿지 않아 면과 모서리는 Swift가 그림
 */
@Composable
internal actual fun NativeAdCard(key: String, shape: AdShape, frame: @Composable (ad: @Composable () -> Unit) -> Unit) {
    val player = koinInject<AdPlayer>() as? IosAdPlayer ?: return
    LaunchedEffect(key) { player.loadNative(key) }
    val style = rememberAdStyle()
    val view = if (player.loaded[key] ==
        true
    ) {
        remember(key, shape, style) { player.nativeView(key, shape == AdShape.Card, style) }
    } else {
        null
    }
    if (view != null) frame { AdView(player, view) }
}

@Composable
private fun rememberAdStyle(): IosAdStyle {
    val colors = DiaryTheme.colors
    val label = stringResource(Res.string.ad_label)
    return remember(label, colors) {
        IosAdStyle(
            label,
            colors.textPrimary.toArgb().toLong(),
            colors.textSecondary.toArgb().toLong(),
            colors.sunken.toArgb().toLong(),
            DiaryRadius.r16.value.toDouble()
        )
    }
}

/** 폭에 맞춰 Swift가 잰 높이로 둠 */
@Composable
private fun AdView(player: IosAdPlayer, view: UIView) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toDouble()
        val height = remember(view, width) { player.fittingHeight(view, width) }
        UIKitView(factory = { view }, modifier = Modifier.fillMaxWidth().height(height.dp))
    }
}
