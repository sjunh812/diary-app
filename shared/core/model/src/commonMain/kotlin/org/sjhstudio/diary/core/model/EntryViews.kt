package org.sjhstudio.diary.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 목록 한 줄에 필요한 것만 담는다.
 * 일기 내용이 들어 있으니 로그나 사용 통계로 넘기지 않는다(헌법 II).
 * [photos]는 앱 저장소의 절대경로이고, 원래 기기에 있어 찾을 수 없는 사진은 null이다.
 */
data class EntryRow(
    val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val preview: String,
    val mood: Mood?,
    val weather: Weather?,
    val placeName: String?,
    val isFavorite: Boolean,
    val photos: List<String?>,
    /** 합치기에서 양쪽 다 고쳐 둘 다 남긴 일기. */
    val keptBoth: Boolean = false
) {
    override fun toString(): String = "EntryRow"
}

/**
 * 상세 화면 사진 한 장.
 * [path]가 null이면 원래 기기에 있어 찾을 수 없는 사진이다.
 */
data class EntryPhoto(val id: String, val path: String?, val width: Int?, val height: Int?, val pending: Boolean = false)

/**
 * 상세 화면.
 * 일기 내용이 들어 있어 [toString]에 담지 않는다.
 */
data class EntryDetail(val entry: Entry, val photos: List<EntryPhoto>, val keptBothWith: String? = null) {
    override fun toString(): String = "EntryDetail"
}

/**
 * 달력 칸과 기분 흐름.
 * [mood]는 그날 마지막 일기의 기분이고 [count]는 편수다.
 * [previousMood]는 바로 앞 일기의 기분이고 달력에서 뒤에 겹쳐 그린다.
 * [moods]는 그날 일기의 기분을 시간순으로 모은 것이고 통계가 센다.
 */
data class DayMood(
    val date: LocalDate,
    val mood: Mood?,
    val count: Int,
    val previousMood: Mood? = null,
    val moods: List<Mood> = listOfNotNull(mood)
)

/**
 * 가장 많은 기분.
 * 같은 수면 뒤에 있는 기분을 고르고, 기분이 없으면 null이다.
 */
fun List<Mood>.mostFrequent(): Mood? {
    val counts = groupingBy { it }.eachCount()
    val most = counts.values.maxOrNull()
    return lastOrNull { counts[it] == most }
}

/** 휴지통에서 영구 삭제까지 두는 날(FR-035). */
const val TRASH_KEEP_DAYS = 30

/**
 * 휴지통 한 줄.
 * [daysLeft]는 영구 삭제까지 남은 날이다.
 */
data class TrashedEntry(val row: EntryRow, val deletedAt: Instant, val daysLeft: Int) {
    override fun toString(): String = "TrashedEntry"
}

/**
 * 한 기간의 기분 개수.
 * 기분 없는 일기는 [noMood]로 따로 센다.
 */
data class MoodCounts(val counts: Map<Mood, Int>, val noMood: Int) {
    val total: Int get() = counts.values.sum() + noMood

    /**
     * 가장 많았던 기분.
     * 같으면 함께 돌려준다.
     * 기분이 하나도 없으면 비어 있다.
     */
    val top: Set<Mood>
        get() {
            val most = counts.values.maxOrNull() ?: return emptySet()
            if (most == 0) return emptySet()
            return counts.filterValues { it == most }.keys
        }
}
