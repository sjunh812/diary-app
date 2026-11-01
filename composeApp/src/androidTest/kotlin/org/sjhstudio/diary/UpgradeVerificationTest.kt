package org.sjhstudio.diary

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

/**
 * 1.7.4에서 올린 기기의 기록 이전을 기기 안에서 확인한다(스펙 001 T050, quickstart §2).
 * `scripts/legacy/upgrade-test.sh`가 1.7.4를 깔고 `seed.sh` 견본을 넣은 뒤 새 APK를 덮어 깔고 이 시험을 부른다.
 * 기대값은 `seed.sh`의 네 편과 덧붙인 큰 견본 수(`files/debug/seed.json`)다. 견본이 없는 기기에서는 건너뛴다.
 */
@RunWith(AndroidJUnit4::class)
class UpgradeVerificationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val seed = File(context.filesDir, "debug/seed.json")

    @Test
    fun legacy_diary_is_migrated_as_is() {
        assumeTrue("seed.sh 견본이 없는 기기", seed.exists())
        val extra = Regex("\"extra\"\\s*:\\s*(\\d+)").find(seed.readText())?.groupValues?.get(1)?.toInt() ?: 0
        ActivityScenario.launch(MainActivity::class.java).use { waitForMigration() }
        val db = GlobalContext.get().get<DiaryDatabase>()
        runBlocking {
            assertEquals(4 + extra, db.entryDao().countAll())

            // 1편: 글 세 문단, 기분·날씨·장소·즐겨찾기, 사진 두 장의 순서
            val first = checkNotNull(db.entryDao().getByLegacyId(1)).asModel()
            assertTrue(first.plainText.startsWith("퇴근길에 한강까지 걸었다."))
            assertTrue(first.plainText.endsWith("내일은 일찍 자야지."))
            assertEquals(Mood.fromLegacyCode(7), first.mood)
            assertEquals(Weather.fromLegacyCode(0), first.weather)
            assertEquals("서울 마포구", first.placeName)
            assertTrue(first.isFavorite)
            val photos = db.mediaDao().forEntry(first.id).map { it.asModel() }.sortedBy { it.sortOrder }
            assertEquals(listOf(MediaStatus.NORMAL, MediaStatus.NORMAL), photos.map { it.status })
            assertEquals(listOf("jpg", "png"), photos.map { it.fileName!!.substringAfterLast('.') })
            for (photo in photos) {
                assertTrue(File(context.filesDir, "media/${photo.fileName}").exists())
                // 썸네일도 이전할 때 만든다(FR-037).
                assertTrue(File(context.noBackupFilesDir, "thumbs/${photo.sha256}.jpg").exists())
            }

            // 2편: 이모지와 잘못된 밀리초 날짜도 글은 그대로
            assertTrue(checkNotNull(db.entryDao().getByLegacyId(2)).asModel().plainText.contains("😵"))

            // 3편: 캐시에서 지워진 사진은 자리만 남는다(FR-004)
            val third = checkNotNull(db.entryDao().getByLegacyId(3)).asModel()
            val missing = db.mediaDao().forEntry(third.id).map { it.asModel() }
            assertEquals(listOf(MediaStatus.MISSING), missing.map { it.status })
            assertTrue(missing.single().legacyPath!!.endsWith("gone.jpg"))

            // 4편: 기분·날씨가 없던 일기
            val fourth = checkNotNull(db.entryDao().getByLegacyId(4)).asModel()
            assertEquals(null, fourth.weather)
            assertFalse(fourth.isFavorite)
        }
    }

    /** 이전이 끝날 때까지 기다린다. 이전은 화면의 관문이 시작한다. */
    private fun waitForMigration() {
        val state = File(context.filesDir, "migration/state.json")
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val text = runCatching { state.readText() }.getOrDefault("")
            if (Regex("\"step\":\"(Done|Partial)\"").containsMatchIn(text)) return
            assertFalse("이전이 실패했어요: $text", text.contains("\"step\":\"Failed\""))
            Thread.sleep(POLL_MILLIS)
        }
        throw AssertionError("이전이 ${TIMEOUT_MILLIS / 1000}초 안에 끝나지 않았어요")
    }

    private companion object {
        const val TIMEOUT_MILLIS = 300_000L
        const val POLL_MILLIS = 250L
    }
}
