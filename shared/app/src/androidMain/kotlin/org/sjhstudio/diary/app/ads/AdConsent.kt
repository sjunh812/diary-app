package org.sjhstudio.diary.app.ads

import android.app.Application
import android.content.pm.ApplicationInfo
import android.provider.Settings
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.core.data.AdPrivacy

/**
 * 광고 동의(Google UMP, `docs/business.md` 광고 동의)
 * 동의가 필요한지는 Google이 접속 지역으로 정해 EEA·영국·스위스에서만 창이 뜸. 그 밖의 지역은 창도 설정 줄도 없음
 * 화면 스레드에서만 부름
 */
class AdConsent(private val app: Application, private val current: CurrentActivity) : AdPrivacy {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private var updated = false
    private var updating = false
    private val afterUpdate = mutableListOf<() -> Unit>()
    private var formWaiters: MutableList<() -> Unit>? = null

    private val required = MutableStateFlow(false)
    override val choicesRequired: StateFlow<Boolean> = required.asStateFlow()

    /** 지난번 답이 남아 있으면 앱을 새로 켜도 바로 true */
    val canRequestAds: Boolean get() = info.canRequestAds()

    /**
     * 앱을 켤 때마다 한 번 동의 정보를 새로 받은 뒤 [then]
     * 받지 못해도 지난번 답으로 이어 감. 화면이 아직 없으면 다음 요청 때 다시 물음
     */
    fun afterUpdate(then: () -> Unit) {
        if (updated) return then()
        afterUpdate += then
        val screen = current.value
        if (!updating && screen != null) {
            updating = true
            info.requestConsentInfoUpdate(screen, parameters(), { finishUpdate() }, { finishUpdate() })
        }
    }

    private fun finishUpdate() {
        updating = false
        updated = true
        refresh()
        val works = afterUpdate.toList()
        afterUpdate.clear()
        works.forEach { it() }
    }

    /** 아직 답하지 않았으면 동의 창을 띄우고 닫힌 뒤 [then]. 필요 없으면 바로 [then] */
    fun showFormIfRequired(then: () -> Unit) {
        formWaiters?.let {
            it += then
            return
        }
        val screen = current.value ?: return then()
        formWaiters = mutableListOf(then)
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(screen) {
            refresh()
            val waiters = formWaiters.orEmpty()
            formWaiters = null
            waiters.forEach { it() }
        }
    }

    override fun showChoices() {
        val screen = current.value ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(screen) { refresh() }
    }

    private fun refresh() {
        required.value = info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * 개발 빌드는 `adb shell settings put global diary_ump_eea <시험 기기 ID>`면 유럽에 있는 것처럼 시험
     * 시험 기기 ID는 UMP가 처음 물을 때 로그에 찍어 줌
     */
    private fun parameters(): ConsentRequestParameters {
        val debuggable = app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val testDevice = Settings.Global.getString(app.contentResolver, DEBUG_EEA).orEmpty().takeIf { debuggable && it.length > 1 }
        if (testDevice != null) info.reset()
        return ConsentRequestParameters.Builder().apply {
            if (testDevice != null) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(app)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .addTestDeviceHashedId(testDevice)
                        .build()
                )
            }
        }.build()
    }

    private companion object {
        const val DEBUG_EEA = "diary_ump_eea"
    }
}
