package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestorePreviewTest {
    private val nothing =
        RestorePreview(
            entriesAdded = 0,
            entriesUpdated = 0,
            entriesKeptBoth = 0,
            entriesUnchanged = 5,
            photosToDownload = 0,
            bytesToDownload = 0,
            photosNotInBackup = 0,
            settingsChange = true,
            deviceBytesFree = 0
        )

    @Test
    fun only_settings_bring_nothing() = assertFalse(nothing.bringsSomething())

    /** 휴지통 일기만 새로 있어도 "이미 모두 있어요"로 넘기지 않음. 영구 삭제한 일기를 되찾는 길 */
    @Test
    fun trashed_entries_alone_bring_something() = assertTrue(nothing.copy(entriesToTrash = 1).bringsSomething())

    @Test
    fun photos_alone_bring_something() = assertTrue(nothing.copy(photosToDownload = 1).bringsSomething())
}
