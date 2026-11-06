package org.sjhstudio.diary.core.migration

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import okio.FileSystem
import okio.Path

/**
 * 복구 문의 묶음을 두는 곳과 지우는 때(FR-010a, contracts/migration-flow.md "복구 문의").
 * 캐시가 아닌 `files/migration/support`에 두고 만든 시각을 함께 적는다.
 * 메일 앱이 첨부를 언제 읽을지 알 수 없어 보낸 직후에는 지우지 않고, 하루가 지나면 지운다(헌법 II 예외).
 */
class SupportFiles(private val fileSystem: FileSystem, migrationDir: Path, private val clock: Clock) {
    val directory: Path = migrationDir / DIRECTORY
    private val stamp = directory / STAMP

    /** 남아 있던 묶음을 지우고 빈 폴더와 만든 시각을 준비한다. */
    fun prepare(): Path {
        deleteAll()
        fileSystem.createDirectories(directory)
        fileSystem.write(stamp) { writeUtf8(clock.now().toEpochMilliseconds().toString()) }
        return directory
    }

    /**
     * 만든 지 하루가 지났거나 만든 시각을 모르면 지운다.
     * 지웠으면 true.
     */
    fun deleteExpired(): Boolean {
        if (!fileSystem.exists(directory)) return false
        val created = runCatching { fileSystem.read(stamp) { readUtf8().trim().toLong() } }.getOrNull()
        if (created != null && clock.now().toEpochMilliseconds() - created < KEEP.inWholeMilliseconds) return false
        deleteAll()
        return true
    }

    fun deleteAll() {
        if (fileSystem.exists(directory)) fileSystem.deleteRecursively(directory)
    }

    private companion object {
        const val DIRECTORY = "support"
        const val STAMP = "created-at"
        val KEEP = 24.hours
    }
}
