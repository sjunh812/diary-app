package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.Plan

/** 시험이 값을 바꾸는 요금제. 구매, 구독 만료, 환불을 값 하나로 흉내 냄 */
class FakePlanRepository(initial: Plan = Plan.FREE, known: Boolean = true) : PlanRepository {
    override val plan: MutableStateFlow<Plan> = MutableStateFlow(initial)

    /** 거짓이면 재설치 직후처럼 요금제를 모름 */
    override val known: MutableStateFlow<Boolean> = MutableStateFlow(known)
}
