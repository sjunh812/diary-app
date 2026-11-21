package org.sjhstudio.diary.feature.write

import androidx.compose.ui.platform.Clipboard
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * 일기 편집기의 클립보드. 붙여넣기는 라이브러리 그대로, 복사만 줄을 `br`로 이은 HTML(`editorToClipboard`)로 올림
 * 라이브러리는 줄마다 `p`를 올려 다른 앱에 붙여넣으면 줄 사이에 빈 줄이 생김
 */
internal expect fun diaryClipboard(state: RichTextState, platform: Clipboard): Clipboard
