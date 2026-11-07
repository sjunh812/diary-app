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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlin.time.Clock
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.datastore.AdStore
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsValueRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 개발자 모드. 설정 맨 아래 줄로 들어오고 출시 빌드에는 없다.
 * 앱과 같은 화면 모드를 따라서 기기 설정과 달라도 앱과 같게 보인다.
 * 개발자만 보는 화면이라 문구를 리소스에 두지 않는다.
 */
class DevModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // adb로 --ez crash true를 넘기면 비정상 종료 보고가 콘솔에 닿는지 시험한다.
        if (intent.getBooleanExtra(EXTRA_CRASH, false)) crash()
        val plans = get<PlanRepository>() as DevPlanRepository
        val ads = get<AdStore>()
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
                val plan by plans.plan.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas).systemBarsPadding()) {
                    DiaryTopBar("개발자 모드", onBack = ::finish)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        DiarySettingsHeader("요금제")
                        Plan.entries.forEach { each ->
                            DiarySettingsChoiceRow(each.label, each == plan, { plans.choose(each) }, description = each.summary)
                        }
                        DiarySettingsHeader("지금 켜진 것", gapAbove = true)
                        DiarySettingsValueRow("광고", if (plan.showsAds) "보임" else "안 보임")
                        DiarySettingsValueRow("한 편 사진", "${plan.photoLimit}장")
                        DiarySettingsValueRow("체크리스트", if (plan.checklist) "켜짐" else "꺼짐")
                        DiarySettingsValueRow("고급 통계", if (plan.advancedStats) "켜짐" else "꺼짐")
                        DiarySettingsHeader("광고", gapAbove = true)
                        DiarySettingsRow("오늘 전면 광고 다시 받기", onClick = { reset { ads.interstitialShown("") } })
                        DiarySettingsRow("보상 광고 혜택 지우기", onClick = { reset { ads.rewardUntil(Clock.System.now()) } })
                        DiarySettingsHeader("화면 견본", gapAbove = true)
                        SCREENS.forEach { (name, screen) -> DiarySettingsRow(name, onClick = { open(screen) }) }
                        DiarySettingsHeader("시험", gapAbove = true)
                        DiarySettingsRow(
                            "비정상 종료 보내기",
                            onClick = ::crash,
                            description = "앱을 끝내고, 다시 열면 Crashlytics로 보고가 가요."
                        )
                    }
                }
            }
        }
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

    private fun crash() {
        window.decorView.post { error("Crashlytics 시험") }
    }

    private companion object {
        const val EXTRA_CRASH = "crash"
        val SCREENS =
            listOf(
                "개발자의 편지(봉투)" to "letter",
                "편지 다시 읽기" to "full_letter",
                "디자인 시스템 견본" to "design_catalog",
                "기록 이전 결과" to "migration_report",
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

private val Plan.summary: String
    get() = when (this) {
        Plan.FREE -> "광고가 보이고 사진은 한 편에 10장"
        Plan.AD_FREE -> "광고만 없어짐"
        Plan.PLUS -> "광고 없음, 사진 30장, 체크리스트, 고급 통계"
    }
