package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

// DB, 설정, 백업에 저장하는 ID가 data-model.md와 같은지 확인
// 바뀌면 이미 저장된 기록을 읽지 못해 시험으로 묶어 둠(헌법 I)
class StoredIdsTest {
    @Test
    fun mood_ids_and_legacy_codes() {
        assertEquals(
            listOf("angry" to 0, "cool" to 1, "sad" to 2, "sick" to 3, "laugh" to 4, "meh" to 5, "bad" to 6, "good" to 7, "tired" to 8),
            Mood.entries.map { it.id to it.legacyCode }
        )
        assertEquals(null, Mood.fromLegacyCode(-1))
        assertEquals(null, Mood.fromLegacyCode(9))
    }

    @Test
    fun weather_ids_and_legacy_codes() {
        assertEquals(
            listOf(
                "sunny" to 0,
                "partly_cloudy" to 1,
                "mostly_cloudy" to 2,
                "cloudy" to 3,
                "rain" to 4,
                "sleet" to 5,
                "snow" to 6
            ),
            Weather.entries.map { it.id to it.legacyCode }
        )
        assertEquals(null, Weather.fromLegacyCode(7))
    }

    @Test
    fun a_font_chosen_before_the_rename_moves_to_changwon_round() {
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("cafe24_surround"))
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("line_seed"))
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("binggrae"))
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("hyemin"))
    }

    @Test
    fun a_replaced_font_moves_to_the_font_in_its_place() {
        assertEquals(DiaryFont.HAKGYOANSIM_BADASSEUGI, DiaryFont.fromId("gowun_dodum"))
        assertEquals(DiaryFont.HAKGYOANSIM_BADASSEUGI, DiaryFont.fromId("gmarket_sans"))
        assertEquals(DiaryFont.HALLYM_MYUNGJO, DiaryFont.fromId("maru_buri"))
        assertEquals(DiaryFont.HALLYM_MYUNGJO, DiaryFont.fromId("kimjungchul_myungjo"))
        assertEquals(DiaryFont.HALLYM_MYUNGJO, DiaryFont.fromId("iropke_batang"))
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("nanum_pen"))
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("kcc_jeongbeom"))
        assertEquals(DiaryFont.CHANGWON_ROUND, DiaryFont.fromId("kyobo_2020"))
        assertEquals(DiaryFont.RIX_WATERMELON, DiaryFont.fromId("gamja_flower"))
        assertEquals(DiaryFont.SEJONG_GEULGGOT, DiaryFont.fromId("hi_melody"))
    }

    @Test
    fun font_ids() {
        assertEquals(
            listOf(
                "pretendard",
                "system",
                "hakgyoansim_badasseugi",
                "changwon_round",
                "hallym_myungjo",
                "gowun_batang",
                "ownglyph_wiseelist",
                "nanum_jeomggol",
                "nanum_cheer",
                "rix_watermelon",
                "sejong_geulggot"
            ),
            DiaryFont.entries.map { it.id }
        )
    }

    @Test
    fun body_text_sizes() {
        assertEquals(
            listOf(
                Triple("small", 15, 24),
                Triple("medium", 17, 28),
                Triple("large", 19, 30),
                Triple("xlarge", 21, 34),
                Triple("xxlarge", 24, 38)
            ),
            BodyTextSize.entries.map { Triple(it.id, it.fontSizeSp, it.lineHeightSp) }
        )
    }

    @Test
    fun setting_and_lock_ids() {
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.id })
        assertEquals(listOf("immediately", "one_minute", "five_minutes", "cold_start"), RelockAfter.entries.map { it.id })
        assertEquals(listOf("normal", "missing", "pending"), MediaStatus.entries.map { it.id })
        assertEquals(
            listOf("NotStarted", "Snapshotting", "Copying", "Verifying", "Committing", "Done", "Partial", "Failed", "Deferred"),
            MigrationStep.entries.map { it.id }
        )
    }

    @Test
    fun defaults_follow_data_model() {
        val settings = AppSettings()
        assertEquals(DiaryFont.PRETENDARD, settings.font)
        assertEquals(BodyTextSize.MEDIUM, settings.bodyTextSize)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(1320, settings.reminderMinuteOfDay)
        val lock = LockSettings()
        assertEquals(RelockAfter.ONE_MINUTE, lock.relockAfter)
        assertEquals("diary.pin.v1", lock.keyAlias)
        assertEquals(false, lock.hasPin)
    }
}
