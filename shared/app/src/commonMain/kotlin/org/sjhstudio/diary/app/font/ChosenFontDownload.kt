package org.sjhstudio.diary.app.font

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BUNDLED_DIARY_FONTS

/**
 * 고른 글꼴이 받는 글꼴인데 아직 없으면 앱을 연 뒤 받기 시작한다(스펙 001 T067, `docs/design.md` 일기 본문 글꼴 10종).
 * 예전 앱에서 옮겨 온 글꼴이 받는 글꼴일 때와, 다른 기기에서 복원해 글꼴 파일이 없을 때가 여기에 걸린다.
 * 받을 때까지 앱은 Pretendard로 보이고, 받지 못하면 다음에 앱을 열 때 다시 한다.
 * 묻거나 알리지 않는다.
 */
class ChosenFontDownload(
    private val settings: SettingsRepository,
    private val installer: FontInstaller,
    private val scope: CoroutineScope
) {
    private val _finished = MutableStateFlow(0)

    /**
     * 받기가 끝날 때마다 늘어난다.
     * 앱 뿌리가 이 값을 보고 받은 글꼴로 바꾼다.
     */
    val finished: StateFlow<Int> = _finished.asStateFlow()

    fun start() {
        scope.launch {
            val font = settings.settings.first().font
            if (font in BUNDLED_DIARY_FONTS || installer.installedPath(font.id) != null) return@launch
            installer.install(font.id).takeWhile { it is FontInstallState.Downloading }.collect {}
            _finished.update { it + 1 }
        }
    }
}
