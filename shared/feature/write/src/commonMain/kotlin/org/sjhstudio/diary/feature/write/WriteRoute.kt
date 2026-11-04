package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlinx.coroutines.flow.drop
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.rememberFileFontFamily

/**
 * 쓰기 진입점. 어디로 갈지는 앱 틀이 정한다.
 * [onSaved]는 일기 ID를, [onClosed]는 임시 저장을 남겼는지를 받는다.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WriteRoute(
    target: WriteTarget,
    onSaved: (String) -> Unit,
    onClosed: (keptDraft: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WriteViewModel = koinViewModel { parametersOf(target) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val rich = rememberRichTextState()
    val theme = DiaryTheme.colors
    val colors = remember(theme) { EditorColors(theme) }
    val saved by rememberUpdatedState(onSaved)
    val haptic = LocalHapticFeedback.current
    val closed by rememberUpdatedState(onClosed)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WriteEvent.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    saved(event.entryId)
                }

                is WriteEvent.Closed -> closed(event.keptDraft)

                WriteEvent.Discarded -> closed(false)
            }
        }
    }
    // 불러온 본문을 넣고, 편집기가 다시 낸 모양을 기준으로 삼는다.
    LaunchedEffect(state.bodyVersion) {
        if (state.bodyVersion > 0) {
            rich.setHtml(colors.toEditor(state.initialBody))
            viewModel.onBodyLoaded(colors.fromEditor(rich.toHtml()))
        }
    }
    LaunchedEffect(rich) {
        snapshotFlow { rich.annotatedString }.drop(1).collect { viewModel.onBodyChange(colors.fromEditor(rich.toHtml())) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    BackHandler { viewModel.close() }

    val family = rememberFileFontFamily(look.fontPath)
    WriteScreen(
        state = state,
        actions = remember(viewModel, rich, colors) { WriteActions(viewModel, rich, colors, viewModel::mediaPath) },
        bodyStyle = diaryBodyTextStyle(look.font, look.size) { family },
        modifier = modifier
    )
}
