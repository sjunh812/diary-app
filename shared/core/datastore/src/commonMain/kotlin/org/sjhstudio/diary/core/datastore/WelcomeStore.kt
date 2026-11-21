package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * `welcome.preferences_pb`
 * 기기 것이라 백업에 넣지 않음(FR-015)
 */
data class WelcomePrefs(
    val letterSeen: Boolean = false,
    val onboardingDone: Boolean = false,
    val tipsSeen: Set<String> = emptySet(),
    /** 일이 생겨 열린 팁(스펙 008) */
    val tipsOpened: Set<String> = emptySet(),
    /** 팁이 마지막으로 보이기 시작한 날(`yyyy-MM-dd`)과 그 팁. 하루에 팁 하나 */
    val lastTipDay: String? = null,
    val lastTip: String? = null,
    /** 3줄에서 잘린 일기를 상세로 연 횟수 */
    val clippedOpens: Int = 0
)

class WelcomeStore(private val dataStore: DataStore<Preferences>) {
    val prefs: Flow<WelcomePrefs> =
        dataStore.data.map {
            WelcomePrefs(
                letterSeen = it[LETTER_SEEN] ?: false,
                onboardingDone = it[ONBOARDING_DONE] ?: false,
                tipsSeen = it[TIPS_SEEN].orEmpty(),
                tipsOpened = it[TIPS_OPENED].orEmpty(),
                lastTipDay = it[LAST_TIP_DAY],
                lastTip = it[LAST_TIP],
                clippedOpens = it[CLIPPED_OPENS] ?: 0
            )
        }

    suspend fun current(): WelcomePrefs = prefs.first()

    suspend fun letterSeen() = dataStore.edit { it[LETTER_SEEN] = true }

    suspend fun onboardingDone() = dataStore.edit { it[ONBOARDING_DONE] = true }

    suspend fun tipSeen(id: String) = dataStore.edit { it[TIPS_SEEN] = it[TIPS_SEEN].orEmpty() + id }

    suspend fun tipOpened(id: String) = dataStore.edit { it[TIPS_OPENED] = it[TIPS_OPENED].orEmpty() + id }

    suspend fun tipShown(day: String, id: String) = dataStore.edit {
        it[LAST_TIP_DAY] = day
        it[LAST_TIP] = id
    }

    suspend fun clippedOpens(count: Int) = dataStore.edit { it[CLIPPED_OPENS] = count }

    private companion object {
        val LETTER_SEEN = booleanPreferencesKey("letter_seen")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val TIPS_SEEN = stringSetPreferencesKey("tips_seen")
        val TIPS_OPENED = stringSetPreferencesKey("tips_opened")
        val LAST_TIP_DAY = stringPreferencesKey("last_tip_day")
        val LAST_TIP = stringPreferencesKey("last_tip")
        val CLIPPED_OPENS = intPreferencesKey("clipped_opens")
    }
}
