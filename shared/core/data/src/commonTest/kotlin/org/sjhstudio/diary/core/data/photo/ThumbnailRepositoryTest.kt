package org.sjhstudio.diary.core.data.photo

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class ThumbnailRepositoryTest {
    private val fs = FileSystem.SYSTEM
    private val root: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "diary-thumbs-${Random.nextLong().toULong()}"
    private val media = root / "media"
    private val thumbs = root / "thumbs"
    private var calls = 0
    private val dispatcher = StandardTestDispatcher()

    private fun repository(works: Boolean = true) = ThumbnailRepository(
        fs,
        media,
        thumbs,
        { _, target, maxSide, quality ->
            calls++
            assertEquals(480 to 80, maxSide to quality)
            if (works) fs.write(target.toPath()) { writeUtf8("jpeg") }
            works
        },
        dispatcher
    )

    private fun photo(name: String): Path {
        fs.createDirectories(media)
        return (media / name).also { fs.write(it) { writeUtf8("original") } }
    }

    @AfterTest
    fun cleanUp() = fs.deleteRecursively(root)

    /** 처음에만 만들고 다음부터는 있는 것을 쓴다. 이름은 원본 해시다. */
    @Test
    fun made_once_then_reused() = runTest(dispatcher) {
        val repo = repository()
        val original = photo("abc123.png")
        val first = repo.thumbnail(original)
        assertEquals(thumbs / "abc123.jpg", first)
        assertEquals(first, repo.thumbnail(original))
        assertEquals(1, calls)
        assertTrue(fs.list(thumbs).none { it.name.endsWith(".tmp") })
    }

    /** 지워졌으면 다시 만든다(FR-037). */
    @Test
    fun deleted_thumbnail_is_made_again() = runTest(dispatcher) {
        val repo = repository()
        val original = photo("abc123.jpg")
        fs.delete(repo.thumbnail(original)!!)
        repo.thumbnail(original)
        assertEquals(2, calls)
    }

    @Test
    fun broken_photo_gives_null_and_leaves_nothing() = runTest(dispatcher) {
        val repo = repository(works = false)
        assertNull(repo.thumbnail(photo("broken.jpg")))
        assertTrue(!fs.exists(thumbs) || fs.list(thumbs).isEmpty())
    }

    /** 사진 폴더 밖의 파일(예전 앱 캐시 경로)은 맡지 않는다. */
    @Test
    fun files_outside_media_are_not_handled() = runTest(dispatcher) {
        fs.createDirectories(root / "cache")
        val outside = (root / "cache" / "1699000000001.jpg").also { fs.write(it) { writeUtf8("x") } }
        assertNull(repository().thumbnail(outside))
        assertEquals(0, calls)
    }
}
