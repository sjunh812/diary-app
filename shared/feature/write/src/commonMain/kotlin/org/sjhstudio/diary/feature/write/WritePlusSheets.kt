package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_checklist_plus_body
import org.sjhstudio.diary.feature.write.resources.write_checklist_plus_title
import org.sjhstudio.diary.feature.write.resources.write_limit_body
import org.sjhstudio.diary.feature.write.resources.write_limit_later
import org.sjhstudio.diary.feature.write.resources.write_limit_plus
import org.sjhstudio.diary.feature.write.resources.write_limit_title

// 쓰기 화면의 Plus 안내 시트

/** 체크리스트는 Plus 기능(Figma P9). 쓰던 글은 그대로라고 말하고 막지 않음 */
@Composable
internal fun ChecklistPlusSheet(onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss,
        title = stringResource(Res.string.write_checklist_plus_title),
        description = stringResource(Res.string.write_checklist_plus_body)
    ) {
        DiaryButtonPair(
            secondary = stringResource(Res.string.write_limit_later),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.write_limit_plus),
            onPrimary = {
                onDismiss()
                onPlus()
            },
            size = DiaryButtonSize.XLarge,
            secondaryKind = DiaryButtonKind.Weak
        )
    }
}

/**
 * 사진 한도 시트(Figma P3)
 * 고른 사진과 글은 그대로라고 먼저 말하고 쓰는 일을 막지 않음
 */
@Composable
internal fun PhotoLimitSheet(limit: Int, onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss,
        title = stringResource(Res.string.write_limit_title, limit),
        description = stringResource(Res.string.write_limit_body, PhotoLimit.PLUS)
    ) {
        DiaryButtonPair(
            secondary = stringResource(Res.string.write_limit_later),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.write_limit_plus),
            onPrimary = {
                onDismiss()
                onPlus()
            },
            size = DiaryButtonSize.XLarge,
            secondaryKind = DiaryButtonKind.Weak
        )
    }
}
