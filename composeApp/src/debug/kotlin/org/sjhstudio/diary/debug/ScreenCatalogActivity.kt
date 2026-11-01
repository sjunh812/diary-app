package org.sjhstudio.diary.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportUiState

/**
 * 개발 빌드에만 있는 화면 모음. 앱 틀에서 아직 들어갈 길이 없는 화면을 견본 상태로 띄워 Figma와 견준다.
 * 일기 DB나 설정은 건드리지 않는다.
 *
 *   adb shell am start -n org.sjhstudio.diary/.debug.ScreenCatalogActivity --es screen migration_report
 *   화면 이름: migration_report(M5), support_sheet(M5 · 고쳐 받기 문의)
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

                    else -> finish()
                }
            }
        }
    }

    private companion object {
        const val EXTRA_SCREEN = "screen"
        const val MIGRATION_REPORT = "migration_report"
        const val SUPPORT_SHEET = "support_sheet"

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
