package org.sjhstudio.diary.core.platform

import kotlin.time.Instant

/** 지금 네트워크. 사진은 데이터 요금이 없는 연결에서만 올린다(FR-014). */
interface NetworkState {
    val isConnected: Boolean
    val isUnmetered: Boolean
}

/** 기기 이름. 백업 파일 이름과 복원 목록에 보인다. */
fun interface DeviceName {
    fun current(): String
}

/** 앱 저장소의 남은 공간(FR-025). */
fun interface FreeSpace {
    fun bytes(): Long
}

/** 백업 알림. 일기 내용, 사진, 개수를 넣지 않는다(FR-016). */
interface BackupNotifier {
    /**
     * 백업이 없는 채로 [at]이 되면 한 번 알린다(FR-016). 앱이 돌지 못해도 와야 해서 미리 예약한다.
     * 다시 부르면 앞의 예약을 바꾼다. 일기 내용과 개수는 넣지 않는다.
     */
    fun schedule(at: Instant, title: String, body: String)

    fun cancel()
}

/** 백업을 언제 돌릴지 시스템에 맡긴다. 실제 작업은 플랫폼이 공통 코드를 부른다(research R14, R15). */
interface BackupScheduler {
    fun scheduleDaily()

    fun cancelDaily()

    /** 사용자가 누른 지금 백업을 시스템 작업으로 넘긴다. 넘겼으면 true이고, 아니면 앱이 직접 돌린다. */
    fun startNow(cellularOnce: Boolean): Boolean

    /** 모바일 데이터라 못 올린 사진을 요금 없는 연결이 되면 잇는다. */
    fun scheduleWifiPhotos()

    /** 복원 사진을 받는 동안 앱을 나가도 멈추지 않게 시스템에 알린다. 시스템이 끝까지 지켜 주지 않는 플랫폼은 하지 않는다. */
    fun keepRestoreRunning() = Unit
}

/** 시스템 예약이 없는 플랫폼. iOS는 앱을 열 때 밀린 백업을 돌린다(T044에서 BGTask를 더한다). */
object NoBackupScheduler : BackupScheduler {
    override fun scheduleDaily() = Unit

    override fun cancelDaily() = Unit

    override fun startNow(cellularOnce: Boolean) = false

    override fun scheduleWifiPhotos() = Unit
}
