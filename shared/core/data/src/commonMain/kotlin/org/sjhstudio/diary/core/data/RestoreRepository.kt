package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.StateFlow
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState

/** 화면이 쓰는 되살리기 입구. 일기 내용은 여기를 지나지 않고 숫자만 지나간다. */
interface RestoreRepository {
    val state: StateFlow<RestoreState>

    /** 드라이브의 백업 목록. 읽지 못하면 null이다. */
    suspend fun backups(): List<BackupEntry>?

    /** 스냅샷을 받아 확인하고 쓰기 없이 결과 숫자를 낸다(FR-021, FR-023). */
    suspend fun prepare(snapshotName: String): RestorePreparation

    /** 확인한 백업을 합치고 사진을 받는다. 지금 기기의 일기는 지우지 않는다(FR-022). */
    fun merge()

    /** 결과 화면을 닫을 때 부른다. */
    fun dismiss()

    /** 앱을 열 때 부른다. 전에 다 못 받은 사진을 이어 받는다(FR-024). */
    suspend fun resumePhotos()
}
