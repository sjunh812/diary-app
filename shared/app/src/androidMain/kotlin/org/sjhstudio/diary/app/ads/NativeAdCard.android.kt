package org.sjhstudio.diary.app.ads

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_label
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

private const val PADDING_DP = 16
private const val GAP_DP = 12

// 영상 광고는 그림 자리가 가로세로 120dp 이상이어야 들어옴(AdMob 네이티브 정책)
private const val MEDIA_DP = 120

// 오른쪽 칸은 비워 둠
// 위에는 앱의 ×, 아래에는 AdChoices가 옴
private const val CLOSE_ROOM_DP = 28
private const val TITLE_SP = 15f
private const val SMALL_SP = 13f

private class AdColors(val title: Int, val body: Int)

/**
 * 일기 줄과 구별되게 `sunken` 면과 "광고" 표시를 둠(AdMob 네이티브 정책)
 * 받기 전이나 못 받으면 그리지 않음
 */
@Composable
internal actual fun NativeAdCard(key: String, shape: AdShape, frame: @Composable (ad: @Composable () -> Unit) -> Unit) {
    val player = koinInject<AdPlayer>() as? AdMobPlayer ?: return
    LaunchedEffect(key) { player.loadNative(key) }
    val ad = player.natives[key] ?: return
    val colors = DiaryTheme.colors
    val label = stringResource(Res.string.ad_label)
    val palette = AdColors(colors.textPrimary.toArgb(), colors.textSecondary.toArgb())
    frame {
        AndroidView(
            factory = { adView(it, shape, label, palette) },
            update = { bind(it, ad) },
            modifier = Modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken)
        )
    }
}

private fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

private fun Context.text(size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

private fun adView(context: Context, shape: AdShape, label: String, colors: AdColors): NativeAdView {
    val pad = context.dp(PADDING_DP)
    val root = NativeAdView(context)
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(pad, pad, context.dp(CLOSE_ROOM_DP), pad)
    }
    if (shape == AdShape.Card) {
        val media = MediaView(context).apply { setImageScaleType(ImageView.ScaleType.FIT_CENTER) }
        val size = context.dp(MEDIA_DP)
        row.addView(media, LinearLayout.LayoutParams(size, size).apply { marginEnd = context.dp(GAP_DP) })
        root.mediaView = media
    }
    val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    // AdMob이 제목 앞부분을 자르지 말라고 해 두 줄까지 둠
    val headline = context.text(TITLE_SP, colors.title, bold = true).apply { maxLines = 2 }
    val action = context.text(SMALL_SP, colors.title, bold = true)
    column.addView(headline)
    column.addView(context.text(SMALL_SP, colors.body).apply { text = label })
    column.addView(action)
    row.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    root.addView(row)
    root.headlineView = headline
    root.callToActionView = action
    return root
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.callToActionView as TextView).text = ad.callToAction
    view.mediaView?.mediaContent = ad.mediaContent
    view.setNativeAd(ad)
}
