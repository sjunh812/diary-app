package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.core.designsystem.component.DiaryTip
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.tip_close

/**
 * [tip]을 처음 만날 때 한 번 보임
 * 안내마다 따로, 닫거나 할 일을 누를 때까지 보임(스펙 004 FR-011)
 * [action]을 누르면 할 일을 한 것이라 다시 띄우지 않음(Apple TipKit `actionPerformed`)
 */
@Composable
fun ContextTipBar(tip: ContextTip, text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val tips = remember { KoinPlatform.getKoinOrNull()?.getOrNull<ContextTips>() } ?: return
    var showing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tip) { showing = tips.claim(tip) }
    if (showing) {
        val finish = {
            showing = false
            scope.launchSafely { tips.done(tip) }
        }
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
