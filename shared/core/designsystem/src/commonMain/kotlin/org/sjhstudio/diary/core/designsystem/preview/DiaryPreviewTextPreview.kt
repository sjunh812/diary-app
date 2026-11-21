package org.sjhstudio.diary.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.component.DiaryPreviewText
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

private const val WRAPPED = "아침에 일어나 창문을 열었더니 바람이 생각보다 차가워서 다시 이불 속으로 들어갔다\n점심\n저녁"
private const val SHORT = "아침\n점심\n저녁"

@Composable
private fun PreviewTexts() {
    val style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textPrimary)
    Column(Modifier.width(240.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 앞줄이 접혀 셋째 쓴 줄이 밀림
        DiaryPreviewText(AnnotatedString(WRAPPED), style, maxLines = 3)
        // 다 들어감
        DiaryPreviewText(AnnotatedString(SHORT), style, maxLines = 3)
    }
}

@Preview(name = "미리보기 글 · 라이트", locale = "ko")
@Composable
private fun PreviewTextLight() = PreviewCanvas(dark = false) { PreviewTexts() }

@Preview(name = "미리보기 글 · 다크", locale = "ko")
@Composable
private fun PreviewTextDark() = PreviewCanvas(dark = true) { PreviewTexts() }
