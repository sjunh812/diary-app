package org.sjhstudio.diary.feature.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.data.WelcomeRepository
import org.sjhstudio.diary.core.model.WelcomeStep
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.launchSafely

class WelcomeViewModel(private val welcome: WelcomeRepository) : ViewModel() {
    private val _step = MutableStateFlow<WelcomeStep?>(null)

    /** null이면 아직 읽는 중이다. */
    val step: StateFlow<WelcomeStep?> = _step.asStateFlow()

    private val _entries = MutableStateFlow<Int?>(null)

    /** 옮긴 일기 수. 편지가 쓴다. */
    val entries: StateFlow<Int?> = _entries.asStateFlow()

    init {
        refresh()
    }

    /**
     * 어디까지 봤는지 읽는다.
     * 저장 공간 문제로 읽지 못하면 편지와 온보딩을 건너뛰고 앱을 연다.
     * 여기서 멈추거나 꺼지면 일기를 쓸 수 없다.
     */
    private fun refresh() {
        viewModelScope.launchSafely {
            // 편지가 개수 없이 먼저 그려졌다가 문단이 끼어들지 않게 개수부터 읽는다.
            _entries.value = orElseOnFailure(null) { welcome.migratedEntries() }
            _step.value = orElseOnFailure(WelcomeStep.DONE) { welcome.step() }
        }
    }

    fun letterRead() = pass(WelcomeStep.ONBOARDING_UPDATED) { welcome.letterRead() }

    fun finish() = pass(WelcomeStep.DONE) { welcome.onboardingFinished() }

    /**
     * 본 것을 적고 다음 화면으로 간다.
     * 저장 공간이 차 적지 못해도 넘어간다.
     * 다음에 앱을 열면 같은 화면을 다시 볼 뿐이고, 여기서 막히면 앱을 쓸 수 없다.
     */
    private fun pass(next: WelcomeStep, write: suspend () -> Unit) {
        viewModelScope.launchSafely {
            _step.value =
                orElseOnFailure(next) {
                    write()
                    welcome.step()
                }
        }
    }
}

/** 설정에서 다시 읽는 편지. */
class LetterViewModel(welcome: WelcomeRepository) : ViewModel() {
    private val _letter = MutableStateFlow<Letter?>(null)

    /** null이면 아직 읽는 중이다. 개수 없는 편지를 먼저 그리지 않는다. */
    val letter: StateFlow<Letter?> = _letter.asStateFlow()

    init {
        viewModelScope.launchSafely { _letter.value = Letter(orElseOnFailure(null) { welcome.migratedEntries() }) }
    }
}

/** [entries]는 옮긴 일기 수다. 새로 설치했거나 읽지 못하면 null이다. */
data class Letter(val entries: Int?)
