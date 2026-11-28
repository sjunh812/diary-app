package org.sjhstudio.diary.app.ads

import android.app.Application
import android.content.pm.ApplicationInfo
import android.provider.Settings
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
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
 * 창은 미리 받아 두고 [gate]가 띄울 때를 정함
 * 화면 스레드에서만 부름
 */
class AdConsent(private val app: Application, private val current: CurrentActivity) : AdPrivacy {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private var updated = false
    private var updating = false
    private val afterUpdate = mutableListOf<() -> Unit>()
    private var form: ConsentForm? = null

    private val required = MutableStateFlow(false)
    override val choicesRequired: StateFlow<Boolean> = required.asStateFlow()

    /** 지난번 답이 남아 있으면 앱을 새로 켜도 바로 true */
    val canRequestAds: Boolean get() = info.canRequestAds()

    private val allowed = MutableStateFlow(info.canRequestAds())

    /** 광고를 받을 수 있는지. 동의 창에 답하면 바뀌어 목록 광고 자리가 다시 받음 */
    val adsAllowed: StateFlow<Boolean> = allowed.asStateFlow()

    val gate =
        ConsentFormGate(
            needed = { info.consentStatus == ConsentInformation.ConsentStatus.REQUIRED },
            load = ::loadForm,
            present = { onClosed ->
                val ready = form
                val screen = current.value
                if (ready == null || screen == null) {
                    onClosed(false)
                } else {
                    ready.show(screen) {
                        form = null
                        refresh()
                        onClosed(true)
                    }
                }
            }
        )

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
        gate.prepare()
        val works = afterUpdate.toList()
        afterUpdate.clear()
        works.forEach { it() }
    }

    /** 창을 받음. 언제 받고 띄울지는 [gate]. 답할 차례가 아니거나 창이 없으면 실패로 끝냄 */
    private fun loadForm() {
        if (info.consentStatus != ConsentInformation.ConsentStatus.REQUIRED || !info.isConsentFormAvailable) return gate.formFailed()
        UserMessagingPlatform.loadConsentForm(
            app,
            { loaded ->
                form = loaded
                gate.formReady()
            },
            { gate.formFailed() }
        )
    }

    override fun showChoices() {
        val screen = current.value ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(screen) { refresh() }
    }

    private fun refresh() {
        allowed.value = info.canRequestAds()
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
