package org.sjhstudio.diary.app.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * 앱 프로세스가 시작할 때 한 번 부른다(Android `DiaryApplication`, iOS `iOSApp`).
 * [isDebug]면 사용 통계 고정 표에 없는 값을 보낼 때 바로 예외를 던진다.
 */
fun initKoin(isDebug: Boolean, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules(isDebug))
    }
}
