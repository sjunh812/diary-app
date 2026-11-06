package org.sjhstudio.diary.feature.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.WelcomeRepository
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.WelcomeStep

class WelcomeViewModel(
    private val welcome: WelcomeRepository,
    private val backup: BackupRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    private val _step = MutableStateFlow<WelcomeStep?>(null)

    /** null이면 아직 읽는 중이다. */
    val step: StateFlow<WelcomeStep?> = _step.asStateFlow()

    val connected: StateFlow<Boolean> =
        backup.status.map {
            it.connection != BackupConnection.NotConnected
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), false)

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    val entries: Int? = welcome.migratedEntries()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch { _step.value = welcome.step() }
    }

    fun letterRead() {
        viewModelScope.launch {
            welcome.letterRead()
            _step.value = welcome.step()
        }
    }

    fun finish() {
        viewModelScope.launch {
            welcome.onboardingFinished()
            _step.value = welcome.step()
        }
    }

    /**
     * 예전 앱이 쓰던 계정을 먼저 보인다.
     * 연결을 마치거나 물러나면 [next]로 넘어간다.
     */
    fun connect(next: () -> Unit) {
        viewModelScope.launch {
            _connecting.value = true
            backup.connect(settings.settings.first().legacyBackup?.account)
            _connecting.value = false
            next()
        }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}

/** 설정에서 다시 읽는 편지. */
class LetterViewModel(welcome: WelcomeRepository) : ViewModel() {
    val entries: Int? = welcome.migratedEntries()
}
