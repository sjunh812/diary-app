package org.sjhstudio.diary.drivecheck

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// 드라이브 확인 화면(스펙 003 quickstart 1).
// 버튼을 순서대로 누르고 아래 기록을 research R1에 옮겨 적기
class MainActivity : ComponentActivity() {
    private var token: String? = null
    private lateinit var log: TextView

    private val consent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        runCatching { Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data) }
            .onSuccess { onAuthorized(it) }
            .onFailure { write("연결 실패: ${it.javaClass.simpleName} ${it.message}") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 120, 40, 40) }
        column.addView(TextView(this).apply { setText(R.string.title); textSize = 20f; setTypeface(typeface, Typeface.BOLD) })
        column.addView(button(R.string.connect) { connect() })
        column.addView(button(R.string.create) { withToken { create(it) } })
        column.addView(button(R.string.find) { withToken { find(it) } })
        column.addView(button(R.string.worker) { enqueueWorker() })
        column.addView(button(R.string.worker_result) { showWorkerResult() })
        column.addView(button(R.string.cleanup) { withToken { cleanup(it) } })
        column.addView(button(R.string.clear) { log.text = "" })
        log = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 12f; setTextIsSelectable(true); setPadding(0, 32, 0, 0) }
        column.addView(log)
        setContentView(ScrollView(this).apply { addView(column) })
        write("기기 ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, 패키지 $packageName")
    }

    private fun button(text: Int, onClick: () -> Unit) = Button(this).apply { setText(text); setOnClickListener { onClick() } }

    private fun connect() = lifecycleScope.launch {
        runCatching { Identity.getAuthorizationClient(this@MainActivity).authorize(DriveAuth.request).await() }
            .onSuccess { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    consent.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    onAuthorized(result)
                }
            }
            .onFailure { write("연결 실패: ${it.javaClass.simpleName} ${it.message}") }
    }

    private fun onAuthorized(result: AuthorizationResult) {
        token = result.accessToken
        write("받은 범위: ${result.grantedScopes.joinToString()}")
        withToken { t -> write("계정: ${DriveRest.userEmail(t)}") }
    }

    private fun create(token: String) {
        val folder = DriveRest.createFolder(token, "1일 1일기 백업 확인", mapOf("diary.kind" to "probe-root", "probe.client" to "android"))
        val file = DriveRest.createTextFile(
            token,
            folder.getString("id"),
            "android-made.txt",
            "Android 확인 빌드가 만든 파일",
            mapOf("diary.kind" to "probe-file", "probe.client" to "android"),
        )
        write("만듦: 폴더 ${folder.getString("name")}, 파일 ${file.getString("name")} (sha256 ${file.optString("sha256Checksum").take(12)})")
    }

    private fun find(token: String) {
        val probe = DriveRest.list(token, DriveAuth.PROBE_FILES)
        write("시험 파일 ${probe.size}개")
        probe.forEach { f ->
            val by = f.optJSONObject("appProperties")?.optString("probe.client") ?: "?"
            write("  · ${f.getString("name")} 만든 쪽 $by, ${f.optString("createdTime")}")
        }
        val legacy = DriveRest.list(token, DriveAuth.LEGACY_BACKUP)
        write("예전 앱 note.db ${legacy.size}개")
        legacy.forEach { f -> write("  · ${f.getString("name")} ${f.optString("size")}바이트, ${f.optString("createdTime")}") }
    }

    // 확인용으로 만든 폴더와 파일만 휴지통으로 이동
    // 예전 앱 note.db는 건드리지 않음
    private fun cleanup(token: String) {
        val targets = DriveRest.list(token, DriveAuth.PROBE_FILES) + DriveRest.list(token, DriveAuth.PROBE_ROOTS)
        targets.forEach { DriveRest.trash(token, it.getString("id")) }
        write("휴지통으로 옮김: ${targets.size}개(${targets.joinToString { it.getString("name") }})")
    }

    private fun enqueueWorker() {
        // 네트워크 조건이 없으면 백그라운드 작업에 네트워크가 열리지 않을 수 있어 연결됨 조건 추가
        val work = OneTimeWorkRequestBuilder<TokenWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueue(work)
        write("15초 뒤 작업 관리자가 돌아요. 홈으로 나가 20초쯤 기다린 뒤 다시 와서 5번을 눌러 주세요.")
    }

    private fun showWorkerResult() {
        val text = getSharedPreferences(TokenWorker.PREFS, Context.MODE_PRIVATE).getString(TokenWorker.KEY, null)
        write("작업 관리자 결과: ${text ?: "아직 없음"}")
    }

    private fun withToken(block: (String) -> Unit) {
        val t = token ?: return write("먼저 1번으로 연결해 주세요.")
        lifecycleScope.launch {
            val error = withContext(Dispatchers.IO) { runCatching { block(t) }.exceptionOrNull() }
            if (error != null) write("실패: ${error.message}")
        }
    }

    private fun write(line: String) = runOnUiThread { log.append(line + "\n") }
}
