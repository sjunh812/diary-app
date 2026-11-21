package org.sjhstudio.diary.debug

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySheetChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.platform.AppFiles
import org.sjhstudio.diary.core.platform.BackupAlertPoster
import org.sjhstudio.diary.helper.AlarmReceiver

/**
 * 개발자 모드. 설정 맨 아래 줄로 들어오고 출시 빌드에는 없음
 * 앱과 같은 화면 모드를 따라 기기 설정과 달라도 앱과 같게 보임
 * 개발자만 보는 화면이라 문구를 리소스에 두지 않음
 */
class DevModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // adb로 --ez crash true를 넘기면 비정상 종료 보고가 콘솔에 닿는지 시험
        if (intent.getBooleanExtra(EXTRA_CRASH, false)) crash()
        // adb로 --es notify reminder|backup을 넘기면 실제 알림을 띄우고 닫음. 알림으로 열 때 화면이 쌓이는지 시험
        intent.getStringExtra(EXTRA_NOTIFY)?.let {
            notify(it)
            finish()
            return
        }
        val plans = get<PlanRepository>() as DevPlanRepository
        val updates = get<DevStoreUpdates>()
        val themeMode = get<SettingsRepository>().settings.map { it.themeMode }
        setContent {
            val mode by themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dark =
                when (mode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            DiaryTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas).systemBarsPadding()) {
                    DiaryTopBar("개발자 모드", onBack = ::finish)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        StateSection(plans, updates)
                        ResetSection()
                        ScreenSection()
                        TestSection()
                    }
                }
            }
        }
    }

    /** 앱이 그 상태인 것처럼 보이게. 앱으로 돌아가면 바뀜 */
    @Composable
    private fun StateSection(plans: DevPlanRepository, updates: DevStoreUpdates) {
        val plan by plans.plan.collectAsStateWithLifecycle()
        val update by updates.mode.collectAsStateWithLifecycle()
        DiarySettingsHeader("상태")
        ChoiceRow("요금제", Plan.entries, plan, Plan::label, Plan::summary, plans::choose)
        ChoiceRow("새 버전", DevUpdate.entries, update, DevUpdate::label, DevUpdate::summary, updates::choose)
    }

    /** 한 번만 뜨는 것을 다시 띄움 */
    @Composable
    private fun ResetSection() {
        val ads = get<AdStore>()
        DiarySettingsHeader("초기화", gapAbove = true)
        DiarySettingsRow("전면 광고", onClick = { reset { ads.interstitialShown("") } }, description = "오늘 본 기록 지움")
        DiarySettingsRow("보상 광고", onClick = { reset { ads.rewardUntil(Clock.System.now()) } }, description = "24시간 혜택 끝냄")
    }

    @Composable
    private fun ScreenSection() {
        DiarySettingsHeader("화면", gapAbove = true)
        SCREENS.forEach { (name, screen) -> DiarySettingsRow(name, onClick = { open(screen) }) }
    }

    @Composable
    private fun TestSection() {
        DiarySettingsHeader("시험", gapAbove = true)
        DiarySettingsRow(
            "일기 1만 편 넣기",
            onClick = { reset { withContext(Dispatchers.IO) { writeSample(get<DiaryDatabase>(), File(get<AppFiles>().mediaDirectory)) } } },
            description = "서식 20% · 긴 일기 5% · 사진 5천 장"
        )
        DiarySettingsRow("비정상 종료", onClick = ::crash, description = "Crashlytics 보고 시험")
    }

    private fun reset(work: suspend () -> Unit) {
        lifecycleScope.launch {
            work()
            Toast.makeText(this@DevModeActivity, "됐어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun open(screen: String) {
        startActivity(Intent(this, ScreenCatalogActivity::class.java).putExtra(ScreenCatalogActivity.EXTRA_SCREEN, screen))
    }

    private fun notify(kind: String) {
        when (kind) {
            "reminder" -> AlarmReceiver().showReminder(this)
            "backup" -> BackupAlertPoster(this, "백업").show("백업 알림 시험", "누르면 앱이 열려요")
        }
    }

    private fun crash() {
        window.decorView.post { error("Crashlytics 시험") }
    }

    private companion object {
        const val EXTRA_CRASH = "crash"
        const val EXTRA_NOTIFY = "notify"
        val SCREENS =
            listOf(
                "편지 · 봉투" to "letter",
                "편지 · 다시 읽기" to "full_letter",
                "온보딩 · 업데이트" to "onboarding_updated",
                "온보딩 · 새 설치" to "onboarding_new",
                "기록 이전 결과" to "migration_report",
                "디자인 시스템" to "design_catalog",
                "1만 편 목록" to "list_performance"
            )
    }
}

private val Plan.label: String
    get() = when (this) {
        Plan.FREE -> "무료"
        Plan.AD_FREE -> "광고 제거"
        Plan.PLUS -> "Plus"
    }

/** 그 요금제에서 켜지는 것. 요금제 표에서 읽어 어긋나지 않음 */
private val Plan.summary: String
    get() = listOfNotNull(
        if (showsAds) "광고 보임" else "광고 없음",
        "사진 ${photoLimit}장",
        "체크리스트".takeIf { checklist },
        "고급 통계".takeIf { advancedStats },
        "목록 서식".takeIf { listFormatting }
    ).joinToString(" · ")

/** 지금 값과 설명을 한 줄에 두고, 누르면 시트에서 고름 */
@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    summary: (T) -> String,
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    DiarySettingsRow(title, onClick = { open = true }, value = label(selected), description = summary(selected))
    if (open) {
        DiaryBottomSheet(onDismiss = { open = false }, title = title, description = "앱으로 돌아가면 적용") {
            Column(Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    DiarySheetChoiceRow(label(option), option == selected, onClick = {
                        onSelect(option)
                        open = false
                    }, description = summary(option))
                }
            }
        }
    }
}
