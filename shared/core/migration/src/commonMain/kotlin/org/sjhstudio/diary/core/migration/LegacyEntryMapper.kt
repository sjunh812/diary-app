package org.sjhstudio.diary.core.migration

import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

/**
 * 레거시 줄 하나를 옮긴 결과.
 * 사진 줄은 [PhotoPaths]와 사진 옮기기가 따로 만든다.
 */
data class MappedEntry(
    val entry: Entry,
    /** 날짜나 시각을 읽지 못해 대체 규칙을 썼다(`MigrationReport.dateUnparsed`). */
    val dateUnparsed: Boolean,
    /** 앞 10글자 날짜와 옛 앱의 느슨한 해석 날짜가 다르다(`MigrationReport.dateDiffers`). */
    val dateDiffers: Boolean
)

/**
 * 레거시 `Note` 줄을 새 [Entry]로 옮긴다.
 * 규칙은 contracts/legacy-mapping.md "일기 줄" 표 그대로다.
 *
 * @param migratedOn 이전한 날(기기 시간대).
 * 날짜를 전혀 읽지 못한 줄에 쓴다.
 * @param timeZone 이전하는 기기의 시간대.
 * `created_at`·`updated_at`을 만든다.
 * @param newId 새 일기 ID(UUID).
 * 시험에서 고정한다.
 */
class LegacyEntryMapper(private val migratedOn: LocalDate, private val timeZone: TimeZone, private val newId: () -> String) {
    fun map(note: LegacyNote): MappedEntry {
        val created = note.createDate
        val strictDate = created?.let(::parseDate)
        val date = strictDate ?: note.modifyDate?.let(::parseDate) ?: migratedOn
        val strictTime = created?.let(::parseTime)
        val time = strictTime ?: LocalTime(0, 0)
        val createdAt = date.atTime(time).toInstant(timeZone)
        val updatedAt = note.modifyDate?.let(::parseDateTime)?.toInstant(timeZone) ?: createdAt
        val loose = created?.let(::looseLegacyDate)
        val contents = note.contents.orEmpty()
        return MappedEntry(
            entry =
                Entry(
                    id = newId(),
                    date = date,
                    time = time,
                    bodyHtml = legacyToHtml(contents),
                    plainText = contents,
                    mood = moodOf(note.mood),
                    weather = weatherOf(note.weather),
                    placeName = placeNameOf(note.address),
                    isFavorite = note.star == 1L,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    legacyId = note.id.toInt(),
                    legacyCreateDate = created,
                    legacyLocation = legacyLocationOf(note.locationX, note.locationY)
                ),
            dateUnparsed = strictDate == null || strictTime == null,
            dateDiffers = loose != null && loose != date
        )
    }

    companion object {
        fun moodOf(code: Long?): Mood? = code?.takeIf { it in 0L..8L }?.let { Mood.fromLegacyCode(it.toInt()) }

        fun weatherOf(code: Long?): Weather? = code?.takeIf { it in 0L..6L }?.let { Weather.fromLegacyCode(it.toInt()) }

        /** 앞뒤 공백을 뺀 값이 비면 null, 아니면 원문 그대로(자르지 않는다). */
        fun placeNameOf(address: String?): String? = address?.takeIf { it.isNotBlank() }

        /** 둘 다 비었거나 null이면 null, 아니면 `x|y` 원문. */
        fun legacyLocationOf(x: String?, y: String?): String? =
            if (x.isNullOrEmpty() && y.isNullOrEmpty()) null else "${x.orEmpty()}|${y.orEmpty()}"

        /** 앞 10글자가 `yyyy-MM-dd`이고 실제 있는 날짜면 그 날. */
        internal fun parseDate(text: String): LocalDate? {
            if (text.length < 10) return null
            val head = text.substring(0, 10)
            if (!DATE.matches(head)) return null
            return runCatching { LocalDate.parse(head) }.getOrNull()
        }

        /**
         * 12~16번째 글자가 `HH:mm`(00~23, 00~59)이면 그 시각.
         * 초 자리는 버린다.
         */
        internal fun parseTime(text: String): LocalTime? {
            if (text.length < 16) return null
            val head = text.substring(11, 16)
            if (!TIME.matches(head)) return null
            val hour = head.substring(0, 2).toInt()
            val minute = head.substring(3, 5).toInt()
            if (hour > 23 || minute > 59) return null
            return LocalTime(hour, minute)
        }

        /** `yyyy-MM-dd HH:mm`까지 읽히면 그 시각. */
        internal fun parseDateTime(text: String): LocalDateTime? {
            val date = parseDate(text) ?: return null
            val time = parseTime(text) ?: return null
            return date.atTime(time)
        }

        /**
         * 옛 앱이 `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")`(느슨한 해석)으로 읽은 날짜.
         * 넘치는 값은 다음 단위로 넘긴다.
         * 예 `2024-03-01 23:59:457` → 3월 2일 00:07:37.
         * 형식이 아예 다르면 null.
         */
        internal fun looseLegacyDate(text: String): LocalDate? {
            val m = LOOSE.find(text) ?: return null
            val (y, mo, d, h, mi, s) = m.destructured
            return runCatching {
                val base = LocalDate(y.toInt(), 1, 1).plus(DatePeriod(months = mo.toInt() - 1, days = d.toInt() - 1))
                val seconds = h.toLong() * 3600 + mi.toLong() * 60 + s.toLong()
                base.plus(DatePeriod(days = (seconds / 86_400).toInt()))
            }.getOrNull()
        }

        private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
        private val TIME = Regex("""\d{2}:\d{2}""")
        private val LOOSE = Regex("""^(\d{1,4})-(\d{1,2})-(\d{1,2}) (\d{1,4}):(\d{1,4}):(\d{1,6})""")
    }
}
