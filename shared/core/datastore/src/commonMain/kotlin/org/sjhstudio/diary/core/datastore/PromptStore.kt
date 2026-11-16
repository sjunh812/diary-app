package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * `prompt.preferences_pb`. 의견 카드와 리뷰 창 기록(스펙 007)
 * 기기 것이라 백업에 넣지 않음
 */
data class PromptPrefs(
    val firstUseAt: Instant? = null,
    val feedbackDone: Boolean = false,
    val feedbackShownDay: String? = null,
    val reviewAskedDay: String? = null
)

class PromptStore(dataStore: DataStore<Preferences>) {
    // 공간이 차 못 적어도 이 프로세스 안에서는 닫은 카드와 물은 리뷰를 다시 띄우지 않음
    private val store = OverlaidPreferences(dataStore)

    val prefs: Flow<PromptPrefs> =
        store.data.map {
            PromptPrefs(
                firstUseAt = it[FIRST_USE_AT]?.let(Instant::fromEpochMilliseconds),
                feedbackDone = it[FEEDBACK_DONE] == true,
                feedbackShownDay = it[FEEDBACK_SHOWN_DAY],
                reviewAskedDay = it[REVIEW_ASKED_DAY]
            )
        }

    suspend fun current(): PromptPrefs = prefs.first()

    /** 처음 한 번만 적음 */
    suspend fun firstUse(at: Instant) = store.edit { if (it[FIRST_USE_AT] == null) it[FIRST_USE_AT] = at.toEpochMilliseconds() }

    suspend fun feedbackDone() = store.edit { it[FEEDBACK_DONE] = true }

    suspend fun feedbackShown(day: String) = store.edit { it[FEEDBACK_SHOWN_DAY] = day }

    suspend fun reviewAsked(day: String) = store.edit { it[REVIEW_ASKED_DAY] = day }

    private companion object {
        val FIRST_USE_AT = longPreferencesKey("first_use_at")
        val FEEDBACK_DONE = booleanPreferencesKey("feedback_done")
        val FEEDBACK_SHOWN_DAY = stringPreferencesKey("feedback_shown_day")
        val REVIEW_ASKED_DAY = stringPreferencesKey("review_asked_day")
    }
}
