package org.sjhstudio.diary.core.migration

import okio.FileSystem
import okio.IOException
import okio.Path
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 옛 앱(1.7.x)의 글꼴, 글자 크기, 화면 모드, 위치 안내, 매일 알림을 새 설정으로 잇는다(스펙 001 US3, research R16).
 * 스플래시 동안 돌아 첫 화면부터 옛 화면 모드와 글꼴로 그린다.
 * 옛 파일은 읽기만 한다.
 * [LegacyLockBootstrap]처럼 이전을 시작하기 전에만 한다.
 * 그 뒤에는 새 앱에서 바꾼 설정이 기준이다.
 */
class LegacySettingsBootstrap(
    private val fileSystem: FileSystem,
    private val files: LegacyFiles,
    private val migrationStep: () -> MigrationStep,
    private val store: SettingsStore
) {
    /**
     * 설정을 옮겼으면 true다.
     * 읽거나 쓰지 못하면 기본 설정으로 두고 false다.
     */
    suspend fun run(): Boolean {
        if (migrationStep() != MigrationStep.NOT_STARTED || !fileSystem.exists(files.prefXml)) return false
        return try {
            val pref = read(files.prefXml)
            val pref2 = if (fileSystem.exists(files.pref2Xml)) read(files.pref2Xml) else emptyMap()
            store.update { LegacySettingsMapper.map(pref, pref2, base = it) }
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun read(path: Path): Map<String, Any> = LegacyPrefs.parse(fileSystem.read(path) { readUtf8() })
}
