package org.sjhstudio.diary.feature.migration

import androidx.compose.runtime.Composable

// iOS에는 옮길 옛 앱이 없어 실패 화면이 뜨지 않음
@Composable
actual fun rememberMigrationActions(onMessage: MigrationMessage): MigrationActions? = null
