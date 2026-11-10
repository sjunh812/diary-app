package org.sjhstudio.diary.feature.write

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.DraftRepository
import org.sjhstudio.diary.core.platform.NoPlaceFinder
import org.sjhstudio.diary.core.platform.PlaceFinder
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

/** 시험용. 설정과 요금제는 가짜(무료)로 채움 */
internal fun testWriteViewModel(
    target: WriteTarget,
    drafts: DraftRepository,
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    mediaPath: (String) -> String = { it },
    settings: SettingsRepository = FakeSettingsRepository(),
    fontPath: (String) -> String? = { null },
    plans: PlanRepository = FakePlanRepository(),
    places: PlaceFinder = NoPlaceFinder,
    restoredKey: String? = null
) = WriteViewModel(target, drafts, clock, timeZone, mediaPath, settings, fontPath, plans, places, restoredKey)
