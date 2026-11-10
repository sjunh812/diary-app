package org.sjhstudio.diary.app.maintenance

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import org.sjhstudio.diary.core.data.diary.MediaRepository
import org.sjhstudio.diary.core.data.diary.TrashRepository

/**
 * 이전 관문을 지난 뒤 앱을 열 때마다 한 번 하는 정리(FR-035)
 * 30일 지난 휴지통을 먼저 비워야 그 일기에만 쓰인 사진 파일까지 함께 지워짐
 * 실패해도 앱은 쓸 수 있고 다음에 열 때 다시 함
 */
class DiaryCleanup(
    private val trash: TrashRepository,
    private val media: MediaRepository,
    private val clock: Clock = Clock.System,
    /** 옮겨 둔 사진 사본을 아직 쓸 수 있으면 사진 파일을 지우지 않음. 사본은 일기 줄이 없어 쓰지 않는 파일처럼 보임 */
    private val keepPhotos: suspend () -> Boolean = { false }
) {
    suspend fun run() {
        val now = clock.now()
        try {
            trash.purgeExpired(now)
            if (!keepPhotos()) media.cleanUnused(now)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }
}
