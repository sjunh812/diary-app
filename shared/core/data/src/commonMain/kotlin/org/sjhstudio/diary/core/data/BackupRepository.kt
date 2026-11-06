package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.ConnectResult

/**
 * 화면이 쓰는 백업 입구.
 * 홈과 설정이 같은 상태를 본다(contracts/backup-flow.md 상태 표시).
 */
interface BackupRepository {
    val status: Flow<BackupStatus>

    /**
     * 홈의 권하기 카드(FR-036).
     * 조건이 맞지 않으면 null이다.
     */
    val nudge: Flow<BackupNudge?>

    /**
     * [나중에].
     * 14일 숨기고 세 번 넘기면 다시 띄우지 않는다.
     */
    suspend fun dismissNudge()

    /**
     * 처음 연결하면 자동 백업을 켠다(FR-013).
     * 끊긴 연결을 다시 이을 때는 사용자가 정해 둔 값을 지킨다.
     * 드라이브에 백업이 없으면 바로 백업하고, 있으면 [foundBackup]으로 알리고 사용자가 고를 때까지 올리지 않는다.
     */
    suspend fun connect(hint: String? = null): ConnectResult

    /**
     * 연결했더니 드라이브에서 찾은 백업.
     * 사용자가 고르면 null이 된다.
     */
    val foundBackup: StateFlow<BackupEntry?>

    /**
     * 찾은 백업을 [나중에]로 넘겼다.
     * 이제 이 기기의 첫 백업을 한다.
     */
    fun skipFoundBackup()

    /**
     * 찾은 백업을 복원하기로 했다.
     * 복원이 끝날 때까지 백업하지 않는다.
     */
    fun takeFoundBackup()

    /** [cellularOnce]는 이번만 모바일 데이터로 사진을 올린다. */
    fun backUpNow(cellularOnce: Boolean = false)

    /**
     * 앱을 열 때 부른다.
     * 자동 백업을 켰고 마지막 성공이 24시간 넘었으면 돈다(FR-015).
     */
    suspend fun backUpIfDue()

    /**
     * 시스템이 준 시간 안에서 부른다.
     * 때가 됐으면 백업이 끝날 때까지 기다린다(iOS 백그라운드 작업).
     */
    suspend fun runIfDue()

    suspend fun setAuto(enabled: Boolean)

    suspend fun setPhotosOnCellular(enabled: Boolean)

    /** 연결을 끊어도 드라이브의 백업은 지우지 않는다(FR-004). */
    suspend fun disconnect()
}
