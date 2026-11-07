package org.sjhstudio.diary.core.platform

/**
 * 지금 있는 곳의 동네 이름 찾기(스펙 002 FR-005).
 * 사용자가 [내 위치 가져오기]를 누른 때만 부른다.
 * 좌표는 운영체제의 주소 변환에만 넘기고 어디에도 남기지 않는다(헌법 II).
 * 권한을 묻는 시스템 창은 화면이 띄운다.
 * 여기서는 지금 상태만 읽는다.
 */
interface PlaceFinder {
    suspend fun find(): PlaceLookup
}

/**
 * 찾은 결과.
 * 실패도 값이다.
 * 쓰기를 막지 않고 한 줄로 알린다.
 */
sealed interface PlaceLookup {
    data class Found(val name: String) : PlaceLookup

    data object Denied : PlaceLookup

    data object LocationOff : PlaceLookup

    data object NotFound : PlaceLookup
}

/** 위치를 쓸 수 없는 자리(미리보기, 시험). */
object NoPlaceFinder : PlaceFinder {
    override suspend fun find(): PlaceLookup = PlaceLookup.NotFound
}

/**
 * 운영체제 지오코더가 준 주소 조각.
 * 비어 있는 칸이 흔하다.
 */
data class PlaceParts(
    val countryCode: String? = null,
    val region: String? = null,
    val city: String? = null,
    val district: String? = null,
    val street: String? = null
) {
    /**
     * 일기에 남길 글자.
     * 한국은 1.7.4처럼 "마포구 망원동"으로 쓴다.
     * 길 이름은 동네가 아니니 쓰지 않는다.
     * 동을 모르면 시·군·구까지만 쓴다.
     * 다른 나라는 작은 지역부터 두 칸을 쓴다.
     */
    fun label(): String? {
        val area = listOf(district, city, region).mapNotNull { it?.trim()?.ifEmpty { null } }.distinct()
        if (countryCode != KOREA) return area.take(2).joinToString(", ").ifEmpty { null }
        val neighborhood = street?.trim()?.takeIf { NEIGHBORHOOD.containsMatchIn(it) }
        return listOfNotNull(area.firstOrNull(), neighborhood).joinToString(" ").ifEmpty { null }
    }

    private companion object {
        const val KOREA = "KR"

        // 동·읍·면·리·가.
        // 영어와 일본어 기기에서는 같은 말이 다르게 온다.
        val NEIGHBORHOOD = Regex("([동읍면리가]|洞|邑|面|里|街|-dong|-eup|-myeon|-ri|-ga)$")
    }
}
