package org.sjhstudio.diary.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * 지금 앞에 있는 화면. 결제 창과 전면 광고를 띄울 때 씀
 * 첫 화면이 뜨기 전에 등록해야 놓치지 않아 앱이 켜질 때 만듦
 */
class CurrentActivity(app: Application) {
    var value: Activity? = null
        private set

    init {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    value = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (value === activity) value = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }
}
