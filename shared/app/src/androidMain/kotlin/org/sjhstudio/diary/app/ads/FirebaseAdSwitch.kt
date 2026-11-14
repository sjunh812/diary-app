package org.sjhstudio.diary.app.ads

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.sjhstudio.diary.core.data.AdSwitch

private const val KEY = "interstitial_enabled"

/** 무료 한도를 넘기지 않게 값은 12시간에 한 번만 받음 */
private const val FETCH_SECONDS = 12 * 60 * 60L

/**
 * Firebase 설정 파일 없이 만든 빌드는 늘 켜짐
 * 받기에 실패하면 마지막 값을 씀
 */
fun adSwitch(context: Context): AdSwitch {
    if (FirebaseApp.getApps(context).isEmpty()) return AdSwitch { true }
    val config = FirebaseRemoteConfig.getInstance()
    config.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(FETCH_SECONDS).build())
    config.setDefaultsAsync(mapOf(KEY to true))
    config.fetchAndActivate()
    return AdSwitch { config.getBoolean(KEY) }
}
