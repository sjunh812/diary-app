package org.sjhstudio.diary.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.app.gate.toastBottomOverApp
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
import org.sjhstudio.diary.app.navigation.SettingsDetailPane
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.navigation.SettingsListPane
import org.sjhstudio.diary.app.navigation.TextSizeSettingsKey
import org.sjhstudio.diary.app.navigation.ThemeSettingsKey
import org.sjhstudio.diary.app.navigation.TrashKey
import org.sjhstudio.diary.app.navigation.tabRoot
import org.sjhstudio.diary.core.backup.BackupAppInfo
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.platform.DevMenu
import org.sjhstudio.diary.core.platform.deviceInfo
import org.sjhstudio.diary.core.ui.LegalLinks
import org.sjhstudio.diary.core.ui.rememberOpenLink
import org.sjhstudio.diary.core.ui.rememberOpenWebLink
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.core.ui.rememberToday
import org.sjhstudio.diary.feature.backup.BackupOptionsRoute
import org.sjhstudio.diary.feature.backup.BackupSettingsRoute
import org.sjhstudio.diary.feature.backup.FileBackupRoute
import org.sjhstudio.diary.feature.backup.RestoreListRoute
import org.sjhstudio.diary.feature.backup.RestorePreviewRoute
import org.sjhstudio.diary.feature.backup.RestoreProgressRoute
import org.sjhstudio.diary.feature.backup.RestoreStatusBar
import org.sjhstudio.diary.feature.backup.settingsBackupValue
import org.sjhstudio.diary.feature.lock.LockSettingsRoute
import org.sjhstudio.diary.feature.migration.MigrationMessage
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.migration.rememberMigrationActions
import org.sjhstudio.diary.feature.plus.PlusRoute
import org.sjhstudio.diary.feature.settings.FEATURE_SURVEY_URL
import org.sjhstudio.diary.feature.settings.FontLicenseRoute
import org.sjhstudio.diary.feature.settings.FontSettingsRoute
import org.sjhstudio.diary.feature.settings.LibraryLicenseRoute
import org.sjhstudio.diary.feature.settings.LicensesRoute
import org.sjhstudio.diary.feature.settings.ListStyleSettingsScreen
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportViewModel
import org.sjhstudio.diary.feature.settings.ReminderSettingsRoute
import org.sjhstudio.diary.feature.settings.SettingsPane
import org.sjhstudio.diary.feature.settings.SettingsScreen
import org.sjhstudio.diary.feature.settings.SettingsViewModel
import org.sjhstudio.diary.feature.settings.TextSizeSettingsScreen
import org.sjhstudio.diary.feature.settings.ThemeSettingsScreen
import org.sjhstudio.diary.feature.settings.rememberFeedbackLink
import org.sjhstudio.diary.feature.settings.storeReviewUrl
import org.sjhstudio.diary.feature.trash.TrashRoute
import org.sjhstudio.diary.feature.welcome.FullLetterScreen
import org.sjhstudio.diary.feature.welcome.LetterViewModel

/**
 * 설정 탭의 화면들(설정 목록 → 글꼴, 본문 글자 크기, 화면 모드, 매일 알림, 잠금, 기록 이전 결과, 오픈소스 라이선스)
 * 기능 모듈끼리는 서로 몰라 어디서 어디로 가는지는 여기서 정함
 */
