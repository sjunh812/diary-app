package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_back
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

/**
 * 두 칸의 오른쪽 첫 화면이면 true
 * 왼쪽 목록이 옆에 있어 돌아갈 곳이 없으니 뒤로 단추를 두지 않음
 */
val LocalTopBarBackHidden = compositionLocalOf { false }

/** 상단 바 오른쪽 글자 동작(저장 같은). 채운 버튼이 아니라 글자 버튼 */
class DiaryTopBarAction(val label: String, val enabled: Boolean = true, val danger: Boolean = false, val onClick: () -> Unit)

/**
 * 상단 바(`docs/design.md` 컴포넌트 규격, SEED TopNavigation 56)
 * 높이 56(글자가 커지면 늘어남), 가운데 제목 headline, 왼쪽 뒤로 가기 칸 48, 오른쪽은 글자 동작
 * 앱 틀 맨 위 화면(설정 같은)은 상단 바 대신 큰 제목을 씀
 */
@Composable
fun DiaryTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, action: DiaryTopBarAction? = null) {
    val colors = DiaryTheme.colors
    Layout(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = DiarySpacing.x14).padding(vertical = DiarySpacing.x0_5),
        content = {
            Box {
                if (onBack != null && !LocalTopBarBackHidden.current) TopBarBack(onBack)
            }
            BasicText(
                text = title,
                style = DiaryTheme.typography.headline.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Box {
                if (action != null) {
                    // 글자 버튼 안쪽이 12라 글자 끝이 화면 여백에 맞음
                    DiaryTopBarTextButton(
                        action.label,
                        action.onClick,
                        Modifier.padding(end = LocalScreenGutter.current - DiarySpacing.x3),
                        enabled = action.enabled,
                        danger = action.danger
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val back = measurables[0].measure(loose)
        val end = measurables[2].measure(loose)
        // 제목은 가운데에 두되 넓은 쪽 칸만큼 양쪽을 비움
        val side = maxOf(back.width, end.width, DiarySpacing.x14.roundToPx())
        val middle = measurables[1].measure(loose.copy(maxWidth = (constraints.maxWidth - side * 2).coerceAtLeast(0)))
        val height = maxOf(constraints.minHeight, back.height, middle.height, end.height)
        layout(constraints.maxWidth, height) {
            back.placeRelative(0, (height - back.height) / 2)
            middle.placeRelative((constraints.maxWidth - middle.width) / 2, (height - middle.height) / 2)
            end.placeRelative(constraints.maxWidth - end.width, (height - end.height) / 2)
        }
    }
}

@Composable
private fun TopBarBack(onBack: () -> Unit) {
    val colors = DiaryTheme.colors
    val back = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .padding(start = DiarySpacing.x1)
                .size(DiarySpacing.minTouchTarget)
                .diaryPress(back, colors.textPrimary, CircleShape)
                .clip(CircleShape)
                .clickable(interactionSource = back, indication = null, role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(DiaryIcons.ChevronLeft, contentDescription = stringResource(Res.string.ds_back), tint = colors.textPrimary)
    }
}

/**
 * 맨 위 줄 오른쪽의 글자 버튼(저장, 건너뛰기, 달력의 오늘 같은)
 * `headline` · `accentText` · 좌우·위아래 12 · 알약 모양 누름 면, 할 수 없으면 38%.
 * 상단 바, 쓰기 화면, 탭 상단 바가 모두 이것을 써 위 줄 글자 버튼이 한 모양
 * 글자 끝을 화면 여백에 맞추는 바깥 여백은 부르는 쪽이 정함
 */
@Composable
fun DiaryTopBarTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val colors = DiaryTheme.colors
    val press = remember { MutableInteractionSource() }
    BasicText(
        text = label,
        style = DiaryTheme.typography.headline.copy(color = if (danger) colors.danger else colors.accentText),
        maxLines = 1,
        modifier =
            modifier
                .alpha(if (enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
                .diaryPress(press, colors.textPrimary, CircleShape)
                .clip(CircleShape)
                .clickable(interactionSource = press, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(DiarySpacing.x3)
    )
}
