package org.sjhstudio.diary.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.FirstLineIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.CenteredLineBreak
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_close
import org.sjhstudio.diary.feature.welcome.resources.letter_feedback
import org.sjhstudio.diary.feature.welcome.resources.letter_feedback_invite
import org.sjhstudio.diary.feature.welcome.resources.letter_hello
import org.sjhstudio.diary.feature.welcome.resources.letter_moved
import org.sjhstudio.diary.feature.welcome.resources.letter_open
import org.sjhstudio.diary.feature.welcome.resources.letter_promise_ads
import org.sjhstudio.diary.feature.welcome.resources.letter_promise_free
import org.sjhstudio.diary.feature.welcome.resources.letter_short
import org.sjhstudio.diary.feature.welcome.resources.letter_sign
import org.sjhstudio.diary.feature.welcome.resources.letter_start
import org.sjhstudio.diary.feature.welcome.resources.letter_title
import org.sjhstudio.diary.feature.welcome.resources.letter_to

private const val RISE_MILLIS = 450
private const val OPEN_MILLIS = 3_200
private const val OPEN_DELAY_MILLIS = 180L
private const val BACKDROP_ALPHA = 0.4f

/**
 * 편지 바탕
 * 편지지가 도드라질 만큼만 아주 옅은 라벤더
 */
@Composable
private fun Modifier.letterBackdrop(): Modifier {
    val colors = DiaryTheme.colors
    return background(colors.canvas).background(colors.selected.copy(alpha = BACKDROP_ALPHA))
}

/**
 * 개발자의 편지(Figma W0, W1)
 * 판매 장치 없이 만든 사람의 말만 둠(FR-003, FR-005)
 * 봉투를 열면 편지지가 나옴
 * 버튼 자리를 그대로 두어 열고 나서 같은 곳을 누르면 넘어감
 * 동작 줄이기에서는 바로 편지지를 보임
 */
@Composable
fun LetterScreen(entries: Int?, onStart: () -> Unit, modifier: Modifier = Modifier) {
    var opened by rememberSaveable { mutableStateOf(false) }
    val still = rememberReduceMotion()
    val opening = remember { Animatable(0f) }
    var waiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 누르면 버튼이 먼저 물러나고, 편지지가 다 펼쳐지면 [계속하기]로 돌아옴
    val buttonShown by animateFloatAsState(if (waiting && !opened) 0f else 1f, tween(DiaryMotion.QUICK_MILLIS))
    val open: () -> Unit = {
        when {
            still -> opened = true

            !opening.isRunning && !waiting -> scope.launch {
                // 누른 표시가 끝난 뒤에 움직임
                // 탭 직후의 바쁜 프레임에 시작하면 덮개와 글자가 끊겨 보임
                waiting = true
                delay(OPEN_DELAY_MILLIS)
                opening.animateTo(1f, tween(OPEN_MILLIS))
                opened = true
            }
        }
    }
    Column(modifier.fillMaxSize().letterBackdrop().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (opened) {
            // 봉투에서 펼쳐진 편지지를 그대로 이음
            // 여기서 다시 올라오면 깜빡임
            LetterSheet(Modifier.weight(1f), rises = false) { LetterBody(entries) }
        } else {
            LetterStage({ opening.value }, open, Modifier.weight(1f)) { card ->
                LetterSheet(Modifier.fillMaxSize(), card, rises = false) { LetterBody(entries) }
            }
        }
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DiaryButton(
                stringResource(if (opened) Res.string.letter_start else Res.string.letter_open),
                if (opened) onStart else open,
                Modifier.fillMaxWidth().alpha(buttonShown),
                size = DiaryButtonSize.XLarge
            )
        }
    }
}

/**
 * 설정에서 다시 읽는 편지
 * 봉투 없이 바로 편지지이고 계속하기 대신 뒤로 가기가 있음
 * 편지 아래에 의견 보내기. 처음 보는 편지에는 두지 않음
 */
@Composable
fun FullLetterScreen(entries: Int?, onBack: () -> Unit, modifier: Modifier = Modifier, onFeedback: (() -> Unit)? = null) {
    Column(modifier.fillMaxSize().letterBackdrop().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.letter_title), onBack = onBack)
        LetterSheet(
            Modifier.weight(1f).navigationBarsPadding(),
            centered = false,
            footer = { onFeedback?.let { FeedbackInvite(it) } }
        ) { LetterBody(entries) }
    }
}

/**
 * 편지를 다 읽은 자리의 의견 보내기
 * 편지가 주인공이라 약한 버튼 하나만 둠
 */
