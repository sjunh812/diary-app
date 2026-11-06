package org.sjhstudio.diary.feature.welcome.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.welcome.FullLetterScreen
import org.sjhstudio.diary.feature.welcome.LetterScreen
import org.sjhstudio.diary.feature.welcome.OnboardingScreen
import org.sjhstudio.diary.feature.welcome.WhatsNewScreen

@Preview(locale = "ko")
@Composable
private fun LetterLight() = DiaryTheme(darkTheme = false) { LetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun LetterDark() = DiaryTheme(darkTheme = true) { LetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun LetterNoEntriesLight() = DiaryTheme(darkTheme = false) { LetterScreen(0, {}) }

@Preview(locale = "ko")
@Composable
private fun FullLetterLight() = DiaryTheme(darkTheme = false) { FullLetterScreen(1204, {}) }

@Preview(locale = "ko")
@Composable
private fun UpdatedLight() = DiaryTheme(darkTheme = false) { OnboardingScreen(true, false, false, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun NewDark() = DiaryTheme(darkTheme = true) { OnboardingScreen(false, false, false, {}, {}) }

@Preview(locale = "ko")
@Composable
private fun NewsLight() = DiaryTheme(darkTheme = false) { WhatsNewScreen({}, {}, {}) }
