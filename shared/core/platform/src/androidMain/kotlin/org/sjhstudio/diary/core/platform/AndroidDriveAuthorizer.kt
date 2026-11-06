package org.sjhstudio.diary.core.platform

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import org.json.JSONException
import org.json.JSONObject
import org.sjhstudio.diary.core.model.BackupFailure

/**
 * 동의 창을 띄우는 쪽.
 * 화면이 있는 `MainActivity`가 건다.
 * 백그라운드에서는 비어 있다.
 */
fun interface DriveConsentLauncher {
    /**
     * 동의 창을 띄우고 돌아온 결과를 준다.
     * 취소하면 null이다.
     */
    suspend fun launch(intent: PendingIntent): Intent?
}

/**
 * `AuthorizationClient`로 `drive.file`만 받는다(research R2).
 * 토큰은 기기에 두지 않고 1시간짜리를 그때그때 받는다.
 * 응답에 계정이 없어 받은 토큰으로 userinfo를 불러 안다.
 * 계정 이메일은 로그에 남기지 않는다.
 */
class AndroidDriveAuthorizer(context: Context) : DriveAuthorizer {
    private val context = context.applicationContext
    private val client = Identity.getAuthorizationClient(this.context)
    private val prefs = this.context.getSharedPreferences("drive_auth", Context.MODE_PRIVATE)
    private val tokens = DriveTokenCache(fetch = ::fetchToken)

    var consent: DriveConsentLauncher? = null

    override suspend fun connect(hint: String?): ConnectResult = try {
        tokens.clear()
        val first = client.authorize(request(hint)).await()
        val result =
            if (first.hasResolution()) {
                val launcher = consent ?: return ConnectResult.Failed(BackupFailure.UNKNOWN)
                val data = launcher.launch(requireNotNull(first.pendingIntent)) ?: return ConnectResult.Cancelled
                client.getAuthorizationResultFromIntent(data)
            } else {
                first
            }
        val account = account(result.accessToken ?: return ConnectResult.Failed(BackupFailure.AUTH))
        prefs.edit { putString(KEY_EMAIL, account.email) }
        // 연결하는 사이에 옛 계정으로 받은 토큰이 남아 있을 수 있다.
        tokens.clear()
        ConnectResult.Connected(account)
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        if (error.statusCode == CommonStatusCodes.CANCELED) ConnectResult.Cancelled else ConnectResult.Failed(error.kind())
    } catch (_: IOException) {
        ConnectResult.Failed(BackupFailure.OFFLINE)
    }

    override suspend fun accessToken(): TokenResult = tokens.get()

    private suspend fun fetchToken(): TokenResult = try {
        val email = prefs.getString(KEY_EMAIL, null) ?: return TokenResult.NeedsUser
        val result = client.authorize(request(email)).await()
        val token = result.accessToken
        if (result.hasResolution() || token == null) TokenResult.NeedsUser else TokenResult.Token(token, account(token))
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        TokenResult.Failed(error.kind())
    } catch (_: IOException) {
        TokenResult.Failed(BackupFailure.OFFLINE)
    }

    override fun invalidateToken() = tokens.clear()

    override suspend fun disconnect() {
        tokens.clear()
        prefs.edit { remove(KEY_EMAIL) }
        // 구글 쪽 권한은 거두지 않는다.
        // drive.file 권한을 거두면 다시 연결해도 앱이 만든 백업이 보이지 않을 수 있어(헌법 I), 확인하기 전까지 이 기기의 연결만 끊는다.
    }

    private fun request(email: String?): AuthorizationRequest {
        val builder = AuthorizationRequest.builder().setRequestedScopes(SCOPES)
        email?.let { builder.setAccount(Account(it, ACCOUNT_TYPE)) }
        return builder.build()
    }

    private suspend fun account(token: String): DriveAccount = withContext(Dispatchers.IO) {
        val connection = URL(USER_INFO).openConnection() as HttpURLConnection
        try {
            // 응답이 없어도 백업이 잠금을 쥔 채 머물지 않는다.
            connection.connectTimeout = HTTP_TIMEOUT_MILLIS
            connection.readTimeout = HTTP_TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("userinfo ${connection.responseCode}")
            val body = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            DriveAccount(body.getString("sub"), body.getString("email"))
        } catch (_: JSONException) {
            // 공용 와이파이의 안내 페이지가 올 수 있다.
            // 응답 내용은 메시지에 넣지 않는다.
            throw IOException("userinfo body")
        } finally {
            connection.disconnect()
        }
    }

    private fun ApiException.kind() = if (statusCode == CommonStatusCodes.NETWORK_ERROR) BackupFailure.OFFLINE else BackupFailure.AUTH

    private suspend fun Task<AuthorizationResult>.await(): AuthorizationResult = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        // 코루틴 취소로 넘기면 부른 쪽은 멈춘 줄로 알고 결과를 받지 못한다.
        // 실패로 돌려준다.
        addOnCanceledListener { continuation.resumeWithException(IOException("authorization canceled")) }
    }

    private companion object {
        val SCOPES = listOf(Scope("https://www.googleapis.com/auth/drive.file"), Scope("openid"), Scope("email"))
        const val USER_INFO = "https://openidconnect.googleapis.com/v1/userinfo"
        const val KEY_EMAIL = "email"
        const val ACCOUNT_TYPE = "com.google"
        const val HTTP_TIMEOUT_MILLIS = 15_000
    }
}
