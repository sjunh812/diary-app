package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import org.sjhstudio.diary.core.designsystem.theme.CenteredLineBreak
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

/**
 * 결과·진행 화면의 머리(`docs/design.md` 결과 머리, SEED Result Section).
 * 배지 56 · 제목 `title2` · 설명 `body` · 가운데 맞춤 · 위아래 24 · 배지와 제목 사이 20 · 제목과 설명 사이 8.
 * 칸을 채워 놓으면 내용이 세로 가운데에 옴.
 */
@Composable
fun DiaryResultHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: DiaryBadgeTone = DiaryBadgeTone.Accent
) = DiaryResultHeader(icon, title, description?.let(::AnnotatedString), modifier, tone)

/** 설명 안에 강조한 낱말이 있는 [DiaryResultHeader]. */
@Composable
fun DiaryResultHeader(
    icon: ImageVector,
    title: String,
    description: AnnotatedString?,
    modifier: Modifier = Modifier,
    tone: DiaryBadgeTone = DiaryBadgeTone.Accent
) {
    val colors = DiaryTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2, Alignment.CenterVertically)
    ) {
        DiaryHeroBadge(icon, tone = tone)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(
            title,
            style = DiaryTheme.typography.title2.copy(
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                lineBreak = CenteredLineBreak
            ),
            modifier = Modifier.semantics { heading() }
        )
        if (description != null) {
            BasicText(
                description,
                style = DiaryTheme.typography.body.copy(
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineBreak = CenteredLineBreak
                )
            )
        }
    }
}
