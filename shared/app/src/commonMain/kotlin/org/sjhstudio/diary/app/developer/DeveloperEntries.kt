package org.sjhstudio.diary.app.developer

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * 개발 빌드에만 있는 화면 묶음(FR-029). 앱 틀이 Koin에서 찾아 화면 전환에 보태고, 있으면 설정에 "개발" 묶음이 생긴다.
 * 출시 빌드는 이 바인딩을 등록하지 않는다. 그래서 화면으로 가는 길이 없고, 안드로이드는 R8이 화면 코드까지 뺀다.
 * 등록은 Android `DebugApplication`(src/debug)과 iOS `startApp`(디버그 바이너리일 때)이 한다.
 */
fun interface DeveloperEntries {
    fun EntryProviderScope<NavKey>.register(pop: () -> Unit)
}
