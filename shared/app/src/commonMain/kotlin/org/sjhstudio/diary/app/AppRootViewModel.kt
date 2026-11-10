package org.sjhstudio.diary.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.sjhstudio.diary.app.maintenance.DiaryCleanup
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.ui.launchSafely

/**
 * 이전 관문을 지난 뒤(일기 DB를 연 뒤) 한 번 하는 일.
 * 화면 효과에 두면 화면을 돌릴 때마다 다시 돌아 화면 모델로 옮김
 */
class AppRootViewModel(cleanup: DiaryCleanup, backup: BackupRepository, private val restore: RestoreRepository) : ViewModel() {
    init {
        viewModelScope.launchSafely { cleanup.run() }
        // 시스템이 자동 백업을 못 돌렸을 수 있어 앱을 열 때도 봄(FR-015). iOS에서는 이게 주된 길
        viewModelScope.launchSafely { backup.backUpIfDue() }
    }

    /** 앱으로 돌아올 때마다. 시스템이 깨워 주지 않는 iOS에서 사진 받기를 이어 가는 길 */
    fun resumed() {
        viewModelScope.launchSafely { restore.resumePhotos() }
    }
}
