package org.sjhstudio.diary.core.backup

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

/** 드라이브에서 권할 백업 찾기(스펙 003 US2 시나리오 7) */
class FindBackupToOfferTest {
    /** 목록을 못 읽은 것을 없음으로 보면 다른 기기 백업을 묻기 전에 이 기기 백업이 올라감 */
    @Test
    fun a_list_that_cannot_be_read_is_not_taken_as_empty() = runTest {
        assertFailsWith<DriveException> { findBackupToOffer(FakeRestoreRepository(entries = null), migrated = false) }
    }

    @Test
    fun an_empty_drive_offers_nothing() = runTest {
        assertNull(findBackupToOffer(FakeRestoreRepository(entries = emptyList()), migrated = false))
    }
}
