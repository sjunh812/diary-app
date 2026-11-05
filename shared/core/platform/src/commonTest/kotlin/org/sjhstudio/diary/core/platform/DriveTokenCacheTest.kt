package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.BackupFailure

class DriveTokenCacheTest {
    private val clock = TestTimeSource()
    private val mine = DriveAccount("sub-1", "diary@gmail.com")
    private val other = DriveAccount("sub-2", "other@gmail.com")
    private var fetches = 0

    private fun cache(answer: suspend () -> TokenResult = { TokenResult.Token("token", mine) }) = DriveTokenCache(clock) {
        fetches++
        answer()
    }

    @Test
    fun six_photos_at_once_ask_only_once() = runTest {
        val cache =
            cache {
                delay(100)
                TokenResult.Token("token", mine)
            }

        val tokens = List(6) { async { cache.get() } }.awaitAll()

        assertEquals(1, fetches)
        assertEquals(6, tokens.count { it == TokenResult.Token("token", mine) })
    }

    @Test
    fun a_token_is_used_again_for_five_minutes() = runTest {
        val cache = cache()

        cache.get()
        clock += 4.minutes
        cache.get()
        assertEquals(1, fetches)

        clock += 2.minutes
        cache.get()
        assertEquals(2, fetches)
    }

    @Test
    fun a_rejected_token_is_not_used_again() = runTest {
        val cache = cache()
        cache.get()

        cache.clear()
        cache.get()

        assertEquals(2, fetches)
    }

    @Test
    fun a_failure_is_not_kept() = runTest {
        var answer: TokenResult = TokenResult.Failed(BackupFailure.OFFLINE)
        val cache = cache { answer }

        assertIs<TokenResult.Failed>(cache.get())
        answer = TokenResult.Token("token", mine)

        assertIs<TokenResult.Token>(cache.get())
    }

    @Test
    fun a_late_answer_for_the_old_account_is_neither_kept_nor_handed_out() = runTest {
        val slow = CompletableDeferred<TokenResult>()
        var first = true
        val cache =
            cache {
                if (first) {
                    first = false
                    slow.await()
                } else {
                    TokenResult.Token("new", other)
                }
            }
        val waiting = async { cache.get() }
        runCurrent()

        cache.clear()
        slow.complete(TokenResult.Token("old", mine))

        assertEquals(TokenResult.Token("new", other), waiting.await())
        assertEquals(TokenResult.Token("new", other), cache.get())
    }

    @Test
    fun an_answer_that_never_comes_does_not_hold_everyone_back() = runTest {
        var hang = true
        val cache =
            cache {
                if (hang) awaitCancellation() else TokenResult.Token("token", mine)
            }
        val stuck = async { cache.get() }
        val behind = async { cache.get() }
        runCurrent()
        hang = false

        advanceTimeBy(61.seconds)

        assertEquals(TokenResult.Failed(BackupFailure.OFFLINE), stuck.await())
        assertEquals(TokenResult.Token("token", mine), behind.await())
    }
}
