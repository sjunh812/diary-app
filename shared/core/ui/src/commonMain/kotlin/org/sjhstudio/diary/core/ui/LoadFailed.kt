package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.load_failed_body
import org.sjhstudio.diary.core.ui.resources.load_failed_retry
import org.sjhstudio.diary.core.ui.resources.load_failed_title

/**
 * 일기를 읽지 못했을 때 화면 가운데에 보이는 안내
 * 뼈대나 빈 화면을 계속 두지 않고 까닭과 [다시 시도]를 보임
 * 홈 목록, 일기 상세, 달력, 돌아보기, 휴지통이 같이 씀
 */
@Composable
fun LoadFailed(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    DiaryEmptyArea(modifier) {
        DiaryEmptyState(
            DiaryIcons.CircleAlert,
            stringResource(Res.string.load_failed_title),
            description = stringResource(Res.string.load_failed_body),
            actionLabel = stringResource(Res.string.load_failed_retry),
            onAction = onRetry
        )
    }
}

/**
 * 화면 일부(달력 아래 그날 일기처럼)를 읽지 못했을 때의 한 줄 안내
 * 칸이 작아 화면 전체용 안내 대신 빈 줄과 같은 모양으로 까닭과 [다시 시도]를 둠
 */
@Composable
fun LoadFailedLine(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    DiaryEmptyLine(stringResource(Res.string.load_failed_title), stringResource(Res.string.load_failed_retry), onRetry, modifier)
}

/** [다시 시도] 버튼 글. 화면 시험이 이 글로 버튼을 찾음 */
@Composable
fun loadFailedRetryLabel(): String = stringResource(Res.string.load_failed_retry)

/**
 * 목록의 다음 쪽(또는 앞 쪽)을 읽지 못하면 그 자리에 [LoadFailedLine]
 * 빈 자리만 남지 않게. 홈 목록 세 꼴과 찾기가 같이 씀
 */
fun LazyListScope.loadMoreFailed(failed: Boolean, onRetry: () -> Unit, key: String = LOAD_MORE_FAILED) {
    if (failed) {
        item(key = key, contentType = LOAD_MORE_FAILED) {
            LoadFailedLine(onRetry, Modifier.padding(horizontal = LocalScreenGutter.current))
        }
    }
}

/** [loadMoreFailed]의 격자판. 한 줄을 다 차지함 */
fun LazyGridScope.loadMoreFailed(failed: Boolean, onRetry: () -> Unit, key: String = LOAD_MORE_FAILED) {
    if (failed) {
        item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = LOAD_MORE_FAILED) {
            LoadFailedLine(onRetry, Modifier.padding(horizontal = LocalScreenGutter.current))
        }
    }
}

private const val LOAD_MORE_FAILED = "load_more_failed"
