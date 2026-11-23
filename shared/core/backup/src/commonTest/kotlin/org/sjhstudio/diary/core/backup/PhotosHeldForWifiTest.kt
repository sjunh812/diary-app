package org.sjhstudio.diary.core.backup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.datastore.BackupPrefs

/** 백업 화면이 남은 사진을 Wi-Fi 기다림으로 말할 때 */
class PhotosHeldForWifiTest {
    @Test
    fun photos_the_last_backup_held_for_wifi_wait_for_wifi() {
        assertTrue(BackupPrefs(photosNotBackedUp = 3, photosWaitingForWifi = true).photosHeldForWifi())
    }

    @Test
    fun turning_on_mobile_data_stops_waiting_for_wifi() {
        assertFalse(BackupPrefs(photosNotBackedUp = 3, photosWaitingForWifi = true, photosOnCellular = true).photosHeldForWifi())
    }

    /** 공간 부족이나 아직 받지 않은 복원 사진으로 남은 것은 Wi-Fi 기다림이 아님 */
    @Test
    fun photos_left_for_other_reasons_do_not_wait_for_wifi() {
        assertFalse(BackupPrefs(photosNotBackedUp = 3, bytesShort = 1_000L).photosHeldForWifi())
    }
}
