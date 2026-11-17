package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.designsystem.component.DiaryTip
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.tip_close

/**
 * [tip]을 처음 만날 때 한 번 보임
 * 안내마다 따로, 닫거나 할 일을 누를 때까지 보임(스펙 004 FR-011)
 * [action]을 누르면 할 일을 한 것이라 다시 띄우지 않음(Apple TipKit `actionPerformed`)
 * 보일지는 [ContextTips]가 쥐고, 화면에 들어올 때 한 번 정함
 * 밖에서 끝내도 떠날 때까지 둠
 * 다른 화면으로 넘어가는 동안 아래 내용이 들썩이지 않게 하려는 것
 */
@Composable
fun ContextTipBar(tip: ContextTip, text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val tips = remember { KoinPlatform.getKoinOrNull()?.getOrNull<ContextTips>() } ?: return
    var showing by remember { mutableStateOf(tips.isOpen(tip)) }
    LaunchedEffect(tips) {
        // 보통은 시작 단계가 읽어둠
        // 아직이면 읽은 뒤 한 번 정함
        if (showing == null) {
            tips.load()
            showing = tips.isOpen(tip) == true
        }
    }
    val finish = {
        showing = false
        tips.done(tip)
    }
    if (showing == true) {
        // 보인 날에는 다른 팁을 띄우지 않음
        LaunchedEffect(tip) { tips.shown(tip) }
        DiaryTip(
            text,
            stringResource(Res.string.tip_close),
            onClose = { finish() },
            modifier = modifier,
            action = action,
            onAction = {
                finish()
                onAction()
            }
        )
    }
}

/**
 * 같은 자리 후보 가운데 지금 열린 첫 팁(스펙 008 FR-011)
 * 한 자리에 팁을 여럿 두면 모두 그날 첫 팁으로 보여, 하나만 고른 뒤 그림
 */
@Composable
fun rememberFirstOpenTip(candidates: List<ContextTip>, today: LocalDate = rememberToday()): ContextTip? {
    val tips = remember { KoinPlatform.getKoinOrNull()?.getOrNull<ContextTips>() } ?: return null
    // 팁이 열리거나(잘린 일기 세 번) 닫히면, 날이 바뀌면 다시 고름
    val revision by tips.revision.collectAsState()
    return remember(candidates, revision, today) { candidates.firstOrNull { tips.isOpen(it) == true } }
}
