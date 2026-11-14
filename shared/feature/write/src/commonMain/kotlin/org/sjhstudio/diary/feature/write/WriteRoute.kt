package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.richtext.hasWords
import org.sjhstudio.diary.core.model.richtext.keepingEditorText
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.rememberFileFontFamily

/**
 * 쓰기 진입점. 어디로 갈지는 앱 틀이 정함
 * [onSaved]는 일기 ID를, [onClosed]는 임시 저장을 남겼는지를 받음
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
    // 잠금이나 앱 종료로 화면 모델이 사라져도 쓰던 글을 그대로 이음
    // 없으면 쓰던 화면인데 "쓰던 일기가 있어요"를 다시 물음
    var draftKey by rememberSaveable { mutableStateOf<String?>(null) }
    val viewModel =
        injected ?: koinViewModel<WriteViewModel>(key = target.viewModelKey()) { parametersOf(target, RestoredDraftKey(draftKey)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.draftKey) { state.draftKey?.let { draftKey = it } }
    val look by viewModel.look.collectAsStateWithLifecycle()
    val theme = DiaryTheme.colors
    val colors = remember(theme) { EditorColors(theme) }
    // 본문의 주인은 화면 모델(임시 저장본) 하나. 편집기는 화면 저장 상태에 글을 넣지 않음
    // 넣으면 앱이 종료됐다 돌아올 때 임시 저장본과 다른 글이 보이고, 아주 긴 글은 저장 상태 크기 한도를 넘음
    // 커서 자리만 저장해 회전 뒤에도 쓰던 자리에서 이어 씀
    var caret by rememberSaveable { mutableIntStateOf(-1) }
    val wanted = "${state.bodyVersion}/${colors.signature}"
    // 화면 모델이 이미 본문을 갖고 있으면(회전) 처음 그릴 때부터 글을 넣음
    // 뒤에 넣으면 커서를 다시 잡는 순간 글이 아직 배치되지 않아 커서 자리가 글 밖이 됨
    var applied by remember { mutableStateOf("") }
    val rich = remember {
        RichTextState().also { editor ->
            if (state.bodyVersion > 0) {
                editor.setHtml(colors.toEditor(viewModel.currentBody()))
                if (caret >= 0) editor.selection = TextRange(caret.coerceAtMost(editor.annotatedString.text.length))
                applied = wanted
            }
        }
    }
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
    // 새로 읽은 본문만 넣음. 같은 판을 다시 넣으면 그 뒤에 친 글이 사라짐
    // 테마가 바뀌면 편집기 안의 색을 저장 값으로 되돌릴 수 없어 그때는 지금 본문을 다시 넣음
    LaunchedEffect(wanted) {
        if (state.bodyVersion > 0 && applied != wanted) {
            rich.setHtml(colors.toEditor(viewModel.currentBody()))
            // 편집기가 다시 낸 모양이 기준
            viewModel.onBodyLoaded(colors.fromEditor(rich.toHtml()))
            // 앱이 종료됐다 돌아와도 쓰던 자리에서 이어 씀
            if (caret >= 0) rich.selection = TextRange(caret.coerceAtMost(rich.annotatedString.text.length))
            applied = wanted
        }
    }
    // 본문을 넣기 전 빈 편집기의 커서가 저장한 자리를 덮지 않게 넣은 뒤부터 기록
    LaunchedEffect(rich) {
        snapshotFlow { if (applied.isEmpty()) null else rich.selection.max }.filterNotNull().collect { caret = it }
    }
    // HTML 변환은 쓰기 직전으로 미루고 여기서는 바뀜 표시만
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
 * 화면 모델을 일기마다 따로 두는 키
 * 앱 틀이 저장소를 나눠 주지 못할 때도 다른 일기의 화면 모델을 받지 않게
 */
private fun WriteTarget.viewModelKey(): String = when (this) {
    is WriteTarget.Edit -> "write:edit:$entryId"
    is WriteTarget.New -> "write:new:$date"
}
