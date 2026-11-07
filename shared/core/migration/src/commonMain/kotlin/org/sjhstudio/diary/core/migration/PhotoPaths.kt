package org.sjhstudio.diary.core.migration

/** 레거시 `picture` 열과 사진 파일 이름 규칙(contracts/legacy-mapping.md "사진"). */
object PhotoPaths {
    /**
     * `","`로 나누고 빈 조각과 공백뿐인 조각을 버린다(옛 앱은 끝에도 `,`를 붙인다).
     * 남은 조각의 순서가 `sort_order`다.
     * 조각은 원문 그대로 돌려준다.
     */
    fun fragments(picture: String?): List<String> = picture.orEmpty().split(',').filter { it.isNotBlank() }

    /**
     * 파일을 찾을 경로 차례.
     * 원문 경로에 없고 앞뒤 공백이 있으면 다듬은 경로를 한 번 더 본다.
     */
    fun candidates(fragment: String): List<String> {
        val trimmed = fragment.trim()
        return if (trimmed == fragment) listOf(fragment) else listOf(fragment, trimmed)
    }

    /**
     * `<sha256>.<ext>`.
     * 절대경로를 담지 않는다.
     */
    fun fileName(sha256: String, extension: String): String = "$sha256.$extension"

    /**
     * 확장자: 경로 확장자를 소문자로.
     * 없거나 알 수 없으면 파일 머리 바이트로, 그래도 모르면 `bin`.
     */
    fun extension(path: String, head: ByteArray): String {
        val name = path.trim().substringAfterLast('/')
        val dot = name.lastIndexOf('.')
        if (dot in 1 until name.length - 1) {
            val ext = name.substring(dot + 1).lowercase()
            if (ext in KNOWN) return ext
        }
        return sniff(head) ?: UNKNOWN
    }

    /** 원본 없이 사본을 찾을 때 볼 확장자 차례. 머리 바이트를 볼 수 없어 경로의 확장자를 먼저 본다. */
    fun extensions(path: String): List<String> = (listOf(extension(path, ByteArray(0))) + KNOWN + UNKNOWN).distinct()

    internal fun sniff(head: ByteArray): String? {
        fun at(offset: Int, vararg bytes: Int) =
            head.size >= offset + bytes.size && bytes.indices.all { head[offset + it] == bytes[it].toByte() }

        fun ascii(offset: Int, text: String) = at(offset, *text.map { it.code }.toIntArray())
        return when {
            at(0, 0xFF, 0xD8, 0xFF) -> "jpg"
            at(0, 0x89, 0x50, 0x4E, 0x47) -> "png"
            ascii(0, "RIFF") && ascii(8, "WEBP") -> "webp"
            ascii(4, "ftypheic") || ascii(4, "ftypmif1") -> "heic"
            else -> null
        }
    }

    private const val UNKNOWN = "bin"
    private val KNOWN = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
}
