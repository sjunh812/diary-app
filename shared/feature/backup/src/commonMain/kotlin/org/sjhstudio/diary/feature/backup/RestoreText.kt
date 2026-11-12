package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.ui.ByteUnit
import org.sjhstudio.diary.core.ui.byteSize
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.restore_count_entries
import org.sjhstudio.diary.feature.backup.resources.restore_count_photos
import org.sjhstudio.diary.feature.backup.resources.restore_device_group
import org.sjhstudio.diary.feature.backup.resources.restore_device_unknown
import org.sjhstudio.diary.feature.backup.resources.restore_group_legacy
import org.sjhstudio.diary.feature.backup.resources.restore_join
import org.sjhstudio.diary.feature.backup.resources.restore_platform_android
import org.sjhstudio.diary.feature.backup.resources.restore_platform_ios
import org.sjhstudio.diary.feature.backup.resources.restore_size_gb
import org.sjhstudio.diary.feature.backup.resources.restore_size_kb
import org.sjhstudio.diary.feature.backup.resources.restore_size_mb
import org.sjhstudio.diary.feature.backup.resources.restore_unit_photos
import org.sjhstudio.diary.feature.backup.resources.restore_when

// 복원 화면에 쓰는 기기 이름·날짜·개수·크기 글

@Composable
internal fun deviceName(entry: BackupEntry): String = entry.deviceName.ifEmpty { stringResource(Res.string.restore_device_unknown) }

@Composable
internal fun deviceLabel(entry: BackupEntry): String = when (entry.platform) {
    PLATFORM_LEGACY -> stringResource(Res.string.restore_group_legacy)
    "android" -> stringResource(Res.string.restore_device_group, deviceName(entry), stringResource(Res.string.restore_platform_android))
    "ios" -> stringResource(Res.string.restore_device_group, deviceName(entry), stringResource(Res.string.restore_platform_ios))
    else -> deviceName(entry)
}

@Composable
internal fun whenText(entry: BackupEntry): String {
    val dates = rememberDiaryDateFormat()
    val local = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    return stringResource(Res.string.restore_when, dates.monthDay(local.date), dates.time(local.time))
}

@Composable
internal fun legacyDate(entry: BackupEntry): String =
    rememberDiaryDateFormat().fullDate(entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)

@Composable
internal fun countsText(entry: BackupEntry): String = stringResource(
    Res.string.restore_join,
    pluralStringResource(Res.plurals.restore_count_entries, entry.entryCount, entry.entryCount.grouped()),
    pluralStringResource(Res.plurals.restore_count_photos, entry.photoCount, entry.photoCount.grouped())
)

/** 받을 사진이 없으면 크기는 뜻이 없어 장수만 보임 */
@Composable
internal fun photosToReceive(photos: Int, bytes: Long): String {
    val count = pluralStringResource(Res.plurals.restore_unit_photos, photos, photos.grouped())
    return if (photos == 0) count else stringResource(Res.string.restore_join, count, sizeText(bytes))
}

@Composable
internal fun sizeText(bytes: Long): String {
    val size = byteSize(bytes)
    return when (size.unit) {
        ByteUnit.KB -> stringResource(Res.string.restore_size_kb, size.value)
        ByteUnit.MB -> stringResource(Res.string.restore_size_mb, size.value)
        ByteUnit.GB -> stringResource(Res.string.restore_size_gb, size.value)
    }
}

internal const val LOADING_DEVICES = 2
internal const val LOADING_VALUES = 4
internal const val LOADING_TITLE_WIDTH = 0.42f
internal const val LOADING_BODY_WIDTH = 0.3f
internal const val LOADING_HERO_WIDTH = 0.5f
internal const val SUPPORT_ADDRESS = "sjunh812@naver.com"
internal const val SUPPORT_MAIL = "mailto:$SUPPORT_ADDRESS"
