package org.sjhstudio.diary.app.gate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.feature.migration.LegacyReaderScreen
import org.sjhstudio.diary.feature.migration.LegacyReaderViewModel
import org.sjhstudio.diary.feature.migration.MigrationMessage
import org.sjhstudio.diary.feature.migration.MigrationScreen
import org.sjhstudio.diary.feature.migration.MigrationUiState
import org.sjhstudio.diary.feature.migration.MigrationViewModel
import org.sjhstudio.diary.feature.migration.SupportAttachSheet
import org.sjhstudio.diary.feature.migration.message
import org.sjhstudio.diary.feature.migration.rememberMigrationActions

/** 결과·실패 화면의 아래 버튼 위 16. 화면 끝 20 + 큰 버튼 56 + 사이 8 + 글자 버튼 48 위에 뜬다. */
private val ToastAboveButtons =
    DiarySpacing.x5 + DiarySpacing.x14 + DiarySpacing.x2 + DiarySpacing.minTouchTarget + DiarySpacing.x4

/**
 * 기록 이전 관문(contracts/migration-flow.md "화면 상태"). 이전이 없거나 끝났으면 [content]로 간다.
 * 실패 화면에서는 다시 시도하거나, 공간 부족이 아니면 새 일기부터 쓰기로 앱 틀로 갈 수 있다.
 * 그때는 임시 DB로 바꾸지 않고 예전 일기를 지금 DB에 더해 새로 쓴 일기를 지킨다.
 * 실패 화면에서는 예전 일기 보기(읽기 전용), 원본 내보내기, 문의하기를 할 수 있다(FR-009).
 * 잠금이 켜져 있으면 이 관문은 잠금 관문 안쪽에 놓인다(FR-015, 스펙 001 US2).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MigrationGate(content: @Composable () -> Unit) {
    val viewModel = koinViewModel<MigrationViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = remember { DiaryToastState() }
    val scope = rememberCoroutineScope()
    // 관문을 지난 뒤의 알림(FR-011)은 앱 틀 위에 한 번 띄운다.
    LaunchedEffect(viewModel) { viewModel.notices.collect { toast.show(it.message()) } }
    var reading by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (state == MigrationUiState.Passed) {
            content()
        } else if (reading && state is MigrationUiState.Failed) {
            BackHandler { reading = false }
            val reader = koinViewModel<LegacyReaderViewModel>()
            val readerState by reader.state.collectAsStateWithLifecycle()
            LegacyReaderScreen(
                state = readerState,
                currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
                onBack = { reading = false },
                onLoadMore = reader::loadMore
            )
        } else {
            val actions = rememberMigrationActions(MigrationMessage { message -> scope.launch { toast.show(message) } })
            var asking by rememberSaveable { mutableStateOf(false) }
            MigrationScreen(
                state = state,
                onStart = viewModel::acknowledgeResult,
                onRetry = viewModel::retry,
                onSkip = viewModel::skip,
                onViewLegacy = { reading = true },
                onExportOriginal = actions?.exportOriginal,
                // 일부만 옮겨진 기기는 고쳐 받기 시트를 먼저 띄운다(FR-010a). 실패한 기기는 바로 메일을 쓴다.
                onContact = actions?.let { a -> if (state is MigrationUiState.Result) ({ asking = true }) else a.contact }
            )
            if (asking && actions != null) {
                SupportAttachSheet(
                    onDismiss = { asking = false },
                    onContactWithout = {
                        asking = false
                        actions.contact()
                    },
                    onContactWith = {
                        asking = false
                        actions.contactWithAttachment()
                    }
                )
            }
        }
        val bottom =
            when {
                state == MigrationUiState.Passed -> toastBottomOverApp()
                reading && state is MigrationUiState.Failed -> DiarySpacing.x4
                else -> ToastAboveButtons
            }
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom))
    }
}
