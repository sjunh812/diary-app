package org.sjhstudio.diary.app.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.app.navigation.BackupOptionsKey
import org.sjhstudio.diary.app.navigation.BackupSettingsKey
import org.sjhstudio.diary.app.navigation.FileBackupKey
import org.sjhstudio.diary.app.navigation.FontLicenseKey
import org.sjhstudio.diary.app.navigation.FontSettingsKey
import org.sjhstudio.diary.app.navigation.FullScreenEntry
import org.sjhstudio.diary.app.navigation.LetterKey
import org.sjhstudio.diary.app.navigation.LibraryLicenseKey
import org.sjhstudio.diary.app.navigation.LicensesKey
import org.sjhstudio.diary.app.navigation.ListStyleSettingsKey
import org.sjhstudio.diary.app.navigation.LockSettingsKey
import org.sjhstudio.diary.app.navigation.MigrationReportKey
import org.sjhstudio.diary.app.navigation.PlusKey
import org.sjhstudio.diary.app.navigation.ReminderSettingsKey
import org.sjhstudio.diary.app.navigation.RestoreListKey
import org.sjhstudio.diary.app.navigation.RestorePreviewKey
import org.sjhstudio.diary.app.navigation.RestoreProgressKey
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.TabRoot
import org.sjhstudio.diary.app.navigation.TextSizeSettingsKey
import org.sjhstudio.diary.app.navigation.ThemeSettingsKey
import org.sjhstudio.diary.app.navigation.TrashKey
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.feature.backup.BackupOptionsRoute
import org.sjhstudio.diary.feature.backup.BackupSettingsRoute
import org.sjhstudio.diary.feature.backup.FileBackupRoute
import org.sjhstudio.diary.feature.backup.RestoreListRoute
import org.sjhstudio.diary.feature.backup.RestorePreviewRoute
import org.sjhstudio.diary.feature.backup.RestoreProgressRoute
import org.sjhstudio.diary.feature.lock.LockSettingsRoute
import org.sjhstudio.diary.feature.migration.MigrationMessage
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.migration.rememberMigrationActions
import org.sjhstudio.diary.feature.plus.PlusRoute
import org.sjhstudio.diary.feature.settings.FontLicenseRoute
import org.sjhstudio.diary.feature.settings.FontSettingsRoute
import org.sjhstudio.diary.feature.settings.LibraryLicenseRoute
import org.sjhstudio.diary.feature.settings.LicensesRoute
import org.sjhstudio.diary.feature.settings.ListStyleSettingsScreen
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportViewModel
import org.sjhstudio.diary.feature.settings.ReminderSettingsRoute
import org.sjhstudio.diary.feature.settings.SettingsScreen
import org.sjhstudio.diary.feature.settings.SettingsViewModel
import org.sjhstudio.diary.feature.settings.TextSizeSettingsScreen
import org.sjhstudio.diary.feature.settings.ThemeSettingsScreen
import org.sjhstudio.diary.feature.trash.TrashRoute
import org.sjhstudio.diary.feature.welcome.FullLetterScreen
import org.sjhstudio.diary.feature.welcome.LetterViewModel
import org.sjhstudio.diary.feature.welcome.OnboardingScreen

/**
 * 설정 탭의 화면들(설정 목록 → 글꼴, 본문 글자 크기, 화면 모드, 매일 알림, 잠금, 기록 이전 결과, 오픈소스 라이선스).
 * 기능 모듈끼리는 서로 모르므로 어디서 어디로 가는지는 여기서 정한다.
 */
