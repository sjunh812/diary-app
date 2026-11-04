package org.sjhstudio.diary.app.maintenance

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import org.sjhstudio.diary.core.data.diary.MediaRepository
import org.sjhstudio.diary.core.data.diary.TrashRepository

/**
 * 이전 관문을 지난 뒤 앱을 열 때마다 한 번 하는 정리(FR-035).
 * 30일 지난 휴지통을 먼저 비워야 그 일기에만 쓰인 사진 파일까지 함께 지워진다.
 * 실패해도 앱은 쓸 수 있고 다음에 열 때 다시 한다.
 */
class DiaryCleanup(private val trash: TrashRepository, private val media: MediaRepository, private val clock: Clock = Clock.System) {
    suspend fun run() {
        val now = clock.now()
        try {
            trash.purgeExpired(now)
            media.cleanUnused(now)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }
}
