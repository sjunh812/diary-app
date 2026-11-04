package org.sjhstudio.diary.app.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.detail_pick_hint
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeRestoreRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.feature.backup.backupFeatureModule
import org.sjhstudio.diary.feature.calendar.CalendarViewModel
import org.sjhstudio.diary.feature.home.HomeViewModel
import org.sjhstudio.diary.feature.settings.FontReplacedNoticeViewModel

private object NoAdaptiveFonts : FontInstaller {
    override fun installedPath(id: String): String? = null

    override fun install(id: String) = emptyFlow<FontInstallState>()
}

/** 창 폭이 바뀌어도 고른 탭이 그대로이고, 840dp부터 두 칸이 된다(FR-037). */
@OptIn(ExperimentalTestApi::class)
class AdaptiveLayoutUiTest {
    private val day = LocalDate(2026, 10, 31)

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                backupFeatureModule,
                module {
                    single<BackupRepository> { FakeBackupRepository() }
                    single<RestoreRepository> { FakeRestoreRepository() }
                    single { Telemetry(NoTelemetrySink, strict = true) }
                    viewModel { FontReplacedNoticeViewModel(FakeSettingsRepository()) }
                    single<EntryRepository> { FakeEntryRepository(mapOf(day to DayMood(day, null, 1))) }
                    viewModel { HomeViewModel(get(), FakeSettingsRepository(), NoAdaptiveFonts) }
                    viewModel { CalendarViewModel(get(), FakeSettingsRepository(), NoAdaptiveFonts) }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() = stopKoin()

    private val tabs = isSelectable() and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private val hint get() = runBlocking { getString(Res.string.detail_pick_hint) }

    @Test
    fun two_panes_start_at_840() = runComposeUiTest {
        var width by mutableStateOf<Dp>(839.dp)
        setContent { DiaryTheme(darkTheme = false) { Box(Modifier.size(width, 700.dp)) { AppScaffold() } } }
        waitForIdle()
        assertEquals(0, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)

        width = 840.dp
        waitForIdle()

        assertEquals(1, onAllNodes(hasText(hint)).fetchSemanticsNodes().size)
    }

    @Test
    fun selected_tab_survives_width_changes() = runComposeUiTest {
        var width by mutableStateOf<Dp>(1280.dp)
        setContent { DiaryTheme(darkTheme = false) { Box(Modifier.size(width, 700.dp)) { AppScaffold() } } }
        waitForIdle()
        onAllNodes(tabs)[1].performClick()
        waitForIdle()

        for (next in listOf(840.dp, 600.dp, 360.dp)) {
            width = next
            waitForIdle()
            onAllNodes(tabs)[1].assertIsSelected()
        }
    }
}
