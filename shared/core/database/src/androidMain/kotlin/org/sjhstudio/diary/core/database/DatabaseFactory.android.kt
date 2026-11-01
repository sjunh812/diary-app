package org.sjhstudio.diary.core.database

import android.content.Context
import androidx.room3.Room
import org.sjhstudio.diary.core.platform.AppFiles

actual class DatabaseFactory(private val context: Context, private val files: AppFiles) {
    actual fun create(): DiaryDatabase = Room
        .databaseBuilder(context.applicationContext, files.databasePath, DiaryDatabaseConstructor::initialize)
        .configured()
        .build()
}
