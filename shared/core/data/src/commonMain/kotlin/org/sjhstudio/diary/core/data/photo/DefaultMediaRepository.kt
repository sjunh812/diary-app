package org.sjhstudio.diary.core.data.photo

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.data.diary.MediaRepository
import org.sjhstudio.diary.core.database.DiaryDatabase

/**
 * 어디에서도 쓰지 않는 사진 파일을 지운다(스펙 002 data-model.md "사진 파일 정리").
 * 방금 고른 사진이 임시 저장본에 잇기 전에 지워지지 않게 하루 넘은 파일만 본다.
 * 휴지통 일기의 사진은 남긴다.
 */
class DefaultMediaRepository(
    private val db: DiaryDatabase,
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val thumbnailDir: Path,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val keepEverything: suspend () -> Boolean = { false }
) : MediaRepository {
    override suspend fun cleanUnused(now: Instant) {
        if (keepEverything()) return
        val used = (db.mediaDao().fileNames() + db.draftDao().fileNames()).toHashSet()
        val before = (now - 1.days).toEpochMilliseconds()
        withContext(io) {
            for (file in fileSystem.listOrNull(mediaDir).orEmpty()) {
                if (file.name in used) continue
                val modified = fileSystem.metadataOrNull(file)?.lastModifiedAtMillis ?: continue
                if (modified >= before) continue
                fileSystem.delete(file, mustExist = false)
                fileSystem.delete(thumbnailDir / "${file.name.substringBefore('.')}.jpg", mustExist = false)
            }
        }
    }
}
