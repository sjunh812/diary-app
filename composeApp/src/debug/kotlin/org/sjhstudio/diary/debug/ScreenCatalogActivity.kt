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
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.ProvideDiaryImageLoader
import org.sjhstudio.diary.feature.lock.ForgotPinSheet
import org.sjhstudio.diary.feature.lock.LockScreen
import org.sjhstudio.diary.feature.lock.LockSettingsRoute
import org.sjhstudio.diary.feature.lock.LockUiState
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportUiState

/**
 * 개발 빌드에만 있는 화면 모음. 앱 틀에서 아직 들어갈 길이 없는 화면을 견본 상태로 띄워 Figma와 견준다.
 * 일기 DB나 설정은 건드리지 않는다.
 *
 *   adb shell am start -n org.sjhstudio.diary/.debug.ScreenCatalogActivity --es screen migration_report
 *   화면 이름: migration_report(M5), support_sheet(M5 · 고쳐 받기 문의), photo_cell(일기 줄 사진 칸, 옮긴 사진으로),
 *   lock_forgot(L3 · 비밀번호를 잊어버렸을 때), lock_settings(L4 잠금 설정)
 * lock_settings만 실제 잠금 설정을 바꾼다. 에뮬레이터에서 잠금을 켜 보는 데 쓴다.
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

                    else -> finish()
                }
            }
        }
    }

    private companion object {
        const val EXTRA_SCREEN = "screen"
        const val MIGRATION_REPORT = "migration_report"
        const val SUPPORT_SHEET = "support_sheet"
        const val PHOTO_CELL = "photo_cell"
        const val LOCK_FORGOT = "lock_forgot"
        const val LOCK_SETTINGS = "lock_settings"

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
 * 옮긴 사진(`files/media`)으로 일기 줄 사진 칸을 그린다. 사진 칸은 썸네일만 읽고, 없으면 처음 보일 때 만든다(FR-036·FR-037).
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
                meta = emptyList(),
                preview = cell.size.toString(),
                moodColor = null,
                favorite = false,
                dividerAbove = index > 0,
                photo = { DiaryPhotoCell(cell) }
            )
        }
    }
}
