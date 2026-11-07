package org.sjhstudio.diary.feature.backup

import org.sjhstudio.diary.core.model.RestoreState

/**
 * 복원 화면을 떠난 뒤에 나온 결과를 알릴지 정한다.
 * 결과는 닫을 때까지 남아 있어서 같은 결과를 두 번 알리지 않게 기억한다.
 */
internal class RestoreResultWatch {
    private var told: RestoreState? = null

    fun shouldTell(state: RestoreState, onResultScreen: Boolean, quiet: Boolean): Boolean {
        val fresh = (state is RestoreState.Done || state is RestoreState.Failed) && state !== told
        if (fresh && (onResultScreen || !quiet)) told = state
        return fresh && !onResultScreen && !quiet
    }
}
