package org.sjhstudio.diary.core.platform

import kotlin.time.Instant

/** 지금 네트워크. 사진은 데이터 요금이 없는 연결에서만 올림(FR-014) */
interface NetworkState {
    val isConnected: Boolean
    val isUnmetered: Boolean
}

/** 기기 이름. 백업 파일 이름과 복원 목록에 보임 */
fun interface DeviceName {
    fun current(): String
}

/** 앱 저장소의 남은 공간(FR-025) */
fun interface FreeSpace {
    fun bytes(): Long
}

/** 백업 알림. 일기 내용, 사진, 개수를 넣지 않음(FR-016) */
interface BackupNotifier {
    /**
     * 백업이 없는 채로 [at]이 되면 한 번 알림(FR-016).
     * 앱이 돌지 못해도 와야 해서 미리 예약하고, 다시 부르면 앞의 예약을 바꿈.
     * 일기 내용과 개수는 넣지 않음.
     */
    suspend fun schedule(at: Instant, title: String, body: String)

    /** 지금 한 번 알림. 예약해 둔 알림은 그대로 둠 */
    fun show(title: String, body: String)

    fun cancel()
}

/** 백업을 언제 돌릴지 시스템에 맡김. 실제 작업은 플랫폼이 공통 코드를 불러 함(research R14, R15) */
interface BackupScheduler {
    fun scheduleDaily()

    fun cancelDaily()

    /**
     * 사용자가 누른 지금 백업을 시스템 작업으로 넘김. 넘겼으면 true, 아니면 앱이 직접 돌림.
     * 앞선 요청이 재시도를 기다리고 있으면 이 요청이 대신함.
     */
    fun startNow(cellularOnce: Boolean): Boolean

    /** 모바일 데이터라 못 올린 사진을 요금 없는 연결이 되면 이어 올림 */
    fun scheduleWifiPhotos()

    /** 복원 사진을 받는 동안 앱을 나가도 멈추지 않게 시스템에 알림. 시스템이 끝까지 지켜 주지 않는 플랫폼은 안 함 */
    fun keepRestoreRunning() = Unit

    /** 복원 사진을 받을 네트워크가 돌아오면 앱을 깨워 잇게 함. 시스템이 해 주지 않는 플랫폼은 앱을 다시 열 때 이음 */
    fun waitForPhotoNetwork(wifiOnly: Boolean) = Unit
}

/** 시스템 예약이 없는 플랫폼. iOS는 앱을 열 때 밀린 백업을 돌림(T044에서 BGTask 추가) */
object NoBackupScheduler : BackupScheduler {
    override fun scheduleDaily() = Unit

    override fun cancelDaily() = Unit

    override fun startNow(cellularOnce: Boolean) = false

    override fun scheduleWifiPhotos() = Unit
}
