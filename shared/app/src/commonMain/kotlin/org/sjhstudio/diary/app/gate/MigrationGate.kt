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
import org.sjhstudio.diary.feature.migration.message
import org.sjhstudio.diary.feature.migration.rememberMigrationActions

/**
 * 기록 이전 관문(contracts/migration-flow.md "화면 상태"). 이전이 없거나 끝났으면 [content]로 간다.
 * 실패한 동안에는 앱 틀로 가지 않는다. 새 일기를 쓰면 다시 시도가 그 일기를 덮을 수 있어서다.
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
            MigrationScreen(
                state = state,
                onStart = viewModel::acknowledgeResult,
                onRetry = viewModel::retry,
                onViewLegacy = { reading = true },
                onExportOriginal = actions?.exportOriginal,
                onContact = actions?.contact
            )
        }
        DiaryToastHost(
            toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(
                bottom =
                    DiarySpacing.x14 + DiarySpacing.x10
            )
        )
    }
}
