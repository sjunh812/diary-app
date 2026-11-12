package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.link_no_browser

/**
 * 주소 열기. 열 앱이 없으면 false
 * 안드로이드는 메일 앱이 없으면 예외로 앱이 꺼지고 iOS는 조용히 아무 일도 하지 않아서 둘 다 결과로 받음
 */
@Composable
expect fun rememberOpenLink(): (String) -> Boolean

/** 웹 주소 열기. 열 앱이 없으면 [toast]로 알림 */
@Composable
fun rememberOpenWebLink(toast: DiaryToastState): (String) -> Unit {
    val open = rememberOpenLink()
    val scope = rememberCoroutineScope()
    val noBrowser = stringResource(Res.string.link_no_browser)
    return remember(open, toast, noBrowser) { { url -> if (!open(url)) scope.launch { toast.show(noBrowser) } } }
}
