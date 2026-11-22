package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryColorPicker
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryThemeModeOption
import org.sjhstudio.diary.core.designsystem.component.DiaryThemeSwatch
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.MiniHomeLayout
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.theme.DiaryBrand
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.designsystem.theme.diaryColors
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.ui.presetBrand
import org.sjhstudio.diary.core.ui.shownColor
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.list_plus_action
import org.sjhstudio.diary.feature.settings.resources.list_plus_later
import org.sjhstudio.diary.feature.settings.resources.plus_badge
import org.sjhstudio.diary.feature.settings.resources.settings_theme
import org.sjhstudio.diary.feature.settings.resources.theme_custom
import org.sjhstudio.diary.feature.settings.resources.theme_custom_apply
import org.sjhstudio.diary.feature.settings.resources.theme_custom_cancel
import org.sjhstudio.diary.feature.settings.resources.theme_custom_hex
import org.sjhstudio.diary.feature.settings.resources.theme_custom_hue
import org.sjhstudio.diary.feature.settings.resources.theme_custom_pad
import org.sjhstudio.diary.feature.settings.resources.theme_dark
import org.sjhstudio.diary.feature.settings.resources.theme_follow_system
import org.sjhstudio.diary.feature.settings.resources.theme_group_color
import org.sjhstudio.diary.feature.settings.resources.theme_group_mode
import org.sjhstudio.diary.feature.settings.resources.theme_light
import org.sjhstudio.diary.feature.settings.resources.theme_note
import org.sjhstudio.diary.feature.settings.resources.theme_plus_body
import org.sjhstudio.diary.feature.settings.resources.theme_plus_title
import org.sjhstudio.diary.feature.settings.resources.theme_swatch_plus

/**
 * 테마(Figma "프로토타입 · 테마 색", 스펙 010)
 * 위는 화면 모드(작은 홈 그림 두 장)와 시스템 설정 스위치, 아래는 테마 색 8칸
 * 그림이 고른 테마 색으로 그려져 미리보기를 겸함. 견본과 그림은 누르면 바로 바뀜
 * [canCustom]이 null이면 요금제를 아직 모름. 산 사람에게 Plus 안내가 스치지 않게 직접 고르기 칸을 누를 수 없게 둠
 */
@Composable
fun ThemeSettingsScreen(
    mode: ThemeMode,
    theme: ThemeChoice,
    listStyle: ListStyle,
    canCustom: Boolean?,
    onMode: (ThemeMode) -> Unit,
    onColor: (ThemeColor) -> Unit,
    onCustom: (Int) -> Unit,
    onPlus: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    var askPlus by rememberSaveable { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val dark =
        when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    // 앱이 이미 계산해 쓰는 테마 색. 직접 고른 색을 화면마다 다시 계산하지 않음
    val brand = DiaryTheme.brand
    Column(
        modifier.fillMaxSize().background(DiaryTheme.colors.canvas).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DiaryTopBar(title = stringResource(Res.string.settings_theme), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            DiarySettingsHeader(stringResource(Res.string.theme_group_mode))
            ModePictures(mode, brand, listStyle.miniHome(), onMode)
            Spacer(Modifier.height(DiarySpacing.x6))
            DiarySettingsSwitchRow(
                stringResource(Res.string.theme_follow_system),
                checked = mode == ThemeMode.SYSTEM,
                // 끄면 지금 보이는 모드를 그대로 고름
                onCheckedChange = { on ->
                    onMode(
                        if (on) {
                            ThemeMode.SYSTEM
                        } else if (systemDark) {
                            ThemeMode.DARK
                        } else {
                            ThemeMode.LIGHT
                        }
                    )
                }
            )
            PhraseText(
                stringResource(Res.string.theme_note),
                style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary),
                modifier = Modifier.padding(horizontal = LocalScreenGutter.current)
            )
            DiarySettingsHeader(stringResource(Res.string.theme_group_color), gapAbove = true)
            Swatches(
                theme,
                dark,
                canCustom,
                onColor = onColor,
                onCustom = { if (canCustom == true) picking = true else askPlus = true },
                modifier = Modifier.padding(horizontal = LocalScreenGutter.current).padding(bottom = DiarySpacing.x6)
            )
        }
    }
    if (picking) {
        CustomColorSheet(
            initial = theme.customRgb ?: (brand.tokens(dark = false).accent.toArgb() and RGB),
            onApply = {
                picking = false
                onCustom(it)
            },
            onDismiss = { picking = false }
        )
    }
    if (askPlus) ThemePlusSheet(onPlus = onPlus, onDismiss = { askPlus = false })
}

