package org.sjhstudio.diary.core.migration

import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.IOException
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.PreviewLength

/**
 * 옛 앱(1.7.4)의 "일기 3줄 보기"를 끈 사람은 목록 전체 보기로 이음(스펙 008 US3)
 * 이미 이전을 마친 기기도 한 번 판단함. 옛 파일은 읽기만 함
 * 이 기기에서 길이를 직접 고른 뒤에는 판단하지 않음
 */
class LegacyListPreview(
    private val fileSystem: FileSystem,
    private val files: LegacyFiles,
    private val store: SettingsStore,
    /** 이었다고 한 번 알릴 팁을 엶 */
    private val onKept: () -> Unit
) {
    /** 전체 보기로 이었으면 true */
    suspend fun run(): Boolean {
        // 이미 판단했으면 null. 있는데 읽지 못했어도 null이라 다음 실행에 다시 봄
        val off = (if (store.settings.first().list.legacyChecked) null else skipNoteOff()) ?: return false
        var kept = false
        // 읽기가 한 번 실패해 기본값을 받았어도 쓰는 순간의 값으로 판단
        store.update {
            if (it.list.legacyChecked) return@update it
            kept = off && !it.list.previewChosen
            val next = if (kept) it.list.copy(previewLength = PreviewLength.FULL) else it.list
            it.copy(list = next.copy(legacyChecked = true))
        }
        if (kept) onKept()
        return kept
    }

    // 파일이 없으면 3줄 그대로(옛 기본값), 읽지 못하면 null
    private fun skipNoteOff(): Boolean? {
        if (!fileSystem.exists(files.prefXml)) return false
        return try {
            LegacyPrefs.parse(fileSystem.read(files.prefXml) { readUtf8() })[SKIP_NOTE] == false
        } catch (_: IOException) {
            null
        }
    }

    private companion object {
        const val SKIP_NOTE = "skip_note"
    }
}
