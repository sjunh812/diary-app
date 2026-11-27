package org.sjhstudio.diary.app.ads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 동의 창은 목록 광고 자리가 화면에 있을 때만 뜸
class ConsentFormGateTest {
    private var needed = true
    private val closes = mutableListOf<(Boolean) -> Unit>()
    private val gate = ConsentFormGate(needed = { needed }, present = { onClosed -> closes += onClosed })

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
    fun a_tap_asks_at_once_and_continues_after_the_answer() {
        var continued = 0
        gate.formReady()
        gate.askNow { continued++ }
        assertEquals(0, continued)

        closes.single()(true)

        assertEquals(1, continued)
    }

    @Test
    fun a_tap_continues_at_once_when_nothing_needs_asking() {
        needed = false
        var continued = 0

        gate.askNow { continued++ }

        assertEquals(1, continued)
        assertTrue(closes.isEmpty())
    }

    @Test
    fun a_tap_continues_when_the_form_failed_to_load() {
        var continued = 0
        gate.askNow { continued++ }

        gate.formFailed()

        assertEquals(1, continued)
    }

    @Test
    fun a_form_answered_elsewhere_is_not_shown() {
        gate.formReady()
        needed = false

        gate.ask("home-5")

        assertTrue(closes.isEmpty())
    }
}
