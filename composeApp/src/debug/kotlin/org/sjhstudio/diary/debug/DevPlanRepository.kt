package org.sjhstudio.diary.debug

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.Plan

/** 개발자 모드에서 고른 요금제. 개발 빌드에만 있고, 앱을 다시 켜도 남는다. */
class DevPlanRepository(context: Context) : PlanRepository {
    private val prefs = context.getSharedPreferences("dev_mode", Context.MODE_PRIVATE)
    private val chosen = MutableStateFlow(prefs.getString(KEY, null)?.let(Plan::valueOf) ?: Plan.FREE)

    override val plan: StateFlow<Plan> = chosen.asStateFlow()

    fun choose(plan: Plan) {
        prefs.edit { putString(KEY, plan.name) }
        chosen.value = plan
    }

    private companion object {
        const val KEY = "plan"
    }
}
