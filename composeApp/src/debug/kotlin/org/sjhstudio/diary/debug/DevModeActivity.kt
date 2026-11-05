package org.sjhstudio.diary.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.get
import org.sjhstudio.diary.MainActivity
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsValueRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Plan

/**
 * 개발자 모드. 요금제마다 앱이 어떻게 달라지는지 결제 없이 본다.
 * 다음 단계가 있는 화면이 아니다. 고르면 바로 적용되고 [앱 열기]로 넘어가 확인한다.
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
                        DiarySettingsChoiceRow(each.label, each == plan, { plans.choose(each) }, description = each.summary)
                    }
                    DiarySettingsHeader("지금 켜진 것")
                    DiarySettingsValueRow("광고", if (plan.showsAds) "보임" else "안 보임")
                    DiarySettingsValueRow("한 편 사진", "${plan.photoLimit}장")
                    DiarySettingsValueRow("고급 통계", if (plan.advancedStats) "켜짐" else "꺼짐")
                    Spacer(Modifier.weight(1f))
                    // 요금제는 고르는 즉시 앱에 적용된다. 이 화면에서 바로 앱으로 넘어간다.
                    DiaryButton(
                        "앱 열기",
                        { startActivity(Intent(this@DevModeActivity, MainActivity::class.java)) },
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(DiarySpacing.x5)
                    )
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
