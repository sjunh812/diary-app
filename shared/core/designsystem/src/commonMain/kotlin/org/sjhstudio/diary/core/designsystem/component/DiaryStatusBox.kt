package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.LineBreak
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 상태 상자(`docs/design.md` 상태 상자, Figma "permission-needed").
 * 안내 상자와 한 집안이다.
 * 바탕 sunken · 모서리 16 · 안쪽 20 · 머리와 설명 사이 6 · 버튼은 설명과 사이 16(`docs/design.md` 상자 여백).
 * 머리 줄은 아이콘 16 + 사이 8 + `bodyStrong`, 아래 설명 `body`, 있으면 작은 약한 버튼.
 */
@Composable
fun DiaryStatusBox(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryKind: DiaryButtonKind = DiaryButtonKind.Weak,
    quietLabel: String? = null,
    onQuiet: (() -> Unit)? = null
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(DiaryShapes.r16)
                .background(colors.sunken)
                .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
    ) {
        // 머리가 두 줄이 되면 4로는 설명과 붙어 보여서 6을 둔다(2026-11-08 사용자 지적).
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.Top) {
                FirstLineIcon(icon, typography.bodyStrong, colors.textPrimary)
                // 한글은 글자마다 줄이 바뀌어 마지막 줄에 "요" 한 글자만 남았다(2026-11-08 사용자 지적).
                // 낱말 안 글자 사이를 묶어 띄어쓰기에서만 줄이 바뀌게 하고, 줄은 고르게 나눈다.
                // 붙임 글자는 그릴 때만 쓰고, 화면 읽기와 시험에는 원래 머리를 준다.
                BasicText(
                    keepWords(title),
                    Modifier.clearAndSetSemantics { text = AnnotatedString(title) },
                    style = typography.bodyStrong.copy(color = colors.textPrimary, lineBreak = LineBreak.Heading)
                )
            }
            PhraseText(body, style = typography.body.copy(color = colors.textSecondary))
        }
        if (actionLabel != null && onAction != null) {
            // 큰 글자나 긴 언어에서 두 버튼이 한 줄에 들어가지 않으면 아래 줄로 내려간다.
            // 줄 사이 16은 누르는 곳 48끼리 겹치지 않는 값이다.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
            ) {
                DiaryButton(actionLabel, onClick = onAction, size = DiaryButtonSize.Small, kind = DiaryButtonKind.Weak)
                if (secondaryLabel != null && onSecondary != null) {
                    DiaryButton(secondaryLabel, onClick = onSecondary, size = DiaryButtonSize.Small, kind = secondaryKind)
                }
                if (quietLabel != null && onQuiet != null) {
                    DiaryButton(quietLabel, onClick = onQuiet, size = DiaryButtonSize.Small, kind = DiaryButtonKind.QuietText)
                }
            }
        }
    }
}

/** 한글 낱말 안 글자 사이에 줄바꿈하지 않는 표시(U+2060)를 넣는다. 화면 표시용이다. */
internal fun keepWords(text: String): String = buildString(text.length * 2) {
    text.forEachIndexed { index, char ->
        if (index > 0 && char.isHangul() && text[index - 1].isHangul()) append(WORD_JOINER)
        append(char)
    }
}

private fun Char.isHangul(): Boolean = this in '\uAC00'..'\uD7A3'

private const val WORD_JOINER = '\u2060'
