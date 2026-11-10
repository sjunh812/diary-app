package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * `welcome.preferences_pb`
 * 기기 것이라 백업에 넣지 않음(FR-015)
 */
data class WelcomePrefs(val letterSeen: Boolean = false, val onboardingDone: Boolean = false, val tipsSeen: Set<String> = emptySet())

class WelcomeStore(private val dataStore: DataStore<Preferences>) {
    val prefs: Flow<WelcomePrefs> =
        dataStore.data.map {
            WelcomePrefs(it[LETTER_SEEN] ?: false, it[ONBOARDING_DONE] ?: false, it[TIPS_SEEN].orEmpty())
        }

    suspend fun current(): WelcomePrefs = prefs.first()

    suspend fun letterSeen() = dataStore.edit { it[LETTER_SEEN] = true }

    suspend fun onboardingDone() = dataStore.edit { it[ONBOARDING_DONE] = true }

    suspend fun tipSeen(id: String) = dataStore.edit { it[TIPS_SEEN] = it[TIPS_SEEN].orEmpty() + id }

    private companion object {
        val LETTER_SEEN = booleanPreferencesKey("letter_seen")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val TIPS_SEEN = stringSetPreferencesKey("tips_seen")
    }
}
