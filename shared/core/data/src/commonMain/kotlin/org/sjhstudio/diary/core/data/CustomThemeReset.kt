package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.sjhstudio.diary.core.model.quietly
import org.sjhstudio.diary.core.model.withoutCustom

/**
 * Plus가 아니면 직접 고른 테마 색을 지우고 라벤더로 되돌림(스펙 010 US6, 사용자 결정)
 * 요금제와 설정을 함께 봄. Plus가 끝날 때도, 무료일 때 직접 고른 색이 든 백업을 복원할 때도 지움
 * 요금제를 확정한 답만 봄. 재설치 직후처럼 아직 모를 때 산 사람의 색을 지우지 않게
 */
class CustomThemeReset(private val settings: SettingsRepository, private val plans: PlanRepository) {
    suspend fun watch() {
        combine(plans.confirmed.filterNotNull(), settings.settings.map { it.theme }) { plan, theme ->
            !plan.customTheme && theme != theme.withoutCustom()
        }.collect { stale ->
            // 저장에 실패해도 감시는 이어 감. 보이는 색은 이미 막혀 있고 다음 변경이나 다음 실행에 다시 지움
            if (stale) quietly { settings.updateTheme { it.withoutCustom() } }
        }
    }
}
