package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.load_failed_body
import org.sjhstudio.diary.core.ui.resources.load_failed_retry
import org.sjhstudio.diary.core.ui.resources.load_failed_title

/**
 * 일기를 읽지 못했을 때 화면 가운데에 보이는 안내.
 * 뼈대나 빈 화면을 계속 두지 않고 까닭과 [다시 시도]를 보인다.
 * 홈 목록, 일기 상세, 달력, 돌아보기, 휴지통이 같이 쓴다.
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
 * 화면 일부(달력 아래 그날 일기처럼)를 읽지 못했을 때의 한 줄 안내.
 * 칸이 작아 화면 전체용 안내 대신 빈 줄과 같은 모양으로 까닭과 [다시 시도]를 둔다.
 */
@Composable
fun LoadFailedLine(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    DiaryEmptyLine(stringResource(Res.string.load_failed_title), stringResource(Res.string.load_failed_retry), onRetry, modifier)
}

/** [다시 시도] 단추 글. 화면 시험이 이 글로 단추를 찾는다. */
@Composable
fun loadFailedRetryLabel(): String = stringResource(Res.string.load_failed_retry)
