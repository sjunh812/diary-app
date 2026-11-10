package org.sjhstudio.diary.core.migration

/**
 * 레거시 `contents`(서식 없는 글)를 본문 HTML로 바꿈(contracts/legacy-mapping.md "본문 변환")
 *
 * 1. 빈 글은 빈 글로
 * 2. 왼쪽부터 `"\n\n"`이 나올 때마다 잘라 덩어리를 만듦(`"a\n\n\nb"` → `["a", "\nb"]`)
 * 3. 덩어리마다 HTML 이스케이프 뒤 `"\n"`을 `<br>`로 바꾸고 `<p>`로 감쌈
 * 4. 공백, 탭, `\r`, 이모지는 건드리지 않음
 *
 * `String`의 UTF-16 단위로 다뤄 서로게이트 쌍이 덩어리 경계에 걸려도 안 깨짐(경계는 늘 `\n`)
 */
fun legacyToHtml(contents: String): String {
    if (contents.isEmpty()) return ""
    val out = StringBuilder(contents.length + 16)
    for (chunk in contents.split("\n\n")) {
        out.append("<p>")
        for (ch in chunk) {
            when (ch) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&#39;")
                '\n' -> out.append("<br>")
                else -> out.append(ch)
            }
        }
        out.append("</p>")
    }
    return out.toString()
}
