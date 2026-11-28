package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import org.sjhstudio.diary.core.model.ContextTip

/**
 * 목록 모양이 바뀌면 목록 보기 팁을 끝냄
 * 팁은 설정에 목록 모양이 있는 걸 모르는 사람에게 알리는 것
 * 설정에서 골랐거나 복원으로 바뀌었으면 이미 아는 사람이라 지금 피드여도 다시 띄우지 않음
 */
class ListStyleTipRule(private val settings: SettingsRepository, private val tips: ContextTips) {
    suspend fun watch() {
        // 처음 값은 저장된 모양이라 바뀐 것이 아님
        settings.settings.map { it.list.listStyle }.distinctUntilChanged().drop(1).collect { tips.done(ContextTip.HOME_LIST_STYLE) }
    }
}
