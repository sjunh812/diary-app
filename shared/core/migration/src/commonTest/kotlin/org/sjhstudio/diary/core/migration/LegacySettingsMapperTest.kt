package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.LegacyBackupLink
import org.sjhstudio.diary.core.model.MigratedSetting
import org.sjhstudio.diary.core.model.ThemeMode

// 옛 설정 옮기기(스펙 001 T065, data-model.md DiaryFont·BodyTextSize 표).
class LegacySettingsMapperTest {
    private fun map(pref: Map<String, Any> = emptyMap(), pref2: Map<String, Any> = emptyMap()) = LegacySettingsMapper.map(pref, pref2)

    @Test
    fun font_key_follows_the_table() {
        val expected =
            mapOf(
                -1 to DiaryFont.PRETENDARD, 0 to DiaryFont.PRETENDARD, 100 to DiaryFont.SYSTEM,
                1 to DiaryFont.KKOMI, 5 to DiaryFont.KKOMI,
                2 to DiaryFont.NANUM_PEN, 7 to DiaryFont.NANUM_PEN, 11 to DiaryFont.NANUM_PEN,
                3 to DiaryFont.NANUM_JEOMGGOL,
                4 to DiaryFont.GAMJA_FLOWER, 10 to DiaryFont.GAMJA_FLOWER,
                6 to DiaryFont.HI_MELODY, 13 to DiaryFont.HI_MELODY,
                8 to DiaryFont.GOWUN_BATANG,
                9 to DiaryFont.CAFE24_SURROUND, 12 to DiaryFont.CAFE24_SURROUND,
                99 to DiaryFont.PRETENDARD
            )
        for ((key, font) in expected) assertEquals(font, map(mapOf("font_key" to key)).font, "font_key $key")
        assertEquals(DiaryFont.PRETENDARD, map().font)
    }

    /** 같은 글꼴이 없으면 옛 이름을 남겨 처음 열 때 한 번 알린다. */
    @Test
    fun replaced_font_keeps_the_old_name() {
        for (same in listOf(-1, 0, 1, 3, 100)) assertNull(map(mapOf("font_key" to same)).fontReplacedFrom, "font_key $same")
        assertEquals("교보손글씨체", map(mapOf("font_key" to 2)).fontReplacedFrom)
        assertEquals("미니콩다방체", map(mapOf("font_key" to 5)).fontReplacedFrom)
        assertEquals("ACC 어린이 마음고운체", map(mapOf("font_key" to 13)).fontReplacedFrom)
        assertNull(map(mapOf("font_key" to 99)).fontReplacedFrom)
    }

    @Test
    fun text_size_theme_and_location() {
        val sizes = mapOf(
            0 to BodyTextSize.SMALL,
            1 to BodyTextSize.SMALL,
            2 to BodyTextSize.MEDIUM,
            3 to BodyTextSize.LARGE,
            4 to BodyTextSize.XLARGE,
            7 to BodyTextSize.MEDIUM
        )
        for ((key, size) in sizes) assertEquals(size, map(mapOf("font_size" to key)).bodyTextSize, "font_size $key")
        assertEquals(BodyTextSize.MEDIUM, map().bodyTextSize)

        assertEquals(ThemeMode.SYSTEM, map(mapOf("mode_key" to 0)).themeMode)
        assertEquals(ThemeMode.LIGHT, map(mapOf("mode_key" to 1)).themeMode)
        assertEquals(ThemeMode.DARK, map(mapOf("mode_key" to 2)).themeMode)
        assertEquals(ThemeMode.SYSTEM, map(mapOf("mode_key" to 5)).themeMode)

        assertEquals(true, map().askLocationAgain)
        assertEquals(false, map(mapOf("ask_location" to false)).askLocationAgain)
    }

    @Test
    fun reminder_time_out_of_range_becomes_22() {
        val on = map(pref2 = mapOf("is_alarm_key" to true, "hour_key" to 21, "minute_key" to 30))
        assertEquals(true, on.reminderEnabled)
        assertEquals(21 * 60 + 30, on.reminderMinuteOfDay)

        for (bad in listOf(
            mapOf("hour_key" to 24, "minute_key" to 0),
            mapOf("hour_key" to 7, "minute_key" to 60),
            mapOf(
                "hour_key" to -1,
                "minute_key" to 5
            ),
            emptyMap()
        )) {
            assertEquals(
                AppSettings.DEFAULT_REMINDER_MINUTE_OF_DAY,
                map(pref2 = bad + ("is_alarm_key" to true)).reminderMinuteOfDay,
                bad.toString()
            )
        }
        assertEquals(false, map().reminderEnabled)
    }

    @Test
    fun legacy_backup_link() {
        val pref = mapOf("diary&me@gmail.com" to true, "diary&me@gmail.com's id" to "1AbC_dEf", "use_pw" to true)
        assertEquals(LegacyBackupLink("diary&me@gmail.com", "1AbC_dEf"), map(pref).legacyBackup)
        assertEquals(LegacyBackupLink("a@b.co", null), map(mapOf("a@b.co" to true)).legacyBackup)
        assertNull(map(mapOf("a@b.co" to false)).legacyBackup)
    }

    @Test
    fun migrated_lists_only_settings_the_old_file_had() {
        assertEquals(emptyList(), LegacySettingsMapper.migrated(emptyMap(), emptyMap()))
        val all =
            LegacySettingsMapper.migrated(
                mapOf(
                    "font_key" to 3,
                    "font_size" to 2,
                    "mode_key" to 2,
                    "ask_location" to false,
                    "use_pw" to true,
                    "finger_print" to true
                ),
                mapOf("is_alarm_key" to false, "hour_key" to 21, "minute_key" to 30)
            )
        assertEquals(MigratedSetting.entries.toList(), all)
        // 잠금을 끈 채 남은 생체 인증 값은 옮긴 것이 아니다.
        val noLock = LegacySettingsMapper.migrated(mapOf("use_pw" to false, "finger_print" to true), emptyMap())
        assertEquals(emptyList(), noLock)
    }
}
