package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.Plan

/** 시험이 값을 바꾸는 요금제. 구매, 구독 만료, 환불을 값 하나로 흉내 냄. */
class FakePlanRepository(initial: Plan = Plan.FREE) : PlanRepository {
    override val plan: MutableStateFlow<Plan> = MutableStateFlow(initial)
}
