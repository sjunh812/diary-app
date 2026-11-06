package org.sjhstudio.diary.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 화면 키(contracts/app-shell.md 화면 키). 스택을 저장할 때 쓸 직렬화기를 한 번에 등록하려고 sealed interface로 묶는다.
 * 일기 ID 같은 값을 든 키는 `toString()`에 그 값을 담지 않는다. 비정상 종료 보고에 섞이지 않게 하려고다(헌법 II).
 */
@Serializable
sealed interface Screen : NavKey

/** 하단 탭·레일의 맨 위 화면. */
@Serializable
sealed interface TabScreen : Screen

@Serializable
data object HomeKey : TabScreen

@Serializable
data object CalendarKey : TabScreen

@Serializable
data object InsightKey : TabScreen

@Serializable
data object SettingsKey : TabScreen

@Serializable
data object FontSettingsKey : Screen

@Serializable
data object TextSizeSettingsKey : Screen

@Serializable
data object ThemeSettingsKey : Screen

@Serializable
data object ReminderSettingsKey : Screen

@Serializable
data object LockSettingsKey : Screen

@Serializable
data object MigrationReportKey : Screen

@Serializable
data object LicensesKey : Screen

@Serializable
data object TrashKey : Screen

@Serializable
data object ListStyleSettingsKey : Screen

/** 찾기. 돌아보기에서 오면 기분과 기간(`yyyy-MM-dd`)이 채워져 있다. */
@Serializable
data class SearchKey(val moods: List<String> = emptyList(), val from: String? = null, val to: String? = null) : Screen {
    override fun toString(): String = "SearchKey"
}

/** 일기 상세. */
@Serializable
data class EntryDetailKey(val entryId: String) : Screen {
    override fun toString(): String = "EntryDetailKey"
}

/** 사진 크게 보기. */
@Serializable
data class PhotoViewerKey(val entryId: String, val index: Int) : Screen {
    override fun toString(): String = "PhotoViewerKey"
}

/** 쓰기. [date]는 `yyyy-MM-dd`로, 달력 빈 날에서 오면 있다. [entryId]가 있으면 고치기다. */
@Serializable
data class WriteKey(val date: String? = null, val entryId: String? = null) : Screen {
    /**
     * 화면 모델과 저장 상태를 일기마다 가르는 키.
     * 기본 키는 [toString]을 쓰므로 모든 쓰기가 같은 키가 된다.
     */
    val contentKey: String get() = "write:$entryId:$date"

    override fun toString(): String = "WriteKey"
}

/** 글꼴 하나의 라이선스 전문. [fontId]는 `DiaryFont.id`다. */
@Serializable
data class FontLicenseKey(val fontId: String) : Screen {
    override fun toString(): String = "FontLicenseKey"
}

/** 라이브러리 하나의 라이선스 전문. [libraryId]는 `group:artifact`다. */
@Serializable
data class LibraryLicenseKey(val libraryId: String) : Screen {
    override fun toString(): String = "LibraryLicenseKey"
}

@Serializable
data object BackupSettingsKey : Screen

@Serializable
data object BackupOptionsKey : Screen

@Serializable
data object RestoreListKey : Screen

@Serializable
data class RestorePreviewKey(val snapshotName: String) : Screen {
    override fun toString(): String = "RestorePreviewKey"
}

@Serializable
data object RestoreProgressKey : Screen

@Serializable
data object FileBackupKey : Screen

@Serializable
data object PlusKey : Screen

@Serializable
data object AdvancedStatsKey : Screen

@Serializable
data object LetterKey : Screen

@Serializable
data object IntroKey : Screen
