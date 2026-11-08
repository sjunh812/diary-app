package org.sjhstudio.diary.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.ui.BUNDLED_DIARY_FONTS
import org.sjhstudio.diary.core.ui.launchWrite

/**
 * 글꼴(G1) 상태.
 * [installedPaths]는 받아 둔 글꼴 파일, [downloading]은 받는 중인 글꼴과 진행(0~1, 모르면 null)이다.
 * 앱에 넣은 글꼴은 늘 쓸 수 있다.
 */
data class FontUiState(
    val settings: AppSettings = AppSettings(),
    val installedPaths: Map<DiaryFont, String> = emptyMap(),
    val downloading: Map<DiaryFont, Float?> = emptyMap()
) {
    fun isReady(font: DiaryFont): Boolean = font in BUNDLED_DIARY_FONTS || font in installedPaths
}

/** 받지 못했을 때 한 번 알리는 것. */
enum class FontNotice { Offline, Failed }

/**
 * 글꼴 고르기.
 * 앱에 있거나 받아 둔 글꼴은 바로 바꾸고, 아니면 받은 뒤 바꾼다(`docs/design.md` 일기 본문 글꼴 10종).
 * 받지 못하면 쓰던 글꼴을 그대로 둔다.
 * 받는 동안 다른 글꼴은 그대로 고를 수 있다.
 */
class FontSettingsViewModel(
    private val settings: SettingsRepository,
    private val installer: FontInstaller,
    private val telemetry: Telemetry = Telemetry(NoTelemetrySink, strict = true)
) : ViewModel() {
    private val installed =
        MutableStateFlow(
            DiaryFont.entries.filter {
                it !in BUNDLED_DIARY_FONTS
            }.mapNotNull { font -> installer.installedPath(font.id)?.let { font to it } }.toMap()
        )
    private val downloading = MutableStateFlow<Map<DiaryFont, Float?>>(emptyMap())
    private val notices = Channel<FontNotice>(Channel.BUFFERED)

    /** 마지막으로 누른 글꼴. */
    private var picked: DiaryFont? = null
    val notice: Flow<FontNotice> = notices.receiveAsFlow()

    val state: StateFlow<FontUiState> =
        combine(settings.settings, installed, downloading) { app, paths, progress -> FontUiState(app, paths, progress) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FontUiState(installedPaths = installed.value))

    fun select(font: DiaryFont) {
        picked = font
        if (state.value.isReady(font)) {
            apply(font)
            return
        }
        if (font in downloading.value) return
        downloading.update { it + (font to null) }
        viewModelScope.launch {
            installer.install(font.id).collect { result ->
                when (result) {
                    is FontInstallState.Downloading -> downloading.update { it + (font to result.fraction) }

                    is FontInstallState.Installed -> {
                        downloading.update { it - font }
                        installed.update { it + (font to result.path) }
                        // 받는 동안 다른 글꼴을 골랐으면 그것을 둔다.
                        if (picked == font) apply(font)
                    }

                    is FontInstallState.Failed -> {
                        downloading.update { it - font }
                        notices.send(if (result.offline) FontNotice.Offline else FontNotice.Failed)
                    }
                }
            }
        }
    }

    private fun apply(font: DiaryFont) {
        viewModelScope.launchWrite { settings.setFont(font) }
        // 글꼴 ID만 보낸다(contracts/app-shell.md 사용 통계 고정 표).
        telemetry.log(AnalyticsEvents.SETTINGS_CHANGED, mapOf("name" to "font", "value" to font.id))
    }
}
