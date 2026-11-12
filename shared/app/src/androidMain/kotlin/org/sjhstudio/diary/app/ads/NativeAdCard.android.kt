package org.sjhstudio.diary.app.ads

import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 광고 뷰는 둥근 면 안쪽 8에 둠. 면 안쪽 16 = 8 + 8
// SDK가 광고 뷰 구석에 넣는 AdChoices가 모서리 곡선에 잘리지 않음(AdMob 정책: 잘 보여야 함)
private const val PADDING_DP = 8
private const val GAP_DP = 12

// 얇은 줄의 광고 아이콘(1:1). 없는 광고는 글만
private const val ICON_DP = 40
private const val ICON_CORNER_DP = 8

// 영상 광고는 그림 자리가 가로세로 120dp 이상이어야 들어옴(AdMob 네이티브 정책)
private const val MEDIA_DP = 120

// 오른쪽 칸은 비워 둠(면 기준 28)
// 위에는 앱의 ×, 아래에는 AdChoices가 옴
private const val CLOSE_ROOM_DP = 20
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
            factory = { adView(it, shape, label) },
            // 색은 여기서 넣음. 보이는 중에 다크 모드를 바꾸면 뷰는 그대로라 factory에 두면 옛 색이 남음
            update = { bind(it, ad, palette) },
            modifier = Modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken).padding(DiarySpacing.x2)
        )
    }
}

private fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

private fun Context.text(size: Float, bold: Boolean = false): TextView = TextView(this).apply {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

private fun adView(context: Context, shape: AdShape, label: String): NativeAdView {
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
    } else {
        val icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = rounded(context.dp(ICON_CORNER_DP).toFloat())
        }
        val size = context.dp(ICON_DP)
        row.addView(icon, LinearLayout.LayoutParams(size, size).apply { marginEnd = context.dp(GAP_DP) })
        root.iconView = icon
    }
    val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    // AdMob이 제목 앞부분을 자르지 말라고 해 두 줄까지 둠
    val headline = context.text(TITLE_SP, bold = true).apply { maxLines = 2 }
    val action = context.text(SMALL_SP, bold = true)
    val mark = context.text(SMALL_SP).apply { text = label }
    column.addView(headline)
    column.addView(mark)
    column.addView(action)
    row.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    root.addView(row)
    root.headlineView = headline
    root.callToActionView = action
    root.tag = mark
    return root
}

private fun rounded(radius: Float) = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
}

private fun bind(view: NativeAdView, ad: NativeAd, colors: AdColors) {
    (view.headlineView as TextView).setTextColor(colors.title)
    (view.callToActionView as TextView).setTextColor(colors.title)
    (view.tag as TextView).setTextColor(colors.body)
    (view.iconView as? ImageView)?.apply {
        setImageDrawable(ad.icon?.drawable)
        visibility = if (ad.icon?.drawable == null) View.GONE else View.VISIBLE
    }
    (view.headlineView as TextView).text = ad.headline
    (view.callToActionView as TextView).text = ad.callToAction
    view.mediaView?.mediaContent = ad.mediaContent
    view.setNativeAd(ad)
}
