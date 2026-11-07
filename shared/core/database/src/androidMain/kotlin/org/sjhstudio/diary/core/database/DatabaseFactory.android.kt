package org.sjhstudio.diary.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.sjhstudio.diary.core.platform.AppFiles

actual class DatabaseFactory(private val context: Context, private val files: AppFiles) {
    actual fun create(): DiaryDatabase = Room
        .databaseBuilder(context.applicationContext, files.databasePath, DiaryDatabaseConstructor::initialize)
        .configured()
        .build()

    actual fun createAt(path: String): DiaryDatabase = Room
        .databaseBuilder(context.applicationContext, path, DiaryDatabaseConstructor::initialize)
        .configured()
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .build()
}
