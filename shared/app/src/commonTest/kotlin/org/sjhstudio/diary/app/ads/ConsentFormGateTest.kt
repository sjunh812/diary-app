package org.sjhstudio.diary.app.ads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

// 동의 창은 목록 광고 자리가 화면에 있을 때만 뜸
class ConsentFormGateTest {
    private var needed = true
    private val closes = mutableListOf<(Boolean) -> Unit>()
    private var loads = 0
    private val gate = ConsentFormGate(needed = { needed }, load = { loads++ }, present = { onClosed -> closes += onClosed })

    @Test
    fun a_form_ready_after_the_slot_left_the_screen_is_not_shown() {
        gate.ask("home-5")
        gate.stopAsking("home-5")
        gate.formReady()

        assertTrue(closes.isEmpty())
    }

    @Test
    fun the_ready_form_is_shown_when_a_slot_comes_back() {
        gate.formReady()
        assertTrue(closes.isEmpty())

        gate.ask("home-5")

        assertEquals(1, closes.size)
    }

    @Test
    fun two_slots_on_screen_show_one_form() {
        gate.ask("home-5")
        gate.ask("insight-3")
        gate.formReady()
        gate.ask("home-15")

        assertEquals(1, closes.size)
    }

    @Test
    fun a_form_that_could_not_be_shown_waits_for_the_next_slot() {
        gate.formReady()
        gate.ask("home-5")
        closes.single()(false)

        gate.ask("home-15")

        assertEquals(2, closes.size)
    }

    @Test
    fun an_answered_form_is_not_shown_again() {
        gate.formReady()
        gate.ask("home-5")
        closes.single()(true)

        gate.stopAsking("home-5")
        gate.ask("home-5")

        assertEquals(1, closes.size)
    }

    @Test
    fun a_tap_asks_at_once_and_continues_after_the_answer() = runTest {
        gate.formReady()
        val tap = launch(start = CoroutineStart.UNDISPATCHED) { gate.answer() }
        gate.ask(SHEET)
        assertTrue(tap.isActive)

        closes.single()(true)
        runCurrent()

        assertTrue(tap.isCompleted)
    }

    @Test
    fun a_tap_continues_at_once_when_nothing_needs_asking() = runTest {
        needed = false

        gate.answer()

        assertTrue(closes.isEmpty())
    }

    @Test
    fun a_tap_continues_when_the_form_failed_to_load() = runTest {
        val tap = launch(start = CoroutineStart.UNDISPATCHED) { gate.answer() }

        gate.formFailed()
        runCurrent()

        assertTrue(tap.isCompleted)
    }

    @Test
    fun a_tap_after_the_form_failed_loads_it_again() = runTest {
        gate.prepare()
        gate.formFailed()
        val tap = launch(start = CoroutineStart.UNDISPATCHED) { gate.answer() }

        assertEquals(2, loads)
        gate.ask(SHEET)
        gate.formReady()
        closes.single()(true)
        runCurrent()

        assertTrue(tap.isCompleted)
    }

    @Test
    fun a_form_being_loaded_is_not_loaded_twice() = runTest {
        gate.prepare()
        launch(start = CoroutineStart.UNDISPATCHED) { gate.answer() }

        assertEquals(1, loads)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_cancelled_tap_does_not_show_the_form_later() = runTest {
        gate.ask(SHEET)
        val tap = launch(start = CoroutineStart.UNDISPATCHED) { gate.answer() }
        tap.cancel()
        gate.stopAsking(SHEET)

        gate.formReady()

        assertTrue(closes.isEmpty())
    }

    @Test
    fun a_form_missed_in_the_background_shows_when_the_sheet_asks_again() = runTest {
        gate.formReady()
        gate.ask(SHEET)
        val tap = launch(start = CoroutineStart.UNDISPATCHED) { gate.answer() }
        closes.single()(false)
        gate.stopAsking(SHEET)

        gate.ask(SHEET)
        closes.last()(true)
        runCurrent()

        assertEquals(2, closes.size)
        assertTrue(tap.isCompleted)
    }

    @Test
    fun a_form_answered_elsewhere_is_not_shown() {
        gate.formReady()
        needed = false

        gate.ask("home-5")

        assertTrue(closes.isEmpty())
    }

    private companion object {
        const val SHEET = "reward"
    }
}
