package org.sjhstudio.diary.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * 시스템 설정 화면 열기
 * 그 화면이 없는 기기는 앱 정보 화면으로 보냄. 둘 다 없으면 아무 일도 하지 않음
 */
fun Context.openSystemSettings(intent: Intent) {
    if (!start(intent)) openAppInfo()
}

fun Context.openAppInfo() {
    start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

private fun Context.start(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}