fun EntryProviderScope<NavKey>.settingsEntries(push: (NavKey) -> Unit, pop: () -> Unit, home: () -> Unit) {
    entry<SettingsKey>(metadata = TabRoot) {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val trash = koinInject<TrashRepository>()
        val trashCount by remember(trash) { trash.items().map { it.size } }.collectAsState(0)
        val plan by koinInject<PlanRepository>().plan.collectAsStateWithLifecycle()
        SettingsScreen(
            state,
            plusActive = plan == Plan.PLUS,
            onFont = { push(FontSettingsKey) },
            onTextSize = { push(TextSizeSettingsKey) },
            onTheme = { push(ThemeSettingsKey) },
            onReminder = { push(ReminderSettingsKey) },
            onLock = { push(LockSettingsKey) },
            onMigrationReport = { push(MigrationReportKey) },
            onLicenses = { push(LicensesKey) },
            onListStyle = { push(ListStyleSettingsKey) },
            onTrash = { push(TrashKey) },
            trashCount = trashCount,
            onBackup = { push(BackupSettingsKey) },
            onLetter = { push(LetterKey) },
            onPlus = { push(PlusKey) }
        )
    }
    entry<BackupSettingsKey> {
        BackupSettingsRoute(
            onBack = pop,
            onOptions = { push(BackupOptionsKey) },
            onRestore = { push(RestoreListKey) },
            onRestoreEntry = { push(RestorePreviewKey(it)) },
            onFile = { push(FileBackupKey) }
        )
    }
    entry<BackupOptionsKey> { BackupOptionsRoute(onBack = pop, onFile = { push(FileBackupKey) }) }
    entry<RestoreListKey> { RestoreListRoute(onBack = pop, onPick = { push(RestorePreviewKey(it)) }, onFile = { push(FileBackupKey) }) }
    entry<FileBackupKey> { FileBackupRoute(onBack = pop, onPreview = { push(RestorePreviewKey(RESTORE_FROM_FILE)) }) }
    entry<RestorePreviewKey> { key ->
        RestorePreviewRoute(key.snapshotName, onBack = pop, onMerged = {
            pop()
            push(RestoreProgressKey)
        })
    }
    entry<RestoreProgressKey> { RestoreProgressRoute(onHome = home, onLock = { push(LockSettingsKey) }, onPickOther = pop) }
    entry<TrashKey> { TrashRoute(onBack = pop) }
    entry<PlusKey>(metadata = FullScreenEntry) { PlusRoute(onClose = pop) }
    entry<LetterKey> { FullLetterScreen(entries = koinViewModel<LetterViewModel>().entries, onBack = pop) }
    entry<ListStyleSettingsKey> {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ListStyleSettingsScreen(state.settings.list.listStyle, onSelect = viewModel::setListStyle, onBack = pop)
    }
    entry<TextSizeSettingsKey> {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        TextSizeSettingsScreen(
            selected = state.settings.bodyTextSize,
            sampleDate = remember { currentDate() },
            font = state.settings.font,
            onSelect = viewModel::setBodyTextSize,
            onBack = pop
        )
    }
    entry<ThemeSettingsKey> {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ThemeSettingsScreen(state.settings.themeMode, onSelect = viewModel::setThemeMode, onBack = pop)
    }
    entry<FontSettingsKey> { FontSettingsRoute(onBack = pop) }
    entry<ReminderSettingsKey> { ReminderSettingsRoute(onBack = pop) }
    entry<LockSettingsKey> { LockSettingsRoute(onBack = pop) }
    entry<MigrationReportKey> { MigrationReportPage(onBack = pop) }
    entry<LicensesKey> {
        LicensesRoute(onFont = { push(FontLicenseKey(it.id)) }, onLibrary = { push(LibraryLicenseKey(it)) }, onBack = pop)
    }
    entry<FontLicenseKey> { key -> FontLicenseRoute(key.fontId, onBack = pop) }
    entry<LibraryLicenseKey> { key -> LibraryLicenseRoute(key.libraryId, onBack = pop) }
}

/**
 * 설정 > 기록 이전 결과.
 * 문의하기는 고쳐 받기 시트를 먼저 띄운다(FR-010a).
 */
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
            onContact = actions?.let { { asking = true } },
            onRetry = report::retry
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
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x4))
    }
}
