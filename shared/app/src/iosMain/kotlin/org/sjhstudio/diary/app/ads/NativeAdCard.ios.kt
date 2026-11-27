package org.sjhstudio.diary.app.ads

import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.LocalChosenFont
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_label
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.designsystem.theme.DiaryRadius
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.diaryLanguage
import org.sjhstudio.diary.core.designsystem.theme.usesSystemFont
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BUNDLED_FONT_FILES
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CALayer
import platform.UIKit.UIGraphicsPopContext
import platform.UIKit.UIGraphicsPushContext
import platform.UIKit.UIScreen
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
    val loaded = player.loaded[key] == true
    val allowed by player.consent.adsAllowed.collectAsStateWithLifecycle()
    player.consent.gate.AskWhileShown(key)
    // Swift가 치운 자리는 받음 표시가 지워져 다시 받음. 동의 창에 답해도 다시 받음
    LaunchedEffect(key, loaded, allowed) { if (!loaded) player.loadNative(key) }
    val style = rememberAdStyle()
    val view = if (loaded) remember(key, shape, style) { player.nativeView(key, shape == AdShape.Card, style) } else null
    if (view != null) frame { AdView(player, view) }
}

@Composable
private fun rememberAdStyle(): IosAdStyle {
    val colors = DiaryTheme.colors
    val label = stringResource(Res.string.ad_label)
    val fonts = rememberAdFonts()
    return remember(label, colors, fonts) {
        IosAdStyle(
            label,
            colors.textPrimary.toArgb().toLong(),
            colors.textSecondary.toArgb().toLong(),
            colors.sunken.toArgb().toLong(),
            colors.canvas.toArgb().toLong(),
            DiaryRadius.r16.value.toDouble(),
            colors.accentText.toArgb().toLong(),
            colors.selected.toArgb().toLong(),
            colors.surface.toArgb().toLong(),
            fonts?.regular,
            fonts?.medium,
            fonts?.semiBold
        )
    }
}

/** 굵기별 글꼴 파일. 굵기가 하나뿐인 글꼴은 세 자리가 같은 파일 */
private data class AdFonts(val regular: String, val medium: String, val semiBold: String)

private val PRETENDARD_FILES = AdFonts("pretendard_regular.otf", "pretendard_medium.otf", "pretendard_semibold.otf")

/**
 * 앱 글자와 같은 글꼴(DiaryApp의 고른 글꼴, diaryTypography)
 * 기본 글꼴의 일본어 화면과 시스템 글꼴은 null
 */
@Composable
private fun rememberAdFonts(): AdFonts? {
    val installer = koinInject<FontInstaller>()
    // 앱 글자와 같은 값을 읽어 카드가 옛 글꼴로 먼저 만들어지지 않게 함
    val font = LocalChosenFont.current
    val changes by installer.changes.collectAsStateWithLifecycle()
    // 화면 스레드에서 파일을 보지 않음
    val path by produceState(installer.knownPath(font.id), font, changes) {
        value = withContext(Dispatchers.IO) { installer.installedPath(font.id) }
    }
    val systemFont = usesSystemFont(diaryLanguage())
    return when (font) {
        DiaryFont.SYSTEM -> null
        DiaryFont.PRETENDARD -> PRETENDARD_FILES.takeUnless { systemFont }
        else -> (BUNDLED_FONT_FILES[font] ?: path)?.let { AdFonts(it, it, it) } ?: PRETENDARD_FILES
    }
}

/**
 * 폭에 맞춰 Swift가 잰 높이로 둠
 * UIKitView는 처음 만든 뷰만 들고 있어 글꼴·색이 바뀌어 새로 만든 뷰를 붙이려면 자리를 나눔
 * 화면이 밀리는 동안은 찍어 둔 모습을 그림. UIKit 뷰는 전환의 흐림 층에서 숨어 그 자리가 검게 뚫림
 */
@Composable
private fun AdView(player: IosAdPlayer, view: UIView) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val settled = transition.targetState == EnterExitState.Visible &&
        (transition.currentState == EnterExitState.Visible || !transition.isRunning)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toDouble()
        val height = remember(view, width) { player.fittingHeight(view, width) }
        val modifier = Modifier.fillMaxWidth().height(height.dp)
        val still = remember(view, width, settled) { if (settled) null else view.snapshot(width, height) }
        if (still == null) {
            key(view) { UIKitView(factory = { view }, modifier = modifier) }
        } else {
            Image(still, contentDescription = null, modifier = modifier, contentScale = ContentScale.FillBounds)
        }
    }
}

/**
 * 뷰를 [width]×[height] 크기로 찍은 그림. 화면에 붙기 전인 뷰도 크기를 맞춰 찍음
 * 전환 첫 프레임에 찍어서 이미지 압축 없이 화소 버퍼에 바로 그림
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIView.snapshot(width: Double, height: Double): ImageBitmap? {
    val scale = UIScreen.mainScreen.scale
    val pixelWidth = (width * scale).toInt()
    val pixelHeight = (height * scale).toInt()
    if (superview == null) {
        setFrame(CGRectMake(0.0, 0.0, width, height))
        layoutIfNeeded()
    }
    val pixels = if (pixelWidth > 0 && pixelHeight > 0) layer.renderPixels(pixelWidth, pixelHeight, scale) else null
    val info = ImageInfo(pixelWidth, pixelHeight, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
    return pixels?.let { SkiaImage.makeRaster(info, it, pixelWidth * BYTES_PER_PIXEL).toComposeImageBitmap() }
}

/** 층을 RGBA 화소로 그림. 그릴 문맥을 못 만들면 null */
@OptIn(ExperimentalForeignApi::class)
private fun CALayer.renderPixels(pixelWidth: Int, pixelHeight: Int, scale: Double): ByteArray? {
    val rowBytes = pixelWidth * BYTES_PER_PIXEL
    val pixels = ByteArray(rowBytes * pixelHeight)
    val space = CGColorSpaceCreateDeviceRGB()
    val drawn = pixels.usePinned { pinned ->
        CGBitmapContextCreate(
            pinned.addressOf(0),
            pixelWidth.toULong(),
            pixelHeight.toULong(),
            BITS_PER_COMPONENT,
            rowBytes.toULong(),
            space,
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        )?.let { context ->
            // UIKit 좌표는 위가 0이라 뒤집음
            CGContextTranslateCTM(context, 0.0, pixelHeight.toDouble())
            CGContextScaleCTM(context, scale, -scale)
            // 화면에 그려진 적 없는 뷰는 글자와 그림을 지금 문맥에 그림
            UIGraphicsPushContext(context)
            renderInContext(context)
            UIGraphicsPopContext()
            CGContextRelease(context)
            true
        } ?: false
    }
    CGColorSpaceRelease(space)
    return pixels.takeIf { drawn }
}

private const val BYTES_PER_PIXEL = 4
private const val BITS_PER_COMPONENT = 8uL
