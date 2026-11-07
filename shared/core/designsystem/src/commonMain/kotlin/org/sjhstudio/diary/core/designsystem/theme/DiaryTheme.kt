package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

private val LocalDiaryColors = staticCompositionLocalOf { LightDiaryColors }
private val LocalDiaryTypography = staticCompositionLocalOf<DiaryTypography> { error("DiaryTheme 안에서만 쓴다") }

/**
 * 앱의 틀이 늘 같은 디자인 시스템을 따르게 한다(헌법 V).
 * 화면 코드는 `DiaryTheme.colors`, `DiaryTheme.typography`와 간격·모서리 토큰만 쓴다.
 * MaterialTheme은 시트·대화상자 같은 Material 컴포넌트가 스스로 고르는 색과 글자를 우리 토큰에 맞추려고만 깐다.
 */
@Composable
fun DiaryTheme(darkTheme: Boolean, fontFamily: FontFamily? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkDiaryColors else LightDiaryColors
    val typography = diaryTypography(fontFamily)
    CompositionLocalProvider(LocalDiaryColors provides colors, LocalDiaryTypography provides typography) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = typography.toMaterialTypography(),
            content = content
        )
    }
}

/**
 * 사용자가 고른 글꼴은 [fontFamily]로 받아 앱 전체 글자에 쓴다.
 * null이면 기본 글꼴이다.
 * 글자 크기·굵기·줄 높이는 글꼴과 상관없이 같은 토큰이다.
 */
object DiaryTheme {
    val colors: DiaryColors
        @Composable @ReadOnlyComposable
        get() = LocalDiaryColors.current

    val typography: DiaryTypography
        @Composable @ReadOnlyComposable
        get() = LocalDiaryTypography.current
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
