package org.sjhstudio.diary.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import org.koin.core.context.GlobalContext
import org.sjhstudio.diary.app.navigation.SettingsKey
import org.sjhstudio.diary.app.scaffold.AppScaffold
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.designsystem.catalog.DesignCatalogScreen
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.component.ProvideScreenGutter
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.ProvideDiaryImageLoader
import org.sjhstudio.diary.feature.backup.BackupFoundSheet
import org.sjhstudio.diary.feature.backup.RestoreListScreen
import org.sjhstudio.diary.feature.backup.RestoreListUi
import org.sjhstudio.diary.feature.backup.RestorePreviewScreen
import org.sjhstudio.diary.feature.backup.RestoreProgressScreen
import org.sjhstudio.diary.feature.lock.ForgotPinSheet
import org.sjhstudio.diary.feature.lock.LockScreen
import org.sjhstudio.diary.feature.lock.LockSettingsRoute
import org.sjhstudio.diary.feature.lock.LockUiState
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportUiState
import org.sjhstudio.diary.feature.welcome.FullLetterScreen
import org.sjhstudio.diary.feature.welcome.LetterScreen

/**
 * 개발 빌드에만 있는 화면 모음.
 * 앱 틀에서 아직 들어갈 길이 없는 화면을 견본 상태로 띄워 Figma와 견준다.
 * 일기 DB나 설정은 건드리지 않는다.
 *
 *   adb shell am start -n org.sjhstudio.diary/.debug.ScreenCatalogActivity --es screen migration_report
 *   화면 이름: migration_report(M5), support_sheet(M5 · 복구 문의), photo_cell(일기 줄 사진 칸, 옮긴 사진으로),
 *   lock_forgot(L3 · 비밀번호를 잊어버렸을 때), lock_settings(L4 잠금 설정), settings(앱 틀의 설정 탭: S6과 G2·G3·L4·M5),
 *   design_catalog(디자인 시스템 견본), list_performance(목록 성능 견본, 일기 1만 편·사진 5천 장, 따로 둔 견본 DB)
 * lock_settings와 settings는 실제 설정을 바꾼다.
 * 에뮬레이터에서 잠금을 켜거나 설정을 바꿔 보는 데 쓴다.
 */
class ScreenCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent.getStringExtra(EXTRA_SCREEN)
        setContent {
            DiaryTheme(darkTheme = isSystemInDarkTheme()) {
                when (screen) {
                    MIGRATION_REPORT, SUPPORT_SHEET -> {
                        MigrationReportScreen(
                            state = MigrationReportUiState(SAMPLE_REPORT, LocalDate(2026, 11, 1)),
                            onBack = ::finish,
                            onExportOriginal = {},
                            onContact = {}
                        )
                        if (screen == SUPPORT_SHEET) SupportAttachSheet(onDismiss = ::finish, onContactWithout = {}, onContactWith = {})
                    }

                    PHOTO_CELL -> PhotoCellSample()

                    LOCK_FORGOT -> {
                        LockScreen(LockUiState(), showBiometric = false, onDigit = {}, onDelete = {}, onBiometric = {}, onForgot = {})
                        ForgotPinSheet(deviceSecure = true, onDismiss = ::finish, onConfirm = {})
                    }

                    LOCK_SETTINGS -> LockSettingsRoute(onBack = ::finish)

                    SETTINGS -> ProvideScreenGutter { AppScaffold(startTab = SettingsKey) }

                    DESIGN_CATALOG -> DesignCatalogScreen(onBack = ::finish)

                    LIST_PERFORMANCE -> ListPerformanceSample()

                    RESTORE_LOADING -> RestoreListScreen(RestoreListUi.Loading, {}, {}, { finish() })

                    RESTORE_LIST ->
                        RestoreListScreen(
                            RestoreListUi.Loaded(
                                listOf(
                                    SAMPLE_BACKUP,
                                    SAMPLE_BACKUP.copy(snapshotName = "b", state = BackupEntryState.Damaged(DamageReason.SNAPSHOT_MISSING)),
                                    SAMPLE_BACKUP.copy(snapshotName = "c", state = BackupEntryState.NeedsNewerApp),
                                    SAMPLE_BACKUP.copy(snapshotName = "d", deviceId = "ipad", deviceName = "iPad", platform = "ios")
                                )
                            ),
                            onPick = {},
                            onRetry = {},
                            onBack = ::finish
                        )

                    RESTORE_CHECK ->
                        RestorePreviewScreen(RestorePreparation.Ready(SAMPLE_BACKUP, SAMPLE_PREVIEW), onMerge = {
                        }, onRetry = {}, onBack = ::finish)

                    RESTORE_NO_SPACE ->
                        RestorePreviewScreen(
                            RestorePreparation.Ready(SAMPLE_BACKUP, SAMPLE_PREVIEW.copy(deviceBytesFree = 200_000_000)),
                            onMerge = {},
                            onRetry = {},
                            onBack = ::finish
                        )

                    LETTER -> LetterScreen(1_204, onStart = ::finish)

                    FULL_LETTER -> FullLetterScreen(1204, onBack = ::finish)

                    RESTORE_LEGACY ->
                        RestorePreviewScreen(
                            RestorePreparation.Ready(
                                SAMPLE_BACKUP.copy(platform = PLATFORM_LEGACY),
                                SAMPLE_PREVIEW.copy(entriesAdded = 85, entriesUnchanged = 1_120, entriesKeptBoth = 3)
                            ),
                            onMerge = {},
                            onRetry = {},
                            onBack = ::finish
                        )

                    RESTORE_LEGACY_RESULT ->
                        Progress(RestoreState.Done(RestoreResult(85, 0, 3, 0, 40, 0, fromLegacy = true)))

                    RESTORE_RUNNING -> Progress(RestoreState.FetchingPhotos(1_324, 120, 340))

                    RESTORE_RESULT -> Progress(
                        RestoreState.Done(RestoreResult(120, 8, 2, 328, 0, 0, photosOnOriginal = 12, lockWasOn = true))
                    )

                    RESTORE_FAILED -> Progress(RestoreState.Failed(RestoreFailure.MISMATCH))

                    RESTORE_FOUND -> BackupFoundSheet(SAMPLE_BACKUP, onLater = ::finish, onRestore = {})

                    else -> finish()
                }
            }
        }
    }

    @Composable
    private fun Progress(state: RestoreState) =
        RestoreProgressScreen(state, onSeeEntries = {}, onStart = {}, onLock = {}, onRetry = {}, onPickOther = {})

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val RESTORE_LIST = "restore_list"
        const val RESTORE_LOADING = "restore_loading"
        const val RESTORE_CHECK = "restore_check"
        const val RESTORE_NO_SPACE = "restore_no_space"
        const val LETTER = "letter"
        const val FULL_LETTER = "full_letter"
        const val RESTORE_LEGACY = "restore_legacy"
        const val RESTORE_LEGACY_RESULT = "restore_legacy_result"
        const val RESTORE_RUNNING = "restore_running"
        const val RESTORE_RESULT = "restore_result"
        const val RESTORE_FAILED = "restore_failed"
        const val RESTORE_FOUND = "restore_found"

        // Figma B3·B4와 같은 숫자
        val SAMPLE_BACKUP =
            BackupEntry(
                "a",
                "phone",
                "Galaxy S24",
                "android",
                Instant.parse("2026-10-31T12:30:00Z"),
                1_204,
                860,
                0,
                "2.0.0",
                BackupEntryState.Ready
            )
        val SAMPLE_PREVIEW = RestorePreview(120, 8, 2, 1_074, 340, 1_200_000_000, 0, true, 8_000_000_000)
        const val MIGRATION_REPORT = "migration_report"
        const val SUPPORT_SHEET = "support_sheet"
        const val PHOTO_CELL = "photo_cell"
        const val LOCK_FORGOT = "lock_forgot"
        const val LOCK_SETTINGS = "lock_settings"
        const val SETTINGS = "settings"
        const val DESIGN_CATALOG = "design_catalog"
        const val LIST_PERFORMANCE = "list_performance"

        // Figma M5 · 일부만과 같은 숫자
        val SAMPLE_REPORT =
            MigrationReport(
                entriesTotal = 1_296,
                entriesMigrated = 1_284,
                entriesUnreadable = 12,
                photosFound = 1_998,
                photosMissing = 33,
                dateDiffers = 2,
                dateUnparsed = 0,
                thumbnailsFailed = 0,
                settingsMigrated = emptyList(),
                finishedAt = Instant.parse("2026-11-01T03:00:00Z"),
                outcome = MigrationOutcome.PARTIAL
            )
    }
}

/**
 * 옮긴 사진(`files/media`)으로 일기 줄 사진 칸을 그린다.
 * 사진 칸은 썸네일만 읽고, 없으면 처음 보일 때 만든다(FR-036·FR-037).
 * 앱 틀처럼 이미지 로더를 썸네일 저장소에 잇는다.
 */
@Composable
private fun PhotoCellSample() {
    val context = LocalContext.current
    val thumbnails = remember { GlobalContext.get().get<ThumbnailRepository>() }
    ProvideDiaryImageLoader { thumbnails.thumbnail(it.toPath())?.toString() }
    val photos = remember { File(context.filesDir, "media").listFiles().orEmpty().map { it.absolutePath }.sorted() }
    val colors = DiaryTheme.colors
    Column(Modifier.fillMaxSize().background(colors.canvas).systemBarsPadding()) {
        listOf(photos.take(1), photos, photos + null).forEachIndexed { index, cell ->
            DiaryEntryRow(
                weekday = null,
                day = (index + 1).toString(),
                preview = cell.size.toString(),
                dividerAbove = index > 0,
                photo = { DiaryPhotoCell(cell) },
                meta = {}
            )
        }
    }
}
