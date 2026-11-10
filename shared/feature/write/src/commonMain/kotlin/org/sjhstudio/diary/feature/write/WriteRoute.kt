package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import org.sjhstudio.diary.core.model.richtext.hasWords
import org.sjhstudio.diary.core.model.richtext.keepingEditorText
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.rememberFileFontFamily

/**
 * 쓰기 진입점.
 * 어디로 갈지는 앱 틀이 정한다.
 * [onSaved]는 일기 ID를, [onClosed]는 임시 저장을 남겼는지를 받는다.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WriteRoute(
    target: WriteTarget,
    onSaved: (String) -> Unit,
    onClosed: (keptDraft: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onPlus: () -> Unit = {},
    injected: WriteViewModel? = null
) {
    // 잠금이나 앱 종료로 화면 모델이 사라져도 쓰던 글을 그대로 잇는다.
    // 이게 없으면 쓰던 화면인데 "쓰던 일기가 있어요"를 다시 묻는다.
    var draftKey by rememberSaveable { mutableStateOf<String?>(null) }
    val viewModel =
        injected ?: koinViewModel<WriteViewModel>(key = target.viewModelKey()) { parametersOf(target, RestoredDraftKey(draftKey)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.draftKey) { state.draftKey?.let { draftKey = it } }
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
    DisposableEffect(rich, colors) {
        viewModel.attachEditor { keepingEditorText(colors.fromEditor(rich.toHtml()), rich.annotatedString.text) }
        onDispose(viewModel::detachEditor)
    }
    // 편집기는 화면을 돌려도 스스로 살아남는다.
    // 받은 본문을 다시 넣으면 그 뒤에 친 글이 사라지니, 넣은 판을 함께 기억한다.
    // 테마가 바뀌면 편집기 안의 색을 저장 값으로 되돌릴 수 없어서 그때는 지금 본문을 다시 넣는다.
    var applied by rememberSaveable { mutableStateOf("") }
    val wanted = "${state.bodyVersion}/${colors.signature}"
    LaunchedEffect(wanted) {
        if (state.bodyVersion > 0 && applied != wanted) {
            rich.setHtml(colors.toEditor(viewModel.currentBody()))
            // 편집기가 다시 낸 모양을 기준으로 삼는다.
            viewModel.onBodyLoaded(colors.fromEditor(rich.toHtml()))
            applied = wanted
        }
    }
    // HTML 변환은 쓰기 직전으로 미룬다.
    // 여기서는 바뀌었다는 표시만 한다.
    LaunchedEffect(rich) {
        snapshotFlow { rich.annotatedString }.drop(1).collect { viewModel.onEditorChanged(hasWords(it.text)) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    BackHandler { viewModel.close() }

    val family = rememberFileFontFamily(look.fontPath)
    WriteScreen(
        state = state,
        actions = remember(viewModel, rich, colors) { WriteActions(viewModel, rich, colors, viewModel::mediaPath, onPlus) },
        bodyStyle = diaryBodyTextStyle(look.font, look.size) { family },
        modifier = modifier
    )
}

/**
 * 화면 모델을 일기마다 따로 두는 키.
 * 앱 틀이 저장소를 나눠 주지 못하는 때도 다른 일기의 화면 모델을 받지 않게 한다.
 */
private fun WriteTarget.viewModelKey(): String = when (this) {
    is WriteTarget.Edit -> "write:edit:$entryId"
    is WriteTarget.New -> "write:new:$date"
}
