package org.sjhstudio.diary.feature.write

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.sjhstudio.diary.core.data.richtext.bodyToEditor
import org.sjhstudio.diary.core.data.richtext.editorToBody
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor

/**
 * 저장 값은 라이트 hex 하나지만 편집기는 지금 테마의 토큰 색으로 보여야 한다(contracts/rich-text-html.md).
 * 편집기에 넣을 때와 꺼낼 때 팔레트 색을 서로 바꾼다.
 */
internal class EditorColors(colors: DiaryColors) {
    val ink: Map<InkColor, Color> =
        mapOf(
            InkColor.RED to colors.ink.red,
            InkColor.ORANGE to colors.ink.orange,
            InkColor.GREEN to colors.ink.green,
            InkColor.BLUE to colors.ink.blue,
            InkColor.GRAY to colors.ink.gray,
            InkColor.PURPLE to colors.ink.purple,
            InkColor.PINK to colors.ink.pink
        )
    val mark: Map<MarkColor, Color> =
        mapOf(
            MarkColor.YELLOW to colors.mark.yellow,
            MarkColor.GREEN to colors.mark.green,
            MarkColor.BLUE to colors.mark.blue,
            MarkColor.PURPLE to colors.mark.purple,
            MarkColor.PINK to colors.mark.pink
        )

    private val pairs: List<Pair<String, String>> =
        ink.map { (key, color) -> key.hex to color.toHex() } + mark.map { (key, color) -> key.hex to color.toHex() }

    private val back: Map<String, String> = pairs.associate { (stored, shown) -> shown to stored }

    /**
     * 테마가 바뀌었는지 가르는 값.
     * 화면을 다시 만든 뒤에도 견줄 수 있게 글자로 둔다.
     */
    val signature: String = pairs.joinToString("") { (_, shown) -> shown }

    /** 저장된 라이트 hex를 지금 테마 색으로, 문단 안 줄바꿈을 에디터의 줄로. */
    fun toEditor(html: String): String =
        pairs.fold(bodyToEditor(html)) { text, (stored, shown) -> text.replace(stored, shown, ignoreCase = true) }

    /**
     * 편집기가 낸 `rgba()`나 hex를 저장 값으로.
     * 정리기가 팔레트 밖 색을 버리므로 여기서 꼭 돌려놓는다.
     */
    fun fromEditor(html: String): String {
        val normalized = RGB.replace(html) { match -> rgbToHex(match) ?: match.value }
        return editorToBody(HEX.replace(normalized) { match -> back[match.value.lowercase()] ?: match.value })
    }

    private fun rgbToHex(match: MatchResult): String? {
        val channels = match.groupValues[1].split(',').take(3).map { it.trim().toFloatOrNull()?.toInt() ?: return null }
        if (channels.size < 3) return null
        return "#" + channels.joinToString("") { it.coerceIn(0, 255).toString(16).padStart(2, '0') }
    }

    private companion object {
        val RGB = Regex("""rgba?\(([^)]*)\)""")
        val HEX = Regex("#[0-9a-fA-F]{6}")
    }
}

private fun Color.toHex(): String {
    val rgb = toArgb() and 0xFFFFFF
    return "#" + rgb.toString(16).padStart(6, '0')
}
