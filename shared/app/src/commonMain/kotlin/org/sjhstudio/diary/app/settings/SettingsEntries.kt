package org.sjhstudio.diary.app.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.app.navigation.LockSettingsKey
import org.sjhstudio.diary.app.navigation.MigrationReportKey
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.TextSizeSettingsKey
import org.sjhstudio.diary.app.navigation.ThemeSettingsKey
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.feature.lock.LockSettingsRoute
import org.sjhstudio.diary.feature.migration.MigrationMessage
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.migration.rememberMigrationActions
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportViewModel
import org.sjhstudio.diary.feature.settings.SettingsScreen
import org.sjhstudio.diary.feature.settings.SettingsViewModel
import org.sjhstudio.diary.feature.settings.TextSizeSettingsScreen
import org.sjhstudio.diary.feature.settings.ThemeSettingsScreen

/**
 * 설정 탭의 화면들(설정 목록 → 본문 글자 크기, 화면 모드, 잠금, 기록 이전 결과).
 * 기능 모듈끼리는 서로 모르므로 어디서 어디로 가는지는 여기서 정한다.
 */
fun EntryProviderScope<NavKey>.settingsEntries(push: (NavKey) -> Unit, pop: () -> Unit) {
    entry<SettingsKey> {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(
            state,
            onTextSize = { push(TextSizeSettingsKey) },
            onTheme = { push(ThemeSettingsKey) },
            onLock = { push(LockSettingsKey) },
            onMigrationReport = { push(MigrationReportKey) }
        )
    }
    entry<TextSizeSettingsKey> {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        TextSizeSettingsScreen(
            selected = state.settings.bodyTextSize,
            sampleDate = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date },
            onSelect = viewModel::setBodyTextSize,
            onBack = pop
        )
    }
    entry<ThemeSettingsKey> {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ThemeSettingsScreen(state.settings.themeMode, onSelect = viewModel::setThemeMode, onBack = pop)
    }
    entry<LockSettingsKey> { LockSettingsRoute(onBack = pop) }
    entry<MigrationReportKey> { MigrationReportPage(onBack = pop) }
}

/** 설정 > 기록 이전 결과. 문의하기는 고쳐 받기 시트를 먼저 띄운다(FR-010a). */
@Composable
private fun MigrationReportPage(onBack: () -> Unit) {
    val report = koinViewModel<MigrationReportViewModel>()
    val reportState by report.state.collectAsStateWithLifecycle()
    val toast = remember { DiaryToastState() }
    val scope = rememberCoroutineScope()
    val actions = rememberMigrationActions(MigrationMessage { message -> scope.launch { toast.show(message) } })
    var asking by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        MigrationReportScreen(
            state = reportState,
            onBack = onBack,
            onExportOriginal = actions?.exportOriginal,
            onContact = actions?.let { { asking = true } }
        )
        if (asking && actions != null) {
            SupportAttachSheet(
                onDismiss = { asking = false },
                onContactWithout = {
                    asking = false
                    actions.contact()
                },
                onContactWith = {
                    asking = false
                    actions.contactWithAttachment()
                }
            )
        }
        DiaryToastHost(
            toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(
                bottom =
                    DiarySpacing.x14 + DiarySpacing.x10
            )
        )
    }
}
