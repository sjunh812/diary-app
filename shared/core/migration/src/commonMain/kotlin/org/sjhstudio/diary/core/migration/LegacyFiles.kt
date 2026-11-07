package org.sjhstudio.diary.core.migration

import okio.FileSystem
import okio.Path

/**
 * 옛 앱 원본이 놓인 자리.
 * Android `MigrationModule`이 `Context`로 채운다.
 * 이 파일들에는 쓰지도 열지도 않는다.
 * 사본을 만들 때 바이트로 읽기만 한다(docs/migration.md 원칙 1, 2).
 */
data class LegacyFiles(val noteDb: Path, val prefXml: Path, val pref2Xml: Path) {
    val noteDbWal: Path get() = noteDb.parent!! / "${noteDb.name}-wal"
    val noteDbShm: Path get() = noteDb.parent!! / "${noteDb.name}-shm"

    /**
     * 옛 앱을 쓴 기기인가(contracts/migration-flow.md 시작할 때).
     * 둘 다 없으면 새로 설치한 것이다.
     */
    fun exist(fileSystem: FileSystem): Boolean = fileSystem.exists(noteDb) || fileSystem.exists(prefXml)
}
