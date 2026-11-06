package org.sjhstudio.diary.feature.settings.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.settings.LibraryNotice
import org.sjhstudio.diary.feature.settings.LicenseTextScreen
import org.sjhstudio.diary.feature.settings.LicensesScreen

// 오픈소스 라이선스(I1)와 전문(I2) 프리뷰.
// 값은 Figma 화면과 같다.

private val sampleLibraries =
    listOf(
        LibraryNotice(
            "com.google.accompanist:accompanist-drawablepainter",
            "Accompanist Drawable Painter library",
            "Google",
            "Apache License 2.0"
        ),
        LibraryNotice("androidx.activity:activity", "Activity", "The Android Open Source Project", "Apache License 2.0"),
        LibraryNotice("androidx.biometric:biometric", "Biometric", "The Android Open Source Project", "Apache License 2.0"),
        LibraryNotice("io.coil-kt.coil3:coil", "coil", "Coil Contributors", "Apache License 2.0")
    )

private const val SAMPLE_OFL =
    "Copyright (c) YoonDesign Inc. All Rights Reserved.\n\n" +
        "This Font Software is licensed under the SIL Open Font License, Version 1.1."

@Composable
private fun List(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    LicensesScreen(sampleLibraries, onFont = {}, onLibrary = {}, onBack = {})
}

@Preview(name = "I1 오픈소스 라이선스 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ListLight() = List(dark = false)

@Preview(name = "I1 오픈소스 라이선스 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ListDark() = List(dark = true)

/** 라이브러리 목록을 읽는 동안은 글꼴만 보인다. */
@Preview(name = "I1 오픈소스 라이선스 · 읽는 중", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ListLoading() = DiaryTheme(darkTheme = false) {
    LicensesScreen(null, onFont = {}, onLibrary = {}, onBack = {})
}

@Composable
private fun FontText(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    LicenseTextScreen("감자꽃", "SIL Open Font License 1.1", SAMPLE_OFL, onBack = {})
}

@Preview(name = "I2 글꼴 전문 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FontTextLight() = FontText(dark = false)

@Preview(name = "I2 글꼴 전문 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FontTextDark() = FontText(dark = true)

@Preview(name = "I2 라이브러리 전문 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LibraryTextLight() = DiaryTheme(darkTheme = false) {
    LicenseTextScreen(
        "Activity",
        "Apache License 2.0",
        "Apache License\nVersion 2.0, January 2004",
        onBack = {},
        details = "androidx.activity:activity 1.12.0\nThe Android Open Source Project"
    )
}
