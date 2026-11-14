package org.sjhstudio.diary.drivecheck

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 스펙 003 research O4: 작업 관리자 안에서 화면 없이 토큰을 받을 수 있는지 확인
// 결과는 기기 안 SharedPreferences에만 기록. 토큰은 기록하지 않음
class TokenWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val at = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date())
        val message = try {
            val result = Identity.getAuthorizationClient(applicationContext).authorize(DriveAuth.request).await()
            val token = result.accessToken
            when {
                result.hasResolution() -> "$at 화면이 필요함(PendingIntent). 백그라운드에서는 토큰을 못 받음"
                token == null -> "$at 토큰 없음"
                else -> {
                    val count = DriveRest.list(token, DriveAuth.PROBE_FILES).size
                    "$at 화면 없이 토큰 받음, 시험 파일 ${count}개 조회 성공"
                }
            }
        } catch (e: Exception) {
            "$at 실패: ${e.javaClass.simpleName} ${e.message?.take(200)}"
        }
        applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, message).apply()
        return Result.success()
    }

    companion object {
        const val PREFS = "drive_check"
        const val KEY = "worker_result"
    }
}
