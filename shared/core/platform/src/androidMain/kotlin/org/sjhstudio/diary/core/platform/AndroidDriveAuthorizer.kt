package org.sjhstudio.diary.core.platform

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.edit
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.sjhstudio.diary.core.model.BackupFailure

/** 동의 창을 띄우는 쪽. 화면이 있는 `MainActivity`가 건다. 백그라운드에서는 비어 있다. */
fun interface DriveConsentLauncher {
    /** 동의 창을 띄우고 돌아온 결과를 준다. 취소하면 null이다. */
    suspend fun launch(intent: PendingIntent): Intent?
}

/**
 * `AuthorizationClient`로 `drive.file`만 받는다(research R2). 토큰은 기기에 두지 않고 1시간짜리를 그때그때 받는다.
 * 응답에 계정이 없어 받은 토큰으로 userinfo를 불러 안다. 계정 이메일은 로그에 남기지 않는다.
 */
class AndroidDriveAuthorizer(context: Context) : DriveAuthorizer {
    private val context = context.applicationContext
    private val client = Identity.getAuthorizationClient(this.context)
    private val prefs = this.context.getSharedPreferences("drive_auth", Context.MODE_PRIVATE)
    private var cached: Pair<String, DriveAccount>? = null

    /** 사진마다 Play 서비스를 부르면 그것만으로 한 장에 시간이 든다. 받은 토큰을 잠깐 다시 쓴다. */
    @Volatile
    private var fresh: Pair<Long, TokenResult.Token>? = null

    var consent: DriveConsentLauncher? = null

    override suspend fun connect(hint: String?): ConnectResult = try {
        fresh = null
        val first = client.authorize(request(hint)).await()
        val result =
            if (first.hasResolution()) {
                val launcher = consent ?: return ConnectResult.Failed(BackupFailure.UNKNOWN)
                val data = launcher.launch(requireNotNull(first.pendingIntent)) ?: return ConnectResult.Cancelled
                client.getAuthorizationResultFromIntent(data)
            } else {
                first
            }
        val account = account(requireNotNull(result.accessToken))
        prefs.edit { putString(KEY_EMAIL, account.email) }
        ConnectResult.Connected(account)
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        if (error.statusCode == CommonStatusCodes.CANCELED) ConnectResult.Cancelled else ConnectResult.Failed(error.kind())
    } catch (_: IOException) {
        ConnectResult.Failed(BackupFailure.OFFLINE)
    }

    override suspend fun accessToken(): TokenResult = try {
        val email = prefs.getString(KEY_EMAIL, null) ?: return TokenResult.NeedsUser
        fresh?.takeIf { it.first > SystemClock.elapsedRealtime() }?.let { return it.second }
        val result = client.authorize(request(email)).await()
        val token = result.accessToken
        if (result.hasResolution() || token == null) {
            TokenResult.NeedsUser
        } else {
            TokenResult.Token(token, account(token)).also { fresh = SystemClock.elapsedRealtime() + TOKEN_REUSE_MILLIS to it }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        TokenResult.Failed(error.kind())
    } catch (_: IOException) {
        TokenResult.Failed(BackupFailure.OFFLINE)
    }

    override fun invalidateToken() {
        fresh = null
    }

    override suspend fun disconnect() {
        cached = null
        fresh = null
        prefs.edit { remove(KEY_EMAIL) }
    }

    private fun request(email: String?): AuthorizationRequest {
        val builder = AuthorizationRequest.builder().setRequestedScopes(SCOPES)
        email?.let { builder.setAccount(Account(it, "com.google")) }
        return builder.build()
    }

    private suspend fun account(token: String): DriveAccount {
        cached?.takeIf { it.first == token }?.let { return it.second }
        return withContext(Dispatchers.IO) {
            val connection = URL(USER_INFO).openConnection() as HttpURLConnection
            try {
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("userinfo ${connection.responseCode}")
                val body = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                DriveAccount(body.getString("sub"), body.getString("email")).also { cached = token to it }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun ApiException.kind() = if (statusCode == CommonStatusCodes.NETWORK_ERROR) BackupFailure.OFFLINE else BackupFailure.AUTH

    private suspend fun Task<AuthorizationResult>.await(): AuthorizationResult = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }

    private companion object {
        val SCOPES = listOf(Scope("https://www.googleapis.com/auth/drive.file"), Scope("openid"), Scope("email"))
        const val USER_INFO = "https://openidconnect.googleapis.com/v1/userinfo"
        const val KEY_EMAIL = "email"
        const val TOKEN_REUSE_MILLIS = 5 * 60 * 1_000L
    }
}
