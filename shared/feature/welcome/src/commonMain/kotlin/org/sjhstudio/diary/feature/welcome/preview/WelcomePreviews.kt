package org.sjhstudio.diary.feature.welcome.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.welcome.FullLetterScreen
import org.sjhstudio.diary.feature.welcome.LetterScreen
import org.sjhstudio.diary.feature.welcome.OnboardingScreen

@Preview(locale = "ko")
@Composable
private fun LetterLight() = DiaryTheme(darkTheme = false) { LetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun LetterDark() = DiaryTheme(darkTheme = true) { LetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun LetterNoEntriesLight() = DiaryTheme(darkTheme = false) { FullLetterScreen(0, {}) }

@Preview(locale = "ko")
@Composable
private fun FullLetterLight() = DiaryTheme(darkTheme = false) { FullLetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun FullLetterDark() = DiaryTheme(darkTheme = true) { FullLetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun UpdatedLight() = DiaryTheme(darkTheme = false) { OnboardingScreen(updated = true, onFinish = {}) }

@Preview(locale = "ko")
@Composable
private fun UpdatedDark() = DiaryTheme(darkTheme = true) { OnboardingScreen(updated = true, onFinish = {}) }

@Preview(locale = "ko")
@Composable
private fun NewLight() = DiaryTheme(darkTheme = false) { OnboardingScreen(updated = false, onFinish = {}) }

@Preview(locale = "ko")
@Composable
private fun NewDark() = DiaryTheme(darkTheme = true) { OnboardingScreen(updated = false, onFinish = {}) }

@Preview(locale = "ko", widthDp = 1024, heightDp = 720)
@Composable
private fun NewTablet() = DiaryTheme(darkTheme = false) { OnboardingScreen(updated = false, onFinish = {}) }
