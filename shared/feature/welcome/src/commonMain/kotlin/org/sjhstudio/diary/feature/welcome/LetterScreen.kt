package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_full
import org.sjhstudio.diary.feature.welcome.resources.letter_hello
import org.sjhstudio.diary.feature.welcome.resources.letter_moved
import org.sjhstudio.diary.feature.welcome.resources.letter_read_all
import org.sjhstudio.diary.feature.welcome.resources.letter_short
import org.sjhstudio.diary.feature.welcome.resources.letter_sign
import org.sjhstudio.diary.feature.welcome.resources.letter_start
import org.sjhstudio.diary.feature.welcome.resources.letter_title

/** 개발자의 편지(Figma W1). 판매 장치 없이 만든 사람의 말만 둔다(FR-003, FR-005). */
@Composable
fun LetterScreen(entries: Int?, onStart: () -> Unit, onReadAll: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val moved = entries?.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.letter_moved, it, it.grouped()) }
    val paragraphs = listOfNotNull(stringResource(Res.string.letter_hello), moved, stringResource(Res.string.letter_short))
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        LetterBody(paragraphs, Modifier.weight(1f))
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
        ) {
            DiaryButton(stringResource(Res.string.letter_start), onStart, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
            DiaryButton(stringResource(Res.string.letter_read_all), onReadAll, kind = DiaryButtonKind.Text)
        }
    }
}

/** 편지 전체. 온보딩 앞에서도, 새 소식에서도 같은 글이다. */
@Composable
fun FullLetterScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.letter_title), onBack = onBack)
        LetterBody(listOf(stringResource(Res.string.letter_full)), Modifier.weight(1f).navigationBarsPadding())
    }
}

@Composable
private fun LetterBody(paragraphs: List<String>, modifier: Modifier) {
    val colors = DiaryTheme.colors
    val style = DiaryTheme.typography.reading.copy(color = colors.textPrimary)
    Column(
        modifier
            .widthIn(max = DiaryWriteSize.bodyMax)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiarySpacing.x6, vertical = DiarySpacing.x6),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)
    ) {
        paragraphs.forEach { BasicText(it, style = style) }
        BasicText(stringResource(Res.string.letter_sign), style = style.copy(color = colors.textSecondary))
    }
}