@Composable
private fun FeedbackInvite(onFeedback: () -> Unit) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = DiarySpacing.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        BasicText(
            stringResource(Res.string.letter_feedback_invite),
            style = DiaryTheme.typography.body.copy(
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineBreak = CenteredLineBreak
            )
        )
        DiaryButton(
            stringResource(Res.string.letter_feedback),
            onFeedback,
            size = DiaryButtonSize.Medium,
            kind = DiaryButtonKind.Weak
        )
    }
}

/**
 * 처음 본 편지와 다시 읽는 편지가 함께 쓰는 본문
 * 옮긴 일기가 없으면 고맙다는 문단은 없음
 */
@Composable
private fun LetterBody(entries: Int?) {
    entries?.takeIf { it > 0 }?.let { LetterParagraphs(pluralStringResource(Res.plurals.letter_moved, it, it.grouped())) }
    LetterParagraphs(stringResource(Res.string.letter_short))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DiaryShapes.r16)
            .letterBackdrop()
            .padding(DiarySpacing.x4),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        Promise(stringResource(Res.string.letter_promise_free))
        Promise(stringResource(Res.string.letter_promise_ads))
    }
    LetterParagraphs(stringResource(Res.string.letter_close))
}

/**
 * 편지지 한 장
 * 받는 사람, 인사, 본문, 보내는 사람 순서로 편지 꼴을 갖춤
 * 문단 사이는 빈 줄이 아니라 간격 16(줄 높이 28의 1.5배 이상이 되는 값, WCAG 1.4.8)
 * 설정에서 열 때는 한 번 살짝 올라오고, 봉투에서 나올 때와 동작 줄이기면 그대로
 */
@Composable
private fun LetterSheet(
    modifier: Modifier,
    cardModifier: Modifier = Modifier,
    rises: Boolean = true,
    centered: Boolean = true,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val still = !rises || rememberReduceMotion() || LocalInspectionMode.current
    val rise = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) { if (!still) rise.animateTo(1f, tween(RISE_MILLIS)) }
    // 처음 보는 편지는 아래 버튼이 있어 남는 자리의 가운데에 둠
    // 다시 읽는 편지는 위에 바만 있어 가운데에 두면 처져 보여 바 바로 아래부터 놓음
    // 길면 스크롤
    BoxWithConstraints(modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x4),
            verticalArrangement = if (centered) Arrangement.Center else Arrangement.Top
        ) {
            LetterCard(cardModifier, {
                alpha = rise.value
                translationY = (1f - rise.value) * DiarySpacing.x6.toPx()
            }, content)
            footer()
        }
    }
}

/**
 * 편지지 카드
 * 편지 꼴을 갖춘 면이고 테두리 없이 부드러운 모서리
 */
@Composable
private fun LetterCard(modifier: Modifier, layer: GraphicsLayerScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer(layer)
            .clip(DiaryShapes.r28)
            .background(colors.paper)
            .padding(DiarySpacing.x6),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
    ) {
        BasicText(stringResource(Res.string.letter_to), style = typography.label.copy(color = colors.accentText))
        BasicText(stringResource(Res.string.letter_hello), style = typography.title2.copy(color = colors.textPrimary))
        content()
        BasicText(
            stringResource(Res.string.letter_sign),
            Modifier.fillMaxWidth(),
            style = typography.label.copy(color = colors.textSecondary, textAlign = TextAlign.End)
        )
    }
}

/**
 * 약속 한 줄
 * 글에 묻히지 않게 표시를 앞에 둠
 */
@Composable
private fun Promise(text: String) {
    val colors = DiaryTheme.colors
    val body = DiaryTheme.typography.body
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        FirstLineIcon(DiaryIcons.CircleCheck, body, colors.textSecondary)
        BasicText(text, Modifier.weight(1f), style = body.copy(color = colors.textPrimary))
    }
}

/**
 * 문자열의 빈 줄을 문단으로 나눠 그림
 * 간격은 부모의 16을 따름
 */
@Composable
private fun LetterParagraphs(text: String) {
    val colors = DiaryTheme.colors
    val style = DiaryTheme.typography.reading.copy(color = colors.textPrimary)
    val weight = DiaryTheme.typography.bodyStrong.fontWeight ?: FontWeight.SemiBold
    text.split("\n\n").forEach { BasicText(emphasized(it, weight), style = style) }
}

/**
 * `**`로 감싼 곳만 굵게 함
 * 번역할 때 강조 자리를 문장에 맞게 옮기려고 문자열에 둠
 */
private fun emphasized(text: String, weight: FontWeight): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = weight)) { append(part) } else append(part)
    }
}
