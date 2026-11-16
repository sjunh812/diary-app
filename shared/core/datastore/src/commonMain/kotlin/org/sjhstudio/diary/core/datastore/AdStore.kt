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
 * `ads.preferences_pb`
 * 기기 것이라 백업에 넣지 않음
 */
data class AdPrefs(val lastInterstitialDay: String? = null, val rewardUntil: Instant? = null, val interstitialNoticed: Boolean = false)

class AdStore(dataStore: DataStore<Preferences>) {
    // 공간이 차 못 적어도 이 프로세스 안에서는 하루 한 번과 안내에 답함을 지킴
    private val store = OverlaidPreferences(dataStore)

    val prefs: Flow<AdPrefs> =
        store.data.map {
            AdPrefs(it[LAST_INTERSTITIAL], it[REWARD_UNTIL]?.let(Instant::fromEpochMilliseconds), it[INTERSTITIAL_NOTICED] == true)
        }

    suspend fun current(): AdPrefs = prefs.first()

    /** [day]가 null이면 기록 삭제 */
    suspend fun interstitialShown(day: String?) = store.edit { prefs ->
        if (day == null) prefs.remove(LAST_INTERSTITIAL) else prefs[LAST_INTERSTITIAL] = day
    }

    suspend fun rewardUntil(until: Instant) = store.edit { it[REWARD_UNTIL] = until.toEpochMilliseconds() }

    /** 첫 전면 광고 앞 안내를 보여 줌. 다시 띄우지 않음 */
    suspend fun interstitialNoticed() = store.edit { it[INTERSTITIAL_NOTICED] = true }

    private companion object {
        val LAST_INTERSTITIAL = stringPreferencesKey("last_interstitial_day")
        val REWARD_UNTIL = longPreferencesKey("reward_until")
        val INTERSTITIAL_NOTICED = booleanPreferencesKey("interstitial_noticed")
    }
}
