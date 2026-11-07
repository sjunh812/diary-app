package org.sjhstudio.diary.core.model

/**
 * 날씨 7종.
 * DB와 백업에는 소문자 [id]를 둔다.
 */
enum class Weather(
    val id: String,
    /** 레거시 `weather` 값. */
    val legacyCode: Int
) {
    SUNNY("sunny", 0),
    PARTLY_CLOUDY("partly_cloudy", 1),
    MOSTLY_CLOUDY("mostly_cloudy", 2),
    CLOUDY("cloudy", 3),
    RAIN("rain", 4),
    SLEET("sleet", 5),
    SNOW("snow", 6)
    ;

    companion object {
        fun fromId(id: String?): Weather? = entries.firstOrNull { it.id == id }

        fun fromLegacyCode(code: Int): Weather? = entries.firstOrNull { it.legacyCode == code }
    }
}
