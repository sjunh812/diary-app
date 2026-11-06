package org.sjhstudio.diary.core.model

/**
 * 일기 본문 글자 크기 5단계.
 * 본문에만 걸리고 기기 글자 크기 위에 곱해진다(research R17).
 */
enum class BodyTextSize(val id: String, val fontSizeSp: Int, val lineHeightSp: Int) {
    SMALL("small", 15, 24),

    /**
     * 기본값.
     * 디자인 시스템의 `reading`과 같다.
     */
    MEDIUM("medium", 17, 28),
    LARGE("large", 19, 30),
    XLARGE("xlarge", 21, 34),
    XXLARGE("xxlarge", 24, 38)
    ;

    companion object {
        val DEFAULT = MEDIUM

        fun fromId(id: String?): BodyTextSize? = entries.firstOrNull { it.id == id }
    }
}
