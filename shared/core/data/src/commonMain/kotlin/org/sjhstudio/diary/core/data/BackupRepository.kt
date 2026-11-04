package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.Flow
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.ConnectResult

/** 화면이 쓰는 백업 입구. 홈과 설정이 같은 상태를 본다(contracts/backup-flow.md 상태 표시). */
interface BackupRepository {
    val status: Flow<BackupStatus>

    /** 처음 연결하면 자동 백업을 켜고 바로 백업한다(FR-013). */
    suspend fun connect(hint: String? = null): ConnectResult

    /** [cellularOnce]는 이번만 모바일 데이터로 사진을 올린다. */
    fun backUpNow(cellularOnce: Boolean = false)

    /** 앱을 열 때 부른다. 자동 백업을 켰고 마지막 성공이 24시간 넘었으면 돈다(FR-015). */
    suspend fun backUpIfDue()

    /** 시스템이 준 시간 안에서 부른다. 때가 됐으면 백업이 끝날 때까지 기다린다(iOS 백그라운드 작업). */
    suspend fun runIfDue()

    suspend fun setAuto(enabled: Boolean)

    suspend fun setPhotosOnCellular(enabled: Boolean)

    /** 연결을 끊어도 드라이브의 백업은 지우지 않는다(FR-004). */
    suspend fun disconnect()
}
