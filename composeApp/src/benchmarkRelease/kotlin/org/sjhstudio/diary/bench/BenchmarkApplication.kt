package org.sjhstudio.diary.bench

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.sjhstudio.diary.DiaryApplication
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.Plan

/**
 * 측정 빌드의 앱
 * 고른 요금제가 있으면 앱 전체가 따르고, 없으면 실제 결제 그대로
 * 측정 빌드는 Play 서명과 달라 결제가 열리지 않음
 */
class BenchmarkApplication : DiaryApplication() {
    override fun onCreate() {
        super.onCreate()
        val chosen = benchPlan(this) ?: return
        loadKoinModules(
            module {
                single<PlanRepository> {
                    object : PlanRepository {
                        override val plan: StateFlow<Plan> = MutableStateFlow(chosen)
                    }
                }
            }
        )
    }
}

/**
 * `adb shell am start -n org.sjhstudio.diary/.bench.BenchPlanActivity --es plan PLUS`
 * 비우려면 `--es plan NONE`. 앱을 다시 켜야 따름
 */
class BenchPlanActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val plan = intent.getStringExtra(EXTRA_PLAN)?.let { name -> Plan.entries.firstOrNull { it.name == name } }
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) { putString(KEY, plan?.name) }
        finish()
    }
}

private fun benchPlan(context: Context): Plan? =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.let { name ->
        Plan.entries.firstOrNull { it.name == name }
    }

private const val PREFS = "bench"
private const val KEY = "plan"
private const val EXTRA_PLAN = "plan"
