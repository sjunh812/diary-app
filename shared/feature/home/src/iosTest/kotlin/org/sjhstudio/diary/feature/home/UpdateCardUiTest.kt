package org.sjhstudio.diary.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ReleaseNote
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.update_card_default
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_title
import org.sjhstudio.diary.feature.home.resources.update_card_failed
import org.sjhstudio.diary.feature.home.resources.update_card_keep_required
import org.sjhstudio.diary.feature.home.resources.update_card_later
import org.sjhstudio.diary.feature.home.resources.update_card_restart
import org.sjhstudio.diary.feature.home.resources.update_card_title_required
import org.sjhstudio.diary.feature.home.resources.update_card_update

// 새 버전 카드: 상태마다 맞는 글과 버튼이 보이고, 버튼이 각자 할 일을 부르는지(스펙 009)
@OptIn(ExperimentalTestApi::class)
class UpdateCardUiTest {
    private val progressBar = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    @Test
    fun available_card_updates_or_later() = runComposeUiTest {
        var updates = 0
        var laters = 0
        var update = ""
        var later = ""
        var fallback = ""
        setContent {
            update = stringResource(Res.string.update_card_update)
            later = stringResource(Res.string.update_card_later)
            fallback = stringResource(Res.string.update_card_default)
            DiaryTheme(darkTheme = false) {
                UpdateCard(UpdateNotice.Available(required = false, note = null), onUpdate = {
                    updates++
                }, onLater = { laters++ }, onRestart = {})
            }
        }
        // 일반 카드는 요약 한 줄뿐. 덧붙는 줄이 없음
        onNode(hasText(fallback)).assertExists()
        onNode(hasText(update)).performClick()
        onNode(hasText(later)).performClick()
        assertEquals(1, updates)
        assertEquals(1, laters)
    }

    @Test
    fun note_replaces_the_default_line() = runComposeUiTest {
        val note = ReleaseNote(mapOf("en" to "Fixed ads showing after purchase.", "ko" to "결제 뒤에도 광고가 보이던 문제를 고쳤어요."))
        setContent {
            DiaryTheme(darkTheme = false) {
                UpdateCard(UpdateNotice.Available(required = false, note = note), onUpdate = {}, onLater = {}, onRestart = {})
            }
        }
        onNode(
            hasText("Fixed ads showing after purchase.", substring = true) or hasText("결제 뒤에도 광고가 보이던 문제를 고쳤어요.", substring = true)
        ).assertExists()
    }

    @Test
    fun required_card_has_no_later() = runComposeUiTest {
        var title = ""
        var later = ""
        var ask = ""
        setContent {
            title = stringResource(Res.string.update_card_title_required)
            later = stringResource(Res.string.update_card_later)
            ask = stringResource(Res.string.update_card_keep_required)
            DiaryTheme(darkTheme = false) {
                UpdateCard(UpdateNotice.Available(required = true, note = null), onUpdate = {}, onLater = {}, onRestart = {})
            }
        }
        onNode(hasText(title)).assertExists()
        onNode(hasText(ask, substring = true)).assertExists()
        onNode(hasText(later)).assertDoesNotExist()
    }

    @Test
    fun required_downloaded_card_has_no_later() = runComposeUiTest {
        var restart = ""
        var later = ""
        setContent {
            restart = stringResource(Res.string.update_card_restart)
            later = stringResource(Res.string.update_card_later)
            DiaryTheme(darkTheme = false) {
                UpdateCard(UpdateNotice.Downloaded(required = true), onUpdate = {}, onLater = {}, onRestart = {})
            }
        }
        onNode(hasText(restart)).assertExists()
        onNode(hasText(later)).assertDoesNotExist()
    }

    @Test
    fun download_shows_progress_then_restart() = runComposeUiTest {
        var notice: UpdateNotice by mutableStateOf(UpdateNotice.Downloading(0.42f))
        var restarts = 0
        var downloading = ""
        var update = ""
        var restart = ""
        setContent {
            downloading = stringResource(Res.string.update_card_downloading_title)
            update = stringResource(Res.string.update_card_update)
            restart = stringResource(Res.string.update_card_restart)
            DiaryTheme(darkTheme = false) { UpdateCard(notice, onUpdate = {}, onLater = {}, onRestart = { restarts++ }) }
        }
        onNode(hasText(downloading)).assertExists()
        onNode(progressBar).assertExists()
        onNode(hasText("42%", substring = true)).assertExists()
        // 받는 동안에는 누를 버튼이 없음
        onNode(hasText(update)).assertDoesNotExist()

        notice = UpdateNotice.Downloaded()
        onNode(progressBar).assertDoesNotExist()
        onNode(hasText(restart)).performClick()
        assertEquals(1, restarts)
    }

    @Test
    fun failed_download_says_so_and_offers_update_again() = runComposeUiTest {
        var failed = ""
        var update = ""
        setContent {
            failed = stringResource(Res.string.update_card_failed)
            update = stringResource(Res.string.update_card_update)
            DiaryTheme(darkTheme = false) {
                UpdateCard(UpdateNotice.Available(required = false, note = null, failed = true), onUpdate = {
                }, onLater = {}, onRestart = {})
            }
        }
        onNode(hasText(failed)).assertExists()
        onNode(hasText(update)).assertExists()
    }
}
