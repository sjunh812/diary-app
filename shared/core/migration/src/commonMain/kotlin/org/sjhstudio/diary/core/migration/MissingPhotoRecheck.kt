package org.sjhstudio.diary.core.migration

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 찾을 수 없는 사진 중 원래 경로를 아는 것을 하루 한 번 다시 찾음(FR-011)
 * 있으면 이전과 같은 방법으로 옮기고 `normal`로 바꿈
 */
internal class MissingPhotoRecheck(
    private val store: MigrationStateStore,
    private val importer: MediaImporter,
    private val clock: Clock,
    private val mediaDir: Path,
    private val makeThumbnails: suspend (List<Path>) -> Int
) {
    /** 찾은 장 수. 이전이 끝나지 않았거나 하루가 안 지났으면 null */
    suspend fun run(db: DiaryDatabase): Int? {
        val state = store.readState()
        val now = clock.now()
        val settled = state.step == MigrationStep.DONE || state.step == MigrationStep.PARTIAL
        val due = state.photoRecheckAt?.let { now - it >= INTERVAL } ?: true
        if (!settled || !due) return null
        store.writeState(state.copy(photoRecheckAt = now, updatedAt = now))
        val found = db.mediaDao().missingWithLegacyPath().count { bringBack(db, it) }
        if (found > 0) recordFound(found)
        return found
    }

    private suspend fun bringBack(db: DiaryDatabase, row: MediaEntity): Boolean {
        val photo = row.legacyPath?.let { importer.importOne(row.entryId, row.sortOrder, it) }
        if (photo?.found != true) return false
        db.mediaDao().update(photo.media.copy(id = row.id, createdAt = Instant.fromEpochMilliseconds(row.createdAt)).asEntity())
        photo.media.fileName?.let { makeThumbnails(listOf(mediaDir / it)) }
        // 수정 중인 일기의 저장본에도 넣어 저장할 때 빠지지 않게
        photo.media.sha256?.let { db.draftDao().adoptArrived(it) }
        return true
    }

    private fun recordFound(found: Int) {
        val report = store.readReport() ?: return
        store.writeReport(
            report.copy(
                photosFound = report.photosFound + found,
                photosMissing = (report.photosMissing - found).coerceAtLeast(0)
            )
        )
    }

    private companion object {
        val INTERVAL = 24.hours
    }
}
