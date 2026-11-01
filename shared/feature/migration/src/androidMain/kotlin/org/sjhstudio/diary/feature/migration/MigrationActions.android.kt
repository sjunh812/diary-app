package org.sjhstudio.diary.feature.migration

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.OriginalExporter
import org.sjhstudio.diary.core.migration.originalExportFileName
import org.sjhstudio.diary.feature.migration.resources.Res
import org.sjhstudio.diary.feature.migration.resources.migration_export_done
import org.sjhstudio.diary.feature.migration.resources.migration_export_failed
import org.sjhstudio.diary.feature.migration.resources.migration_mail_body
import org.sjhstudio.diary.feature.migration.resources.migration_mail_missing
import org.sjhstudio.diary.feature.migration.resources.migration_mail_subject
import org.sjhstudio.diary.feature.migration.resources.migration_readme

/** 옛 앱 문의처(contracts/migration-flow.md "문의하기"). */
private const val SUPPORT_EMAIL = "sjunh812@naver.com"

@Composable
actual fun rememberMigrationActions(onMessage: MigrationMessage): MigrationActions? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = koinInject<OriginalExporter>()
    val migration = koinInject<LegacyMigration>()
    // 저장 창(SAF): 사용자가 고른 곳에만 쓴다.
    val saver =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val readme = getString(Res.string.migration_readme)
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { exporter.write(it, readme) } ?: error("열 수 없음")
                        }.isSuccess
                    }
                onMessage.show(getString(if (ok) Res.string.migration_export_done else Res.string.migration_export_failed))
            }
        }
    return remember(saver) {
        MigrationActions(
            exportOriginal = {
                saver.launch(originalExportFileName(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date))
            },
            contact = {
                scope.launch {
                    val summary = migration.supportSummary()
                    val version = runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }.getOrNull().orEmpty()
                    val body =
                        getString(
                            Res.string.migration_mail_body,
                            version,
                            "Android ${Build.VERSION.RELEASE}",
                            summary.failure?.step?.id ?: "-",
                            summary.failure?.kind?.name ?: "-",
                            summary.attempt.toString(),
                            summary.entriesBucket,
                            summary.photosBucket,
                            summary.unreadableBucket
                        )
                    val intent =
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                            putExtra(Intent.EXTRA_SUBJECT, getString(Res.string.migration_mail_subject))
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        onMessage.show(getString(Res.string.migration_mail_missing))
                    }
                }
            }
        )
    }
}