fun EntryProviderScope<NavKey>.settingsEntries(
    push: (NavKey) -> Unit,
    pop: () -> Unit,
    home: () -> Unit,
    open: (NavKey) -> Unit = push,
    opened: () -> NavKey? = { null }
) {
    entry<SettingsKey>(metadata = SettingsListPane + tabRoot(SettingsKey)) {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val app = koinInject<BackupAppInfo>()
        val toast = remember { DiaryToastState() }
        val openWeb = rememberOpenWebLink(toast)
        val scope = rememberCoroutineScope()
        val device = remember { deviceInfo() }
        val feedback = rememberFeedbackLink(app.versionName, device.os, device.model)
        val openLink = rememberOpenLink()
        Box(Modifier.fillMaxSize()) {
            SettingsScreen(
                state,
                appVersion = app.versionName,
                onFeedback = {
                    // 메일 앱이 없는 기기에서도 보낼 곳을 알 수 있게
                    if (!openLink(feedback.url)) scope.launch { toast.show(feedback.noMail) }
                },
                onRate = storeReviewUrl(app.platform)?.let { url -> { openWeb(url) } },
                plusActive = state.plan == Plan.PLUS,
                adsRemoved = state.plan == Plan.AD_FREE,
                onDevMode = getKoin().getOrNull<DevMenu>()?.let { menu -> menu::open },
                onFont = { open(FontSettingsKey) },
                onTextSize = { open(TextSizeSettingsKey) },
                onTheme = { open(ThemeSettingsKey) },
                onReminder = { open(ReminderSettingsKey) },
                onLock = { open(LockSettingsKey) },
                onMigrationReport = { open(MigrationReportKey) },
                onLicenses = { open(LicensesKey) },
                onListStyle = { open(ListStyleSettingsKey) },
                onTrash = { open(TrashKey) },
                trashCount = state.trashCount,
                onBackup = { open(BackupSettingsKey) },
                backupValue = state.lastBackupAt?.let { settingsBackupValue(it) },
                onLetter = { push(LetterKey) },
                onTerms = { openWeb(LegalLinks.TERMS) },
                onPrivacy = { openWeb(LegalLinks.PRIVACY) },
                onPlus = { push(PlusKey) },
                selected = opened()?.let(::settingsPane)
            )
            DiaryToastHost(
                toast,
                Modifier.align(Alignment.BottomCenter).padding(bottom = toastBottomOverApp()),
                reduceMotion = rememberReduceMotion()
            )
        }
    }
    entry<BackupSettingsKey>(metadata = SettingsDetailPane) {
        Column(Modifier.fillMaxSize()) {
            BackupSettingsRoute(
                onBack = pop,
                onOptions = { push(BackupOptionsKey) },
                onRestore = { push(RestoreListKey) },
                onRestoreEntry = { push(RestorePreviewKey(it)) },
                onFileBackup = { push(FileBackupKey) },
                onFilePreview = { push(RestorePreviewKey(RESTORE_FROM_FILE)) },
                statusBar = { pickFile -> RestoreStatusBar(onPickFile = pickFile) }
            )
        }
    }
    entry<BackupOptionsKey>(metadata = SettingsDetailPane) { BackupOptionsRoute(onBack = pop) }
    entry<RestoreListKey>(metadata = SettingsDetailPane) { RestoreListRoute(onBack = pop, onPick = { push(RestorePreviewKey(it)) }) }
    entry<FileBackupKey>(metadata = SettingsDetailPane) { FileBackupRoute(onBack = pop) }
    entry<RestorePreviewKey>(metadata = SettingsDetailPane) { key ->
        RestorePreviewRoute(key.snapshotName, onBack = pop, onMerged = {
            pop()
            push(RestoreProgressKey)
        })
    }
    entry<RestoreProgressKey> { RestoreProgressRoute(onHome = home, onLock = { push(LockSettingsKey) }, onPickOther = pop) }
    entry<TrashKey>(metadata = SettingsDetailPane) { TrashRoute(onBack = pop) }
    entry<PlusKey>(metadata = FullScreenEntry) { PlusRoute(onClose = pop) }
    entry<LetterKey> {
        val letter by koinViewModel<LetterViewModel>().letter.collectAsStateWithLifecycle()
        val app = koinInject<BackupAppInfo>()
        val device = remember { deviceInfo() }
        val feedback = rememberFeedbackLink(app.versionName, device.os, device.model)
        val openLink = rememberOpenLink()
        val toast = remember { DiaryToastState() }
        val scope = rememberCoroutineScope()
        Box(Modifier.fillMaxSize()) {
            // 읽는 동안은 바탕만 그림(편지는 개수 문단까지 한 번에 나옴)
            letter?.let {
                FullLetterScreen(
                    entries = it.entries,
                    onBack = pop,
                    // 설문을 열지 못하면 메일
                    onFeedback = {
                        val opened = openLink(FEATURE_SURVEY_URL) || openLink(feedback.url)
                        if (!opened) scope.launch { toast.show(feedback.noMail) }
                    }
                )
            } ?: Box(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas))
            DiaryToastHost(
                toast,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x4),
                reduceMotion = rememberReduceMotion()
            )
        }
    }
    entry<ListStyleSettingsKey>(metadata = SettingsDetailPane) {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        // 읽기 전의 기본값을 고른 것으로 보이지 않게 기다림
        if (!state.loaded) return@entry
        ListStyleSettingsScreen(state.settings.list.listStyle, onSelect = viewModel::setListStyle, onBack = pop)
    }
    entry<TextSizeSettingsKey>(metadata = SettingsDetailPane) {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val look by viewModel.look.collectAsStateWithLifecycle()
        // 읽기 전의 기본값을 고른 것으로 보이지 않게 기다림
        if (!state.loaded) return@entry
        TextSizeSettingsScreen(
            selected = state.settings.bodyTextSize,
            sampleDate = rememberToday(),
            look = look,
            onSelect = viewModel::setBodyTextSize,
            onBack = pop
        )
    }
    entry<ThemeSettingsKey>(metadata = SettingsDetailPane) {
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        // 읽기 전의 기본값을 고른 것으로 보이지 않게 기다림
        if (!state.loaded) return@entry
        ThemeSettingsScreen(state.settings.themeMode, onSelect = viewModel::setThemeMode, onBack = pop)
    }
    entry<FontSettingsKey>(metadata = SettingsDetailPane) { FontSettingsRoute(onBack = pop) }
    entry<ReminderSettingsKey>(metadata = SettingsDetailPane) { ReminderSettingsRoute(onBack = pop) }
    entry<LockSettingsKey>(metadata = SettingsDetailPane) { LockSettingsRoute(onBack = pop) }
    entry<MigrationReportKey>(metadata = SettingsDetailPane) { MigrationReportPage(onBack = pop) }
    entry<LicensesKey>(metadata = SettingsDetailPane) {
        LicensesRoute(onFont = { push(FontLicenseKey(it.id)) }, onLibrary = { push(LibraryLicenseKey(it)) }, onBack = pop)
    }
    entry<FontLicenseKey>(metadata = SettingsDetailPane) { key -> FontLicenseRoute(key.fontId, onBack = pop) }
    entry<LibraryLicenseKey>(metadata = SettingsDetailPane) { key -> LibraryLicenseRoute(key.libraryId, onBack = pop) }
}

/**
 * 설정 > 기록 이전 결과
 * 문의하기는 복구 문의 시트를 먼저 띄움(FR-010a)
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
        DiaryToastHost(
            toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x4),
            reduceMotion = rememberReduceMotion()
        )
    }
}

/** 오른쪽에 열린 화면이 어느 설정 줄에서 왔는지 */
private fun settingsPane(key: NavKey): SettingsPane? = when (key) {
    BackupSettingsKey -> SettingsPane.BACKUP
    TrashKey -> SettingsPane.TRASH
    FontSettingsKey -> SettingsPane.FONT
    TextSizeSettingsKey -> SettingsPane.TEXT_SIZE
    ThemeSettingsKey -> SettingsPane.THEME
    ListStyleSettingsKey -> SettingsPane.LIST_STYLE
    ReminderSettingsKey -> SettingsPane.REMINDER
    LockSettingsKey -> SettingsPane.LOCK
    LicensesKey -> SettingsPane.LICENSES
    MigrationReportKey -> SettingsPane.MIGRATION_REPORT
    else -> null
}
