package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** `ads.preferences_pb`. 기기 것이라 백업에 넣지 않는다. */
data class AdPrefs(val lastInterstitialDay: String? = null, val rewardUntil: Instant? = null)

class AdStore(private val dataStore: DataStore<Preferences>) {
    val prefs: Flow<AdPrefs> =
        dataStore.data.map { AdPrefs(it[LAST_INTERSTITIAL], it[REWARD_UNTIL]?.let(Instant::fromEpochMilliseconds)) }

    suspend fun current(): AdPrefs = prefs.first()

    suspend fun interstitialShown(day: String) = dataStore.edit { it[LAST_INTERSTITIAL] = day }

    suspend fun rewardUntil(until: Instant) = dataStore.edit { it[REWARD_UNTIL] = until.toEpochMilliseconds() }

    private companion object {
        val LAST_INTERSTITIAL = stringPreferencesKey("last_interstitial_day")
        val REWARD_UNTIL = longPreferencesKey("reward_until")
    }
}
