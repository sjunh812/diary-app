package org.sjhstudio.diary.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.get
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Plan

/**
 * 개발자 모드. 요금제마다 앱이 어떻게 달라지는지 결제 없이 본다.
 * `src/debug`에만 있어 출시 빌드에는 코드도 화면도 들어가지 않는다. 사용자에게 보이지 않으므로 문구를 리소스에 두지 않는다.
 */
class DevModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val plans = get<PlanRepository>() as DevPlanRepository
        setContent {
            DiaryTheme(darkTheme = isSystemInDarkTheme()) {
                val plan by plans.plan.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding()) {
                    DiaryTopBar("개발자 모드", onBack = ::finish)
                    DiarySettingsHeader("요금제")
                    Plan.entries.forEach { each ->
                        DiarySettingsRow(each.label, { plans.choose(each) }, description = each.summary, selected = each == plan)
                    }
                    DiarySettingsHeader("지금 켜진 것")
                    DiarySettingsRow("광고", {}, value = if (plan.showsAds) "보임" else "안 보임")
                    DiarySettingsRow("한 편 사진", {}, value = "${plan.photoLimit}장")
                    DiarySettingsRow("고급 통계", {}, value = if (plan.advancedStats) "켜짐" else "꺼짐")
                }
            }
        }
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
        Plan.PLUS -> "광고 없음, 사진 30장, 고급 통계"
    }
