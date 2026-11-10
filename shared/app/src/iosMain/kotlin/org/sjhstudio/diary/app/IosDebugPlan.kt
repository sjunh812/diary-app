package org.sjhstudio.diary.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.Plan
import platform.Foundation.NSUserDefaults

private const val PLAN_ARGUMENT = "diaryPlan"

/**
 * 개발 빌드에서만 요금제를 정함.
 * 안드로이드의 개발자 모드 대신 실행 인자 `-diaryPlan PLUS`로 유료 상태를 시험.
 * 출시 빌드는 부르지 않음
 */
internal fun applyDebugPlan() {
    val name = NSUserDefaults.standardUserDefaults.stringForKey(PLAN_ARGUMENT) ?: return
    val plan = Plan.entries.firstOrNull { it.name == name } ?: return
    loadKoinModules(
        module {
            single<PlanRepository> {
                object : PlanRepository {
                    override val plan: StateFlow<Plan> = MutableStateFlow(plan)
                }
            }
        }
    )
}
