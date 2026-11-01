package org.sjhstudio.diary.core.database

import androidx.room3.Room
import org.sjhstudio.diary.core.platform.AppFiles

actual class DatabaseFactory(private val files: AppFiles) {
    actual fun create(): DiaryDatabase = Room
        .databaseBuilder(files.databasePath, DiaryDatabaseConstructor::initialize)
        .configured()
        .build()
}
