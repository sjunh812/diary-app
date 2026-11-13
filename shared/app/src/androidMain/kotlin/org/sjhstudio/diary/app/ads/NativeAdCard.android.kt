package org.sjhstudio.diary.app.ads

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_label
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.designsystem.theme.DiaryAdSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 일기 줄과 구별되게 `sunken` 면과 "광고" 표시를 둠(AdMob 네이티브 정책)
 * 받기 전이나 못 받으면 그리지 않음
 * 글은 앱 글꼴과 토큰으로 그려 다크 모드와 고른 글꼴을 그대로 따름
 */
@Composable
internal actual fun NativeAdCard(key: String, shape: AdShape, frame: @Composable (ad: @Composable () -> Unit) -> Unit) {
    val player = koinInject<AdPlayer>() as? AdMobPlayer ?: return
    LaunchedEffect(key) { player.loadNative(key) }
    val ad = player.natives[key] ?: return
    frame {
        Box(
            // 오른쪽은 앱의 × 자리. 광고 뷰가 그 밑까지 오면 ×가 광고를 덮음(AdMob: 앱 콘텐츠가 네이티브 광고와 겹치면 안 됨)
            // 광고 뷰가 면 안쪽 16에 있어 SDK가 구석에 넣는 AdChoices가 모서리 곡선에 잘리지 않음
            Modifier
                .fillMaxWidth()
                .clip(DiaryShapes.r16)
                .background(DiaryTheme.colors.sunken)
                .padding(start = DiarySpacing.x4, top = DiarySpacing.x4, end = DiarySpacing.minTouchTarget, bottom = DiarySpacing.x4)
        ) {
            NativeAdFrame(ad) { adView ->
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                    if (shape == AdShape.Card) AdMedia(ad, adView) else AdIcon(ad, adView)
                    AdTexts(ad, adView, stacked = shape == AdShape.Card)
                }
            }
        }
    }
}

/**
 * [NativeAdView] 안에 Compose로 그린 광고를 담음
 * 요소가 모두 등록된 뒤에 광고를 붙여야 눌림이 이어져 안쪽이 두 프레임 그려진 뒤에 붙임
 */
@Composable
private fun NativeAdFrame(ad: NativeAd, content: @Composable (NativeAdView) -> Unit) {
    val context = LocalContext.current
    val adView = remember { NativeAdView(context) }
    AndroidView(
        factory = {
            adView.apply {
                addView(
                    ComposeView(context).apply { setContent { content(adView) } },
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    LaunchedEffect(ad) {
        withFrameNanos { }
        withFrameNanos { }
        adView.setNativeAd(ad)
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
}

/**
 * 광고 요소 하나를 안드로이드 뷰로 감싸 [register]로 [NativeAdView]에 등록
 * AdMob은 등록된 뷰의 눌림만 셈. 바탕까지 눌리게 하면 정책 위반이라 요소마다 따로 등록
 */
@Composable
private fun AdAsset(adView: NativeAdView, register: NativeAdView.(View) -> Unit, content: @Composable () -> Unit) {
    AndroidView(
        factory = { context ->
            ComposeView(context).apply {
                setContent(content)
                adView.register(this)
            }
        }
    )
}

// 정사각이 아닌 그림은 자르지 않고 칸 안에 맞춤(AdMob: 그림을 자르지 말 것). 남는 자리는 면보다 밝게
@Composable
private fun AdMedia(ad: NativeAd, adView: NativeAdView) {
    AndroidView(
        factory = { context ->
            MediaView(context).also {
                it.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
                it.mediaContent = ad.mediaContent
                adView.mediaView = it
            }
        },
        modifier = Modifier.size(DiaryAdSize.media).clip(DiaryShapes.r12).background(DiaryTheme.colors.surface)
    )
}

// 아이콘이 없는 광고는 글만
@Composable
private fun AdIcon(ad: NativeAd, adView: NativeAdView) {
    val drawable = ad.icon?.drawable ?: return
    AndroidView(
        factory = { context ->
            ImageView(context).also {
                it.scaleType = ImageView.ScaleType.CENTER_CROP
                it.setImageDrawable(drawable)
                adView.iconView = it
            }
        },
        modifier = Modifier.size(DiaryAdSize.icon).clip(DiaryShapes.r8)
    )
}

/**
 * 제목, "광고" 표시, 행동 버튼
 * 카드는 그림 옆에 세로로 쌓고, 얇은 줄은 표시와 버튼을 한 줄에 둬 높이를 줄임
 */
@Composable
private fun AdTexts(ad: NativeAd, adView: NativeAdView, stacked: Boolean) {
    val typography = DiaryTheme.typography
    val colors = DiaryTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)) {
        AdAsset(adView, register = { headlineView = it }) {
            // AdMob이 제목 앞부분을 자르지 말라고 해 두 줄까지 둠
            BasicText(
                ad.headline.orEmpty(),
                style = typography.bodyStrong.copy(color = colors.textPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (stacked) {
            AdLabel()
            AdCallToAction(ad, adView)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
                AdLabel()
                AdCallToAction(ad, adView)
            }
        }
    }
}

@Composable
private fun AdLabel() {
    BasicText(
        stringResource(Res.string.ad_label),
        modifier = Modifier
            .clip(DiaryShapes.r8)
            .background(DiaryTheme.colors.surface)
            .padding(horizontal = DiarySpacing.x1_5, vertical = DiarySpacing.x0_5),
        style = DiaryTheme.typography.micro.copy(color = DiaryTheme.colors.textSecondary),
        maxLines = 1
    )
}

// 행동 문구가 없는 광고는 버튼을 두지 않음
@Composable
private fun AdCallToAction(ad: NativeAd, adView: NativeAdView) {
    val action = ad.callToAction ?: return
    val typography = DiaryTheme.typography
    val colors = DiaryTheme.colors
    AdAsset(adView, register = { callToActionView = it }) {
        BasicText(
            action,
            modifier = Modifier
                .clip(DiaryShapes.r8)
                .background(colors.surface)
                .padding(horizontal = DiarySpacing.x3, vertical = DiarySpacing.x1_5),
            style = typography.captionStrong.copy(color = colors.textPrimary),
            maxLines = 1
        )
    }
}