/** 라이트·다크 그림. 시스템 설정을 따르면 둘 다 고르지 않은 모양 */
@Composable
private fun ModePictures(mode: ThemeMode, brand: DiaryBrand, layout: MiniHomeLayout, onMode: (ThemeMode) -> Unit) {
    val light = remember(brand) { diaryColors(dark = false, brand = brand) }
    val dark = remember(brand) { diaryColors(dark = true, brand = brand) }
    Row(
        Modifier.fillMaxWidth().padding(top = DiarySpacing.x2),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x14, Alignment.CenterHorizontally)
    ) {
        DiaryThemeModeOption(
            stringResource(Res.string.theme_light),
            light,
            layout,
            selected = mode == ThemeMode.LIGHT,
            onClick = { onMode(ThemeMode.LIGHT) }
        )
        DiaryThemeModeOption(
            stringResource(Res.string.theme_dark),
            dark,
            layout,
            selected = mode == ThemeMode.DARK,
            onClick = { onMode(ThemeMode.DARK) }
        )
    }
}

/** 정한 7색과 직접 고르기, 4칸씩 두 줄 */
@Composable
private fun Swatches(
    theme: ThemeChoice,
    dark: Boolean,
    canCustom: Boolean?,
    onColor: (ThemeColor) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shown = theme.shownColor(customAllowed = canCustom == true)
    val customRgb = theme.customRgb
    // 직접 고른 색을 쓰는 중이면 앱 값을 그대로, 다른 색을 쓰는 중일 때만 계산
    val appBrand = DiaryTheme.brand
    val using = shown == ThemeColor.CUSTOM
    val computed = remember(customRgb, using) { if (using) null else customRgb?.let { DiaryBrand.custom(it) } }
    val custom = if (using) appBrand else computed
    val plus = stringResource(Res.string.plus_badge)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        (ThemeColor.presets + ThemeColor.CUSTOM).chunked(COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { color ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        if (color == ThemeColor.CUSTOM) {
                            val name = stringResource(Res.string.theme_custom)
                            val tokens = custom?.tokens(dark)
                            val locked = canCustom == false
                            DiaryThemeSwatch(
                                name = name,
                                label = if (locked) stringResource(Res.string.theme_swatch_plus, name) else name,
                                color = tokens?.accent,
                                checkColor = tokens?.onAccent ?: DiaryTheme.colors.onAccent,
                                selected = shown == ThemeColor.CUSTOM,
                                onClick = onCustom,
                                plusLabel = plus.takeIf { locked },
                                enabled = canCustom != null
                            )
                        } else {
                            val tokens = color.presetBrand().tokens(dark)
                            val name = stringResource(color.label())
                            DiaryThemeSwatch(
                                name = name,
                                label = name,
                                color = tokens.accent,
                                checkColor = tokens.onAccent,
                                selected = shown == color,
                                onClick = { onColor(color) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 직접 고르기. [이 색으로 하기]를 누를 때만 바뀜 */
@Composable
private fun CustomColorSheet(initial: Int, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableIntStateOf(initial) }
    // 색 코드를 덜 넣는 동안은 적용할 수 없음
    var complete by rememberSaveable { mutableStateOf(true) }
    DiaryBottomSheet(onDismiss, title = stringResource(Res.string.theme_custom)) {
        DiaryColorPicker(
            initial = initial,
            onChange = { rgb ->
                complete = rgb != null
                if (rgb != null) picked = rgb
            },
            padLabel = stringResource(Res.string.theme_custom_pad),
            hueLabel = stringResource(Res.string.theme_custom_hue),
            hexLabel = stringResource(Res.string.theme_custom_hex)
        )
        DiaryButtonPair(
            secondary = stringResource(Res.string.theme_custom_cancel),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.theme_custom_apply),
            onPrimary = { onApply(picked) },
            size = DiaryButtonSize.XLarge,
            primaryEnabled = complete
        )
    }
}

/** 무료 사용자가 직접 고르기를 누르면. 체크리스트 Plus 시트와 같은 꼴 */
@Composable
private fun ThemePlusSheet(onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss,
        title = stringResource(Res.string.theme_plus_title),
        description = stringResource(Res.string.theme_plus_body)
    ) {
        DiaryButtonPair(
            secondary = stringResource(Res.string.list_plus_later),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.list_plus_action),
            onPrimary = {
                onDismiss()
                onPlus()
            },
            size = DiaryButtonSize.XLarge
        )
    }
}

private fun ListStyle.miniHome(): MiniHomeLayout = when (this) {
    ListStyle.FEED -> MiniHomeLayout.Feed
    ListStyle.CARD -> MiniHomeLayout.Card
    ListStyle.PHOTO_GRID -> MiniHomeLayout.Grid
}

private const val COLUMNS = 4
private const val RGB = 0xFFFFFF
