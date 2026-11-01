package org.sjhstudio.diary.app.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.sjhstudio.diary.core.database.DatabaseFactory
import org.sjhstudio.diary.core.platform.AppFiles

internal actual val platformModule: Module =
    module {
        single { AppFiles(androidContext()) }
        single { DatabaseFactory(androidContext(), get()) }
    }
