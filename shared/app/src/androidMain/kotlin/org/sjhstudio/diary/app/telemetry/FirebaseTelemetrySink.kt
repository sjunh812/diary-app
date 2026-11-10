package org.sjhstudio.diary.app.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.TelemetrySink

/**
 * 고정 표를 지난 이벤트와 키만 받음([org.sjhstudio.diary.core.platform.Telemetry]).
 * 일기 내용은 여기까지 오지 않음(헌법 II).
 */
private class FirebaseTelemetrySink(context: Context) : TelemetrySink {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(event: String, params: Map<String, String>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            bundle.putString(
                if (event ==
                    AnalyticsEvents.SCREEN_VIEW
                ) {
                    FirebaseAnalytics.Param.SCREEN_NAME
                } else {
                    key
                },
                value
            )
        }
        analytics.logEvent(event, bundle)
    }

    override fun setCustomKey(key: String, value: String) = crashlytics.setCustomKey(key, value)
}

/** `google-services.json` 없이 만든 빌드는 Firebase가 꺼져 있어 아무것도 보내지 않음 */
fun telemetrySink(context: Context): TelemetrySink =
    if (FirebaseApp.getApps(context).isEmpty()) NoTelemetrySink else FirebaseTelemetrySink(context)
