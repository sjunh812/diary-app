package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

private val LocalDiaryColors = staticCompositionLocalOf { LightDiaryColors }
private val LocalDiaryTypography = staticCompositionLocalOf<DiaryTypography> { error("DiaryTheme 안에서만 쓴다") }
private val LocalDiaryBrand = staticCompositionLocalOf { DiaryBrand.Lavender }

/**
 * 앱의 틀이 늘 같은 디자인 시스템을 따르게 함(헌법 V)
 * 화면 코드는 `DiaryTheme.colors`, `DiaryTheme.typography`와 간격·모서리 토큰만 씀
 * 테마 색([brand])은 브랜드색 자리 토큰만 바꿈(스펙 010)
 * MaterialTheme은 시트·대화상자 같은 Material 컴포넌트가 스스로 고르는 색과 글자를 우리 토큰에 맞추는 용도로만 깖
 */
@Composable
fun DiaryTheme(
    darkTheme: Boolean,
    fontFamily: FontFamily? = null,
    brand: DiaryBrand = DiaryBrand.Lavender,
    content: @Composable () -> Unit
) {
    val colors = remember(darkTheme, brand) { diaryColors(darkTheme, brand) }
    val typography = diaryTypography(fontFamily)
    // Material 값은 equals가 없어 매번 만들면 설정이 바뀔 때마다 앱 전체가 다시 그려짐
    val colorScheme = remember(colors, darkTheme) { colors.toMaterialColorScheme(darkTheme) }
    val materialTypography = remember(typography) { typography.toMaterialTypography() }
    CompositionLocalProvider(
        LocalDiaryColors provides colors,
        LocalDiaryTypography provides typography,
        LocalDiaryBrand provides brand
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = materialTypography, content = content)
    }
}

/**
 * 사용자가 고른 글꼴은 [fontFamily]로 받아 앱 전체 글자에 씀. null이면 기본 글꼴
 * 글자 크기·굵기·줄 높이는 글꼴과 상관없이 같은 토큰
 */
object DiaryTheme {
    val colors: DiaryColors
        @Composable @ReadOnlyComposable
        get() = LocalDiaryColors.current

    val typography: DiaryTypography
        @Composable @ReadOnlyComposable
        get() = LocalDiaryTypography.current

    /** 지금 앱이 쓰는 테마 색. 직접 고른 색은 이미 계산된 값 */
    val brand: DiaryBrand
        @Composable @ReadOnlyComposable
        get() = LocalDiaryBrand.current
}

private fun DiaryColors.toMaterialColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = selected,
        onPrimaryContainer = textPrimary,
        secondaryContainer = selected,
        onSecondaryContainer = textPrimary,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = sunken,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = sunken,
        outline = lineStrong,
        outlineVariant = line,
        error = danger,
        onError = onDanger,
        scrim = scrim
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = selected,
        onPrimaryContainer = textPrimary,
        secondaryContainer = selected,
        onSecondaryContainer = textPrimary,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = sunken,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = sunken,
        outline = lineStrong,
        outlineVariant = line,
        error = danger,
        onError = onDanger,
        scrim = scrim
    )
}

private fun DiaryTypography.toMaterialTypography() = Typography(
    displaySmall = display,
    headlineSmall = title1,
    titleLarge = title2,
    titleMedium = headline,
    bodyLarge = reading,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = captionStrong,
    labelSmall = micro
)
