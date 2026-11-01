package org.sjhstudio.diary.app.gate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.feature.migration.MigrationScreen
import org.sjhstudio.diary.feature.migration.MigrationUiState
import org.sjhstudio.diary.feature.migration.MigrationViewModel

/**
 * 기록 이전 관문(contracts/migration-flow.md "화면 상태"). 이전이 없거나 끝났으면 [content]로 간다.
 * 실패한 동안에는 앱 틀로 가지 않는다. 새 일기를 쓰면 다시 시도가 그 일기를 덮을 수 있어서다.
 * 잠금이 켜져 있으면 이 관문은 잠금 관문 안쪽에 놓인다(FR-015, 스펙 001 US2).
 */
@Composable
fun MigrationGate(content: @Composable () -> Unit) {
    val viewModel = koinViewModel<MigrationViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state == MigrationUiState.Passed) {
        content()
    } else {
        // 예전 일기 보기, 원본 내보내기, 문의하기는 스펙 001 T046에서 잇는다.
        MigrationScreen(state = state, onStart = viewModel::acknowledgeResult, onRetry = viewModel::retry)
    }
}
