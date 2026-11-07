package org.sjhstudio.diary.feature.backup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState

class RestoreResultWatchTest {
    private val done = RestoreState.Done(RestoreResult(1, 0, 0, 0, 0, 0))
    private val failed = RestoreState.Failed(RestoreFailure.OTHER)

    @Test
    fun a_result_that_comes_after_leaving_the_screen_is_told_once() {
        val watch = RestoreResultWatch()

        assertTrue(watch.shouldTell(done, onResultScreen = false, quiet = false))
        assertFalse(watch.shouldTell(done, onResultScreen = false, quiet = false))
    }

    @Test
    fun a_failure_is_told_too() {
        assertTrue(RestoreResultWatch().shouldTell(failed, onResultScreen = false, quiet = false))
    }

    @Test
    fun a_result_seen_on_its_own_screen_is_not_told_again() {
        val watch = RestoreResultWatch()

        assertFalse(watch.shouldTell(failed, onResultScreen = true, quiet = false))
        assertFalse(watch.shouldTell(failed, onResultScreen = false, quiet = false))
    }

    @Test
    fun a_result_waits_until_writing_ends() {
        val watch = RestoreResultWatch()

        assertFalse(watch.shouldTell(done, onResultScreen = false, quiet = true))
        assertTrue(watch.shouldTell(done, onResultScreen = false, quiet = false))
    }

    @Test
    fun a_running_restore_is_not_a_result() {
        val watch = RestoreResultWatch()

        assertFalse(watch.shouldTell(RestoreState.Merging, onResultScreen = false, quiet = false))
        assertFalse(watch.shouldTell(RestoreState.WaitingForBackup, onResultScreen = false, quiet = false))
        assertFalse(watch.shouldTell(RestoreState.FetchingPhotos(1, 0, 1), onResultScreen = false, quiet = false))
        assertFalse(watch.shouldTell(RestoreState.Idle, onResultScreen = false, quiet = false))
    }

    @Test
    fun a_second_restore_is_told_as_well() {
        val watch = RestoreResultWatch()
        watch.shouldTell(done, onResultScreen = false, quiet = false)

        assertTrue(watch.shouldTell(RestoreState.Done(done.result.copy(entriesAdded = 2)), onResultScreen = false, quiet = false))
    }
}
