package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import okio.Path
import okio.Path.Companion.toPath
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.data.photo.ThumbnailEncoder
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.platform.sha256Hex
import org.sjhstudio.diary.core.testing.TestClock

// 옛 앱의 큰 사진은 줄인 사본으로 옮기고, 원본은 그대로 두며, 줄이지 못하면 그대로 복사해 잃지 않음
class MediaImporterShrinkTest {
    /** 파일 머리로 크기를 정하는 가짜. BIG은 4000×3000, SMALL은 줄인 크기, 나머지는 작은 사진 */
    private fun sizeOf(dir: TempDir) = ImageSizeReader { path ->
        val head = dir.fs.read(path.toPath()) { readUtf8() }
        when {
            head.startsWith("BIG") -> 4000 to 3000
            head.startsWith("SMALL") -> PhotoStore.MAX_SIDE to 1920
            else -> 800 to 600
        }
    }

    /** 줄인 사본을 쓰는 가짜 인코더. [works]가 거짓이면 실패 */
    private class Encoder(var works: Boolean = true, var throws: Boolean = false, var writes: String = "SMALL") : ThumbnailEncoder {
        var calls = 0
        var lastMaxSide = 0
        var lastQuality = 0

        override fun encode(source: String, target: String, maxSide: Int, quality: Int): Boolean {
            calls++
            lastMaxSide = maxSide
            lastQuality = quality
            if (throws) throw IllegalArgumentException("bitmap")
            if (!works) return false
            okio.FileSystem.SYSTEM.write(target.toPath()) { writeUtf8("$writes of ${source.toPath().name}") }
            return true
        }
    }

    private fun importer(dir: TempDir, encoder: ThumbnailEncoder?) =
        MediaImporter(dir.fs, dir.root / "media", sizeOf(dir), TestClock(), newId = { "id" }, shrink = encoder)

    private fun hash(dir: TempDir, path: Path) = dir.fs.sha256Hex(path)

    @Test
    fun a_big_photo_is_kept_as_a_smaller_jpeg_and_the_original_stays() {
        TempDir().use { dir ->
            val source = dir.write("cache/big.png", "BIG photo".encodeToByteArray())
            val before = hash(dir, source)
            val encoder = Encoder()

            val photo = importer(dir, encoder).importOne("e1", 0, source.toString())

            assertEquals(MediaStatus.NORMAL, photo.media.status)
            assertTrue(photo.media.fileName!!.endsWith(".jpg"))
            assertEquals(PhotoStore.MAX_SIDE, photo.media.width)
            assertNotEquals(before, photo.media.sha256)
            assertEquals(PhotoStore.MAX_SIDE to PhotoStore.QUALITY, encoder.lastMaxSide to encoder.lastQuality)
            // 검증 단계가 해시를 다시 재도 맞음
            assertTrue(importer(dir, encoder).verify(photo.media.fileName!!))
            // 옛 앱의 원본은 그대로
            assertEquals(before, hash(dir, source))
            // 쓰다 만 임시 파일이 남지 않음
            assertEquals(listOf(photo.media.fileName), dir.fs.list(dir.root / "media").map { it.name })
        }
    }

    @Test
    fun a_small_photo_is_copied_as_it_is() {
        TempDir().use { dir ->
            val source = dir.write("cache/small.jpg", "small photo".encodeToByteArray())
            val encoder = Encoder()

            val photo = importer(dir, encoder).importOne("e1", 0, source.toString())

            assertEquals(hash(dir, source), photo.media.sha256)
            assertEquals(0, encoder.calls)
        }
    }

    @Test
    fun a_photo_that_cannot_be_shrunk_is_copied_as_it_is() {
        TempDir().use { dir ->
            val source = dir.write("cache/big.png", "BIG photo".encodeToByteArray())

            val photo = importer(dir, Encoder(works = false)).importOne("e1", 0, source.toString())

            assertEquals(MediaStatus.NORMAL, photo.media.status)
            assertEquals(hash(dir, source), photo.media.sha256)
            assertTrue(photo.media.fileName!!.endsWith(".png"))
        }
    }

    /** 다시 시도할 때 OS가 캐시의 원본을 지웠어도 앞서 만든 줄인 사본을 찾음 */
    @Test
    fun a_retry_finds_the_shrunk_copy_after_the_original_is_gone() {
        TempDir().use { dir ->
            val source = dir.write("cache/big.png", "BIG photo".encodeToByteArray())
            val first = importer(dir, Encoder()).importOne("e1", 0, source.toString())
            dir.fs.delete(source)

            val again = importer(dir, Encoder()).importOne("e1", 0, source.toString(), knownSha256 = first.media.sha256)

            assertEquals(MediaStatus.NORMAL, again.media.status)
            assertEquals(first.media.fileName, again.media.fileName)
        }
    }

    @Test
    fun an_encoder_that_throws_still_copies_the_original() {
        TempDir().use { dir ->
            val source = dir.write("cache/big.png", "BIG photo".encodeToByteArray())

            val photo = importer(dir, Encoder(throws = true)).importOne("e1", 0, source.toString())

            assertEquals(MediaStatus.NORMAL, photo.media.status)
            assertEquals(hash(dir, source), photo.media.sha256)
            assertEquals(listOf(photo.media.fileName), dir.fs.list(dir.root / "media").map { it.name })
        }
    }

    /** 줄인 파일을 다시 읽지 못하면 그 사본은 버리고 원본을 복사함 */
    @Test
    fun an_unreadable_shrunk_copy_falls_back_to_the_original() {
        TempDir().use { dir ->
            val source = dir.write("cache/big.png", "BIG photo".encodeToByteArray())
            val sizes = ImageSizeReader { path -> if (path.endsWith("big.png")) 4000 to 3000 else null }
            val importer =
                MediaImporter(dir.fs, dir.root / "media", sizes, TestClock(), newId = { "id" }, shrink = Encoder(writes = "BROKEN"))

            val photo = importer.importOne("e1", 0, source.toString())

            assertEquals(hash(dir, source), photo.media.sha256)
            assertEquals(listOf(photo.media.fileName), dir.fs.list(dir.root / "media").map { it.name })
        }
    }
}
