package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.model.Plan

/** 지금 요금제. 화면과 기능은 이것만 본다. 결제(스펙 006)가 붙으면 구현만 바뀐다. */
interface PlanRepository {
    val plan: StateFlow<Plan>
}

/** 결제가 붙기 전의 구현. 언제나 무료다. */
class FreePlanRepository : PlanRepository {
    override val plan: StateFlow<Plan> = MutableStateFlow(Plan.FREE).asStateFlow()
}
