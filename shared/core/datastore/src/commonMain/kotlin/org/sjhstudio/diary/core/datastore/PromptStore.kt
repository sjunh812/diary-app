package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

class PromptStore(private val dataStore: DataStore<Preferences>) {
    val prefs: Flow<PromptPrefs> =
        dataStore.data.map {
            PromptPrefs(
                firstUseAt = it[FIRST_USE_AT]?.let(Instant::fromEpochMilliseconds),
                feedbackDone = it[FEEDBACK_DONE] == true,
                feedbackShownDay = it[FEEDBACK_SHOWN_DAY],
                reviewAskedDay = it[REVIEW_ASKED_DAY]
            )
        }

    suspend fun current(): PromptPrefs = prefs.first()

    /** 처음 한 번만 적음 */
    suspend fun firstUse(at: Instant) = dataStore.edit { if (it[FIRST_USE_AT] == null) it[FIRST_USE_AT] = at.toEpochMilliseconds() }

    suspend fun feedbackDone() = dataStore.edit { it[FEEDBACK_DONE] = true }

    suspend fun feedbackShown(day: String) = dataStore.edit { it[FEEDBACK_SHOWN_DAY] = day }

    suspend fun reviewAsked(day: String) = dataStore.edit { it[REVIEW_ASKED_DAY] = day }

    private companion object {
        val FIRST_USE_AT = longPreferencesKey("first_use_at")
        val FEEDBACK_DONE = booleanPreferencesKey("feedback_done")
        val FEEDBACK_SHOWN_DAY = stringPreferencesKey("feedback_shown_day")
        val REVIEW_ASKED_DAY = stringPreferencesKey("review_asked_day")
    }
}
