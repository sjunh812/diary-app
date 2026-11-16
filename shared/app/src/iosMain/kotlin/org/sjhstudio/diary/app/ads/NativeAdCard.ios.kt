package org.sjhstudio.diary.app.ads

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.StartupState
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.ad_label
import org.sjhstudio.diary.core.data.AdPlayer
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.designsystem.theme.DiaryRadius
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.diaryLanguage
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BUNDLED_FONT_FILES
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
    // 치워진 자리는 받음 표시가 지워져 다시 받음
    LaunchedEffect(key, loaded) { if (!loaded) player.loadNative(key) }
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
    val repository = koinInject<SettingsRepository>()
    val installer = koinInject<FontInstaller>()
    val startup = koinInject<Startup>()
    val fontFlow = remember(repository) { repository.settings.map { it.font }.distinctUntilChanged() }
    // 시작 단계에서 읽은 글꼴로 첫 프레임을 맞춤. 기본 글꼴로 먼저 그리면 카드가 다시 만들어지며 깜빡임
    val initial = (startup.state.value as? StartupState.Ready)?.settings?.font ?: DiaryFont.DEFAULT
    val font by fontFlow.collectAsStateWithLifecycle(initial)
    val changes by installer.changes.collectAsStateWithLifecycle()
    // 화면 스레드에서 파일을 보지 않음
    val path by produceState(installer.knownPath(font.id), font, changes) {
        value = withContext(Dispatchers.IO) { installer.installedPath(font.id) }
    }
    val japanese = diaryLanguage() == "ja"
    return when (font) {
        DiaryFont.SYSTEM -> null
        DiaryFont.PRETENDARD -> PRETENDARD_FILES.takeUnless { japanese }
        else -> (BUNDLED_FONT_FILES[font] ?: path)?.let { AdFonts(it, it, it) } ?: PRETENDARD_FILES
    }
}

/**
 * 폭에 맞춰 Swift가 잰 높이로 둠
 * UIKitView는 처음 만든 뷰만 들고 있어 글꼴·색이 바뀌어 새로 만든 뷰를 붙이려면 자리를 나눔
 */
@Composable
private fun AdView(player: IosAdPlayer, view: UIView) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toDouble()
        val height = remember(view, width) { player.fittingHeight(view, width) }
        key(view) { UIKitView(factory = { view }, modifier = Modifier.fillMaxWidth().height(height.dp)) }
    }
}
