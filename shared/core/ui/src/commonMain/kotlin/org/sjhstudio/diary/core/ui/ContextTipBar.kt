package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import org.sjhstudio.diary.core.designsystem.component.DiaryTip
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.ContextTips
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.tip_close

/**
 * [tip]을 처음 만날 때 한 번 보인다.
 * 세션당 하나만 뜬다(스펙 004 FR-011).
 */
@Composable
fun ContextTipBar(tip: ContextTip, text: String, modifier: Modifier = Modifier) {
    val tips = remember { KoinPlatform.getKoinOrNull()?.getOrNull<ContextTips>() } ?: return
    var showing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tip) { showing = tips.claim(tip) }
    if (showing) {
        DiaryTip(text, stringResource(Res.string.tip_close), onClose = {
            showing = false
            scope.launch { tips.done(tip) }
        }, modifier)
    }
}
