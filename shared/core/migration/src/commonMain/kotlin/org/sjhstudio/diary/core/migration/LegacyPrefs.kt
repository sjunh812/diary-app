package org.sjhstudio.diary.core.migration

/**
 * 옛 앱 설정 파일(`shared_prefs/pref.xml`, `pref2.xml`)을 XML 글로 직접 읽는다(research R5).
 * `getSharedPreferences`로 열면 나중에 같은 이름으로 쓸 때 원본이 다시 쓰일 수 있어 쓰지 않는다.
 */
object LegacyPrefs {
    /** 값은 String, Boolean, Int, Long, Float, Set<String> 가운데 하나다. */
    fun parse(xml: String): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        STRING.findAll(xml).forEach { out[unescape(it.groupValues[1])] = unescape(it.groupValues[2]) }
        EMPTY_STRING.findAll(xml).forEach { out[unescape(it.groupValues[1])] = "" }
        SCALAR.findAll(xml).forEach { m ->
            val (type, name, value) = m.destructured
            val parsed: Any? =
                when (type) {
                    "boolean" -> value.toBooleanStrictOrNull()
                    "int" -> value.toIntOrNull()
                    "long" -> value.toLongOrNull()
                    "float" -> value.toFloatOrNull()
                    else -> null
                }
            if (parsed != null) out[unescape(name)] = parsed
        }
        SET.findAll(xml).forEach { m ->
            out[unescape(m.groupValues[1])] = SET_ITEM.findAll(m.groupValues[2]).map { unescape(it.groupValues[1]) }.toSet()
        }
        return out
    }

    /** 사본에 쓸 글. `password` 줄만 빼고 나머지는 한 글자도 바꾸지 않는다. */
    fun withoutPassword(xml: String): String = PASSWORD_LINE.replace(xml, "")

    private fun unescape(text: String): String = ENTITY.replace(text) { m ->
        when (val name = m.groupValues[1]) {
            "amp" -> "&"

            "lt" -> "<"

            "gt" -> ">"

            "quot" -> "\""

            "apos" -> "'"

            else ->
                when {
                    name.startsWith("#x") -> name.drop(2).toIntOrNull(16)
                    name.startsWith("#") -> name.drop(1).toIntOrNull()
                    else -> null
                }?.let { code -> if (code < 0x10000) code.toChar().toString() else m.value } ?: m.value
        }
    }

    private val STRING = Regex("""<string\s+name="([^"]*)"\s*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val EMPTY_STRING = Regex("""<string\s+name="([^"]*)"\s*/>""")
    private val SCALAR = Regex("""<(boolean|int|long|float)\s+name="([^"]*)"\s+value="([^"]*)"\s*/>""")
    private val SET = Regex("""<set\s+name="([^"]*)"\s*>(.*?)</set>""", RegexOption.DOT_MATCHES_ALL)
    private val SET_ITEM = Regex("""<string>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);""")

    /** 비밀번호 줄. 앞의 들여쓰기와 뒤의 줄바꿈까지 함께 뺀다. */
    private val PASSWORD_LINE =
        Regex("""[ \t]*<string\s+name="password"\s*(?:/>|>.*?</string>)[ \t]*\r?\n?""", RegexOption.DOT_MATCHES_ALL)
}
