package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.SavedStateHandle
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.testing.FakePlanRepository

/** 팁을 띄우지 않는 시험용 */
private object NoTips : ContextTips {
    override suspend fun claim(tip: ContextTip): Boolean = false

    override suspend fun done(tip: ContextTip) = Unit
}

/** 시험용. 요금제는 가짜(무료), 팁은 없음 */
internal fun testInsightViewModel(
    stats: StatsRepository,
    today: () -> LocalDate,
    plans: PlanRepository = FakePlanRepository(),
    saved: SavedStateHandle = SavedStateHandle(),
    tips: ContextTips = NoTips
) = InsightViewModel(stats, today, plans, saved, tips)
