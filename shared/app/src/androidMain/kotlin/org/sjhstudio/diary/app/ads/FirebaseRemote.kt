package org.sjhstudio.diary.app.ads

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.sjhstudio.diary.core.data.AdSwitch
import org.sjhstudio.diary.core.data.AppUpdateRepository
import org.sjhstudio.diary.core.data.RemoteValues

private const val INTERSTITIAL = "interstitial_enabled"

/** 무료 한도를 넘기지 않게 값은 12시간에 한 번만 받음. 키가 늘어도 받는 횟수는 같음 */
private const val FETCH_SECONDS = 12 * 60 * 60L

/**
 * Remote Config 원격 값. 전면 광고 스위치와 새 버전 안내(스펙 009)
 * Firebase 설정 파일 없이 만든 빌드는 기본값(광고 켜짐, 업데이트 값 빈 문자열)
 * 받기에 실패하면 마지막 값
 */
class FirebaseRemote(context: Context) {
    private val config: FirebaseRemoteConfig? =
        if (FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(FETCH_SECONDS).build())
                setDefaultsAsync(defaults())
                fetchAndActivate()
            }
        }

    val adSwitch = AdSwitch { config?.getBoolean(INTERSTITIAL) ?: true }

    val values = RemoteValues { key -> config?.getString(key).orEmpty() }

    private fun defaults(): Map<String, Any> {
        val update =
            listOf(AppUpdateRepository.KEY_MIN_VERSION, AppUpdateRepository.KEY_NOTE_VERSION) +
                AppUpdateRepository.NOTE_LANGUAGES.map(AppUpdateRepository::noteKey)
        return mapOf(INTERSTITIAL to true) + update.associateWith { "" }
    }
}
