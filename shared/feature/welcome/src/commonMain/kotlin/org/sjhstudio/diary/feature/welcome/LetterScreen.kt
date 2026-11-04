package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryNote
import org.sjhstudio.diary.core.designsystem.component.DiaryNoteBox
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_close
import org.sjhstudio.diary.feature.welcome.resources.letter_full
import org.sjhstudio.diary.feature.welcome.resources.letter_hello
import org.sjhstudio.diary.feature.welcome.resources.letter_moved
import org.sjhstudio.diary.feature.welcome.resources.letter_promise_ads
import org.sjhstudio.diary.feature.welcome.resources.letter_promise_free
import org.sjhstudio.diary.feature.welcome.resources.letter_read_all
import org.sjhstudio.diary.feature.welcome.resources.letter_short
import org.sjhstudio.diary.feature.welcome.resources.letter_sign
import org.sjhstudio.diary.feature.welcome.resources.letter_start
import org.sjhstudio.diary.feature.welcome.resources.letter_title

/** 개발자의 편지(Figma W1). 판매 장치 없이 만든 사람의 말만 둔다(FR-003, FR-005). 약속은 글에 묻히지 않게 상자로 뺀다. */
@Composable
fun LetterScreen(entries: Int?, onStart: () -> Unit, onReadAll: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val thanks = entries?.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.letter_moved, it, it.grouped()) }
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        LetterBody(Modifier.weight(1f)) {
            if (thanks != null) LetterParagraphs(thanks)
            LetterParagraphs(stringResource(Res.string.letter_short))
            DiaryNoteBox(
                listOf(
                    DiaryNote(DiaryIcons.CircleCheck, stringResource(Res.string.letter_promise_free), null),
                    DiaryNote(DiaryIcons.CircleCheck, stringResource(Res.string.letter_promise_ads), null)
                )
            )
            LetterParagraphs(stringResource(Res.string.letter_close))
        }
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
        LetterBody(Modifier.weight(1f).navigationBarsPadding()) { LetterParagraphs(stringResource(Res.string.letter_full)) }
    }
}

/** 제목 한 줄, 본문, 서명 순서다. 문단 사이는 빈 줄이 아니라 간격 16이다(줄 높이 28의 1.5배 이상이 되는 값, WCAG 1.4.8). */
@Composable
private fun LetterBody(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(
        modifier
            .widthIn(max = DiaryWriteSize.bodyMax)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiarySpacing.x6, vertical = DiarySpacing.x8),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
    ) {
        BasicText(stringResource(Res.string.letter_hello), style = typography.title1.copy(color = colors.textPrimary))
        Spacer(Modifier.height(DiarySpacing.x1))
        content()
        Spacer(Modifier.height(DiarySpacing.x1))
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
            MoodFace(Mood.GOOD, DiarySpacing.x6)
            BasicText(stringResource(Res.string.letter_sign), style = typography.label.copy(color = colors.textSecondary))
        }
    }
}

/** 문자열의 빈 줄을 문단으로 나눠 그린다. 간격은 부모의 16을 따른다. */
@Composable
private fun LetterParagraphs(text: String) {
    val colors = DiaryTheme.colors
    val style = DiaryTheme.typography.reading.copy(color = colors.textPrimary)
    val weight = DiaryTheme.typography.bodyStrong.fontWeight ?: FontWeight.SemiBold
    text.split("\n\n").forEach { BasicText(emphasized(it, weight), style = style) }
}

/** `**`로 감싼 곳만 굵게 한다. 번역할 때 강조 자리를 문장에 맞게 옮길 수 있도록 문자열에 둔다. */
private fun emphasized(text: String, weight: FontWeight): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = weight)) { append(part) } else append(part)
    }
}
