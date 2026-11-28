package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.sjhstudio.diary.core.model.quietly
import org.sjhstudio.diary.core.model.withoutCustom

/**
 * Plus가 아니면 Plus 꾸미기를 되돌림
 * 직접 고른 테마 색은 지우고 라벤더로(스펙 010 US6), Plus 기분 테마는 기본으로(스펙 013 US4). 클레이 꼴은 남김
 * 요금제와 설정을 함께 봄. Plus가 끝날 때도, 무료일 때 Plus 꾸미기가 든 백업을 복원할 때도 되돌림
 * 요금제를 확정한 답만 봄. 재설치 직후처럼 아직 모를 때 산 사람의 꾸미기를 지우지 않게
 */
class PlusLookReset(private val settings: SettingsRepository, private val plans: PlanRepository) {
    suspend fun watch() {
        combine(plans.confirmed.filterNotNull(), settings.settings) { plan, current ->
            val theme = !plan.customTheme && current.theme != current.theme.withoutCustom()
            val mood = !plan.moodThemes && current.moodLook != current.moodLook.withoutPlusTheme()
            theme to mood
        }.collect { (theme, mood) ->
            // 저장에 실패해도 감시는 이어 감. 보이는 꾸미기는 이미 막혀 있고 다음 변경이나 다음 실행에 다시 되돌림
            if (theme) quietly { settings.updateTheme { it.withoutCustom() } }
            if (mood) quietly { settings.updateMoodLook { it.withoutPlusTheme() } }
        }
    }
}
