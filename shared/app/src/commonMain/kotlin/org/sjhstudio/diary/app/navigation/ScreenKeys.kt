package org.sjhstudio.diary.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * 화면 키(contracts/app-shell.md 화면 키)
 * 스택을 저장할 때 쓸 직렬화기를 한 번에 등록하려고 sealed interface로 묶음
 * 일기 ID 같은 값을 든 키는 `toString()`에 그 값을 담지 않음. 비정상 종료 보고에 섞이지 않게(헌법 II)
 */
@Serializable
sealed interface Screen : NavKey

/** 하단 탭·레일의 맨 위 화면 */
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

/** 찾기. 돌아보기에서 오면 기분과 기간(`yyyy-MM-dd`)이 채워져 있음 */
@Serializable
data class SearchKey(val moods: List<String> = emptyList(), val from: String? = null, val to: String? = null) : Screen {
    override fun toString(): String = "SearchKey"
}

/** 일기 상세 */
@Serializable
data class EntryDetailKey(val entryId: String) : Screen {
    override fun toString(): String = "EntryDetailKey"
}

/** 사진 크게 보기 */
@Serializable
data class PhotoViewerKey(val entryId: String, val index: Int) : Screen {
    override fun toString(): String = "PhotoViewerKey"
}

/**
 * 쓰기
 * [date]는 `yyyy-MM-dd`로, 달력 빈 날에서 오면 있음
 * [entryId]가 있으면 수정
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalUuidApi::class)
@Serializable
data class WriteKey(
    val date: String? = null,
    val entryId: String? = null,
    /**
     * 쓰기를 열 때마다 새로 만듦
     * 없으면 닫히는 화면이 사라지기 전에 다시 열 때 그 화면 모델을 그대로 받아
     * 닫는 중인 채로 시작해 뒤로 가기가 먹지 않고 키보드도 뜨지 않음
     * 앱이 다시 떠도 같은 화면으로 이어지게 함께 저장
     */
    @EncodeDefault val opened: String = Uuid.random().toString()
) : Screen {
    /**
     * 화면 모델과 저장 상태를 열 때마다 가르는 키
     * 기본 키는 [toString]을 써서 모든 쓰기가 같은 키가 됨
     */
    val contentKey: String get() = "write:$entryId:$date:$opened"

    /** 같은 일기를 가리키는지 */
    fun sameTarget(other: WriteKey): Boolean = date == other.date && entryId == other.entryId

    override fun toString(): String = "WriteKey"
}

/** 글꼴 하나의 라이선스 전문. [fontId]는 `DiaryFont.id` */
@Serializable
data class FontLicenseKey(val fontId: String) : Screen {
    override fun toString(): String = "FontLicenseKey"
}

/** 라이브러리 하나의 라이선스 전문. [libraryId]는 `group:artifact` */
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
