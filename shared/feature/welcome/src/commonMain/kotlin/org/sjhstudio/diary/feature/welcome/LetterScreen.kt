package org.sjhstudio.diary.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_close
import org.sjhstudio.diary.feature.welcome.resources.letter_hello
import org.sjhstudio.diary.feature.welcome.resources.letter_moved
import org.sjhstudio.diary.feature.welcome.resources.letter_promise_ads
import org.sjhstudio.diary.feature.welcome.resources.letter_promise_free
import org.sjhstudio.diary.feature.welcome.resources.letter_short
import org.sjhstudio.diary.feature.welcome.resources.letter_sign
import org.sjhstudio.diary.feature.welcome.resources.letter_start
import org.sjhstudio.diary.feature.welcome.resources.letter_title
import org.sjhstudio.diary.feature.welcome.resources.letter_to

private const val RISE_MILLIS = 450
private const val BACKDROP_ALPHA = 0.4f

/** 편지 바탕. 편지지가 도드라질 만큼만 아주 옅은 라벤더를 깐다. */
@Composable
private fun Modifier.letterBackdrop(): Modifier {
    val colors = DiaryTheme.colors
    return background(colors.canvas).background(colors.selected.copy(alpha = BACKDROP_ALPHA))
}

/** 개발자의 편지(Figma W1). 판매 장치 없이 만든 사람의 말만 둔다(FR-003, FR-005). 글을 나열하지 않고 편지지 한 장으로 묶는다. */
@Composable
fun LetterScreen(entries: Int?, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().letterBackdrop().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        LetterSheet(Modifier.weight(1f)) { LetterBody(entries) }
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DiaryButton(stringResource(Res.string.letter_start), onStart, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
        }
    }
}

/** 새 소식에서 다시 읽는 편지. 처음 본 편지와 같은 글이고 계속하기 대신 뒤로 가기가 있다. */
@Composable
fun FullLetterScreen(entries: Int?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().letterBackdrop().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.letter_title), onBack = onBack)
        LetterSheet(Modifier.weight(1f).navigationBarsPadding()) { LetterBody(entries) }
    }
}

/** 처음 본 편지와 새 소식의 편지가 함께 쓰는 본문. 옮긴 일기가 없으면 고맙다는 문단은 없다. */
@Composable
private fun LetterBody(entries: Int?) {
    entries?.takeIf { it > 0 }?.let { LetterParagraphs(pluralStringResource(Res.plurals.letter_moved, it, it.grouped())) }
    LetterParagraphs(stringResource(Res.string.letter_short))
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        Promise(stringResource(Res.string.letter_promise_free))
        Promise(stringResource(Res.string.letter_promise_ads))
    }
    LetterParagraphs(stringResource(Res.string.letter_close))
}

/**
 * 편지지 한 장. 받는 사람, 인사, 본문, 보내는 사람 순서로 편지 꼴을 갖춘다.
 * 문단 사이는 빈 줄이 아니라 간격 16이다(줄 높이 28의 1.5배 이상이 되는 값, WCAG 1.4.8).
 * 열릴 때 한 번 살짝 올라오고, 동작 줄이기면 그대로 있다.
 */
@Composable
private fun LetterSheet(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val rise = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) { if (!still) rise.animateTo(1f, tween(RISE_MILLIS)) }
    Column(
        modifier
            .widthIn(max = DiaryWriteSize.bodyMax)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x4)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = rise.value
                    translationY = (1f - rise.value) * DiarySpacing.x6.toPx()
                }.clip(DiaryShapes.r20)
                .background(colors.surface)
                .border(DiarySpacing.hairline, colors.line, DiaryShapes.r20)
                .padding(horizontal = DiarySpacing.x6, vertical = DiarySpacing.x6),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
        ) {
            BasicText(stringResource(Res.string.letter_to), style = typography.label.copy(color = colors.accentText))
            BasicText(stringResource(Res.string.letter_hello), style = typography.title1.copy(color = colors.textPrimary))
            content()
            Spacer(Modifier.height(DiarySpacing.x1))
            BasicText(
                stringResource(Res.string.letter_sign),
                Modifier.fillMaxWidth(),
                style = typography.label.copy(color = colors.textSecondary, textAlign = TextAlign.End)
            )
        }
    }
}

/** 약속 한 줄. 글에 묻히지 않게 표시를 앞에 둔다. */
@Composable
private fun Promise(text: String) {
    val colors = DiaryTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
        DiaryIcon(DiaryIcons.CircleCheck, contentDescription = null, tint = colors.accent, size = DiaryIconSize.i20)
        BasicText(text, style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary))
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
