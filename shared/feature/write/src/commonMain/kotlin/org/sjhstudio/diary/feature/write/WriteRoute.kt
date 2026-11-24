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
    // 도구 줄은 누를 때의 색을 읽음. 색이 바뀌어도 도구를 다시 만들지 않아 열어 둔 팔레트가 닫히지 않게
    val currentColors by rememberUpdatedState(colors)
    // 본문의 주인은 화면 모델(임시 저장본) 하나. 편집기는 화면 저장 상태에 글을 넣지 않음
    // 넣으면 앱이 종료됐다 돌아올 때 임시 저장본과 다른 글이 보이고, 아주 긴 글은 저장 상태 크기 한도를 넘음
    // 커서 자리만 저장해 회전 뒤에도 쓰던 자리에서 이어 씀
    var caret by rememberSaveable { mutableIntStateOf(-1) }
    // 편집기는 화면 모델이 읽은 본문 판마다 새로 만들고, 만들 때 본문을 넣음
    // 처음 그리는 순간부터 글이 들어 있어 빈 편집기가 비치지 않고, 커서도 배치된 글 안에 놓임
    // 0판은 아직 읽는 중이라 빈 편집기
    val editor =
        remember(state.bodyVersion) {
            val rich = RichTextState()
            if (state.bodyVersion > 0) rich.loadBody(colors.toEditor(viewModel.currentBody()), caret, writing = !state.isEdit)
            LoadedEditor(rich, state.bodyVersion, colors)
        }
    val rich = editor.rich
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
    LaunchedEffect(editor) {
        if (editor.version == 0) return@LaunchedEffect
        // 편집기가 다시 낸 모양이 기준. 태그 모양만 다른 레거시 글을 고친 것으로 치지 않음
        viewModel.onBodyLoaded(colors.fromEditor(rich.toHtml()))
        // 본문을 넣은 편집기의 커서만 기록. 읽는 중의 빈 편집기는 저장한 자리를 덮음
        snapshotFlow { rich.selection.max }.collect { caret = it }
    }
    // 테마가 바뀌면 편집기 안의 글자색을 새 색으로 바꿔 다시 넣음. 친 글은 그대로
    LaunchedEffect(editor, colors) {
        if (editor.version == 0 || editor.colors == colors) return@LaunchedEffect
        // 고르던 범위까지 그대로 돌려줌
        val selection = rich.selection
        rich.loadBody(colors.toEditor(editor.colors.fromEditor(rich.toHtml())), selection.max, writing = true)
        val end = rich.annotatedString.text.length
        rich.selection = TextRange(selection.start.coerceAtMost(end), selection.end.coerceAtMost(end))
        editor.colors = colors
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
        actions = remember(viewModel, rich) { WriteActions(viewModel, rich, { currentColors }, viewModel::mediaPath, onPlus) },
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

/**
 * 본문을 넣고 커서를 [caret]에 둠. 저장한 자리가 없으면(-1) [writing]으로 정함
 * 바로 쓰는 새 글·이어 쓰기([writing])는 글 끝에서 이어 씀
 * 먼저 읽는 수정은 글 맨 앞. 글 끝이면 처음 누를 때 입력칸이 누른 자리보다 옛 커서를 먼저 보이려고 맨 아래까지 스크롤함
 */
internal fun RichTextState.loadBody(html: String, caret: Int, writing: Boolean) {
    setHtml(html)
    val end = annotatedString.text.length
    selection = TextRange(
        when {
            caret >= 0 -> caret.coerceAtMost(end)
            writing -> end
            else -> 0
        }
    )
}

/** 화면 모델의 [version]판 본문을 넣은 편집기. [colors]는 편집기 안 글자색을 만든 테마 */
private class LoadedEditor(val rich: RichTextState, val version: Int, var colors: EditorColors)
