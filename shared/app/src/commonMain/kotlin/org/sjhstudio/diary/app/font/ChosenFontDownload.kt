package org.sjhstudio.diary.app.font

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BUNDLED_DIARY_FONTS

/**
 * 고른 글꼴이 받는 글꼴인데 아직 없으면 앱을 연 뒤 받기 시작(스펙 001 T067, `docs/design.md` 일기 본문 글꼴 10종)
 * 예전 앱에서 옮겨 온 글꼴이 받는 글꼴일 때, 다른 기기에서 복원해 글꼴 파일이 없을 때 해당
 * 받을 때까지 앱은 Pretendard로 보이고, 다 받으면 [FontInstaller.changes]를 따라 바로 바뀜
 * 받지 못하면 다음에 앱을 열 때 다시 받음
 * 앱에서 뺀 글꼴의 받은 파일은 이때 지움
 * 묻거나 알리지 않음
 */
class ChosenFontDownload(
    private val settings: SettingsRepository,
    private val installer: FontInstaller,
    private val scope: CoroutineScope
) {
    fun start() {
        scope.launch {
            installer.removeExcept(DiaryFont.entries.mapTo(HashSet()) { it.id })
            val font = settings.settings.first().font
            if (font in BUNDLED_DIARY_FONTS || installer.installedPath(font.id) != null) return@launch
            installer.install(font.id).takeWhile { it is FontInstallState.Downloading }.collect {}
        }
    }
}
