package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.testing.FakeDrive

/**
 * 사진을 받다가 일이 꼬였을 때 남은 사진이 돌아오는지 본다.
 * 파일 복원 중 다른 파일 고르기, 앱 종료, 공간 부족, 네트워크 끊김을 다룬다.
 */
class RestorePhotosTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var drive: FakeDrive
    private lateinit var a: TestPhone
    private lateinit var b: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "restore-photos-${Random.nextLong()}"
        drive = FakeDrive(fs)
        a = TestPhone(dir / "a", drive, name = "A")
        b = TestPhone(dir / "b", drive, name = "B")
    }

    @AfterTest
    fun tearDown() {
        a.close()
        b.close()
        fs.deleteRecursively(dir)
    }

    /** 일기 둘과 사진 둘을 묶음 파일로 낸다. */
    private suspend fun export(name: String = "export.zip"): Path {
        a.write("one")
        a.write("two")
        a.photo("one", 0, "첫째 사진")
        a.photo("two", 0, "둘째 사진")
        val zip = dir / name
        a.exportTo(zip)
        return zip
    }

    private fun other(name: String, text: String): Path {
        val path = dir / name
        fs.write(path) { writeUtf8(text) }
        return path
    }

    private suspend fun RestoreController.pick(path: Path) = prepareFile(fs.source(path))

    // 복원 중 다른 파일 고르기

    @Test
    fun picking_another_file_during_a_restore_is_refused_and_every_photo_arrives() = runTest {
        val zip = export()
        val controller = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(controller.pick(zip))
        // 백업이 돌고 있어 복원이 시작만 하고 기다린다.
        b.gate.enter()
        controller.merge()

        val second = controller.pick(other("second.zip", "다른 파일"))

        assertEquals(RestoreFailure.BUSY, assertIs<RestorePreparation.Failed>(second).reason)
        assertEquals(RestoreFailure.BUSY, assertIs<RestorePreparation.Failed>(controller.prepare("snapshot")).reason)
        b.gate.leave()
        val result = assertIs<RestoreState.Done>(controller.finished()).result
        assertEquals(2, result.photosRestored)
        assertEquals(0, result.photosMissing)
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
    }

    // 찾을 수 없는 사진 다시 받기

    @Test
    fun a_photo_marked_missing_comes_back_when_the_same_file_is_restored_again() = runTest {
        val zip = export()
        val first = b.controller(backgroundScope)
        assertIs<RestorePreparation.Ready>(first.pick(zip))
        first.merge()
        assertIs<RestoreState.Done>(first.finished())
        first.dismiss()
        // 받던 파일이 깨져 찾을 수 없는 사진으로 굳었다.
        val photos = b.db.backupDao().allMedia()
        photos.forEach { row ->
            fs.delete(b.media / requireNotNull(row.fileName))
            b.db.mediaDao().update(row.copy(status = MediaStatus.MISSING.id))
        }

        val again = b.controller(backgroundScope)
        val ready = assertIs<RestorePreparation.Ready>(again.pick(zip))
        assertEquals(2, ready.preview.photosToDownload)
        again.merge()

        val result = assertIs<RestoreState.Done>(again.finished()).result
        assertEquals(2, result.photosRestored)
        assertEquals(0, result.photosMissing)
        assertEquals(setOf(MediaStatus.NORMAL.id), b.photoStates().values.toSet())
        photos.forEach { assertEquals(it.sha256, fs.hashes(b.media / requireNotNull(it.fileName)).sha256) }
    }

    @Test
    fun a_missing_photo_comes_back_from_the_drive_too() = runTest {
        a.write("one")
        a.photo("one", 0, "첫째 사진")
        a.clock.advance(1.days)
        assertIs<BackupOutcome.Done>(a.runner().run())
        val first = b.controller(backgroundScope)
        first.prepare(first.backups()!!.first().snapshotName)
        first.merge()
        assertIs<RestoreState.Done>(first.finished())
        first.dismiss()
        val row = b.db.backupDao().allMedia().single()
        fs.delete(b.media / requireNotNull(row.fileName))
        b.db.mediaDao().update(row.copy(status = MediaStatus.MISSING.id))

        val again = b.controller(backgroundScope)
        val ready = assertIs<RestorePreparation.Ready>(again.prepare(again.backups()!!.first().snapshotName))
        assertEquals(1, ready.preview.photosToDownload)
        again.merge()

        assertEquals(1, assertIs<RestoreState.Done>(again.finished()).result.photosRestored)
        assertEquals(MediaStatus.NORMAL.id, b.db.backupDao().allMedia().single().status)
    }

    @Test
    fun a_missing_photo_without_a_hash_stays_as_it_is() = runTest {
        val zip = export()
        b.write("mine")
        val lost = TestPhone.photoRow("mine", 0, "없는 사진").copy(status = MediaStatus.MISSING.id, sha256 = null, fileName = null)
        b.db.mediaDao().insertAll(listOf(lost))

        val controller = b.controller(backgroundScope)
        val ready = assertIs<RestorePreparation.Ready>(controller.pick(zip))
        assertEquals(2, ready.preview.photosToDownload)
        controller.merge()
        controller.finished()

        assertEquals(MediaStatus.MISSING.id, b.photoStates()["mine-0"])
    }
}
