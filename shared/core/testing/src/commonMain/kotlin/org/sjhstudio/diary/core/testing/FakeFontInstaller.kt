package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller

/** 받은 글꼴 파일을 시험이 정하는 설치기. [finish]가 다 받은 것처럼 만듦 */
class FakeFontInstaller : FontInstaller {
    private val paths = mutableMapOf<String, String>()
    private val _changes = MutableStateFlow(0)
    override val changes: StateFlow<Int> = _changes.asStateFlow()

    override fun installedPath(id: String): String? = paths[id]

    override fun install(id: String): Flow<FontInstallState> = flow { emit(FontInstallState.Installed(finish(id))) }

    fun finish(id: String): String = "/fonts/$id.ttf".also {
        paths[id] = it
        _changes.update { count -> count + 1 }
    }
}
