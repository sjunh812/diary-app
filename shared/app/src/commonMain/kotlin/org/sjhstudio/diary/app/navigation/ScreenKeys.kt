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

/** 디자인 시스템 견본. 개발 빌드에만 길이 있다(FR-029, `developer/DeveloperEntries.kt`). */
@Serializable
data object DesignCatalogKey : Screen
