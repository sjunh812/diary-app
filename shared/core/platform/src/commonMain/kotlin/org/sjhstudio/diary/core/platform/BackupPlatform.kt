package org.sjhstudio.diary.core.platform

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
    /** 7일 넘게 백업하지 못했을 때 한 번 보낸다. */
    fun notifyLongFailure(title: String, body: String)

    fun clear()
}

/** 백업을 언제 돌릴지 시스템에 맡긴다. 실제 작업은 플랫폼이 공통 코드를 부른다(research R14, R15). */
interface BackupScheduler {
    fun scheduleDaily()

    fun cancelDaily()

    /** 사용자가 누른 지금 백업. */
    fun startNow()
}

/** 시스템 예약이 없는 플랫폼. iOS는 앱을 열 때 밀린 백업을 돌린다(T044에서 BGTask를 더한다). */
object NoBackupScheduler : BackupScheduler {
    override fun scheduleDaily() = Unit

    override fun cancelDaily() = Unit

    override fun startNow() = Unit
}
