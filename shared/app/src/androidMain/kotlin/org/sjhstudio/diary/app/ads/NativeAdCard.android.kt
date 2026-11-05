package org.sjhstudio.diary.app.ads

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
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
private const val TITLE_SP = 15f
private const val BODY_SP = 13f

/** 일기 줄과 구별되게 `sunken` 면과 "광고" 표시를 둔다(AdMob 네이티브 정책). */
@Composable
internal actual fun NativeAdCard(modifier: Modifier) {
    val player = koinInject<AdPlayer>() as? AdMobPlayer ?: return
    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(player) {
        player.start()
        val loader =
            AdLoader.Builder(context, player.units.native)
                .forNativeAd { ad = it }
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            ad = null
                        }
                    }
                ).build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { ad?.destroy() }
    }
    val shown = ad ?: return
    val colors = DiaryTheme.colors
    val label = stringResource(Res.string.ad_label)
    val title = colors.textPrimary.toArgb()
    val body = colors.textSecondary.toArgb()
    AndroidView(
        factory = { adView(it, label, title, body) },
        update = { view -> bind(view, shown) },
        modifier = modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken)
    )
}

private fun adView(context: Context, label: String, titleColor: Int, bodyColor: Int): NativeAdView {
    val pad = (PADDING_DP * context.resources.displayMetrics.density).toInt()
    val root = NativeAdView(context)
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, pad, pad, pad)
    }
    column.addView(
        TextView(context).apply {
            text = label
            textSize = BODY_SP
            setTextColor(bodyColor)
        }
    )
    val headline = TextView(context).apply {
        textSize = TITLE_SP
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(titleColor)
    }
    val body = TextView(context).apply {
        textSize = BODY_SP
        setTextColor(bodyColor)
    }
    val action = TextView(context).apply {
        textSize = BODY_SP
        gravity = Gravity.START
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(titleColor)
    }
    column.addView(headline)
    column.addView(body)
    column.addView(action)
    root.addView(column)
    root.headlineView = headline
    root.bodyView = body
    root.callToActionView = action
    return root
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.bodyView as TextView).text = ad.body
    (view.callToActionView as TextView).text = ad.callToAction
    view.setNativeAd(ad)
}
