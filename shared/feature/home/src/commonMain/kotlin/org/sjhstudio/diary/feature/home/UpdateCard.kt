package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.data.percent
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.update_card_default
import org.sjhstudio.diary.feature.home.resources.update_card_downloaded_body
import org.sjhstudio.diary.feature.home.resources.update_card_downloaded_title
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_body
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_body_unknown
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_title
import org.sjhstudio.diary.feature.home.resources.update_card_failed
import org.sjhstudio.diary.feature.home.resources.update_card_keep
import org.sjhstudio.diary.feature.home.resources.update_card_kept
import org.sjhstudio.diary.feature.home.resources.update_card_later
import org.sjhstudio.diary.feature.home.resources.update_card_restart
import org.sjhstudio.diary.feature.home.resources.update_card_title
import org.sjhstudio.diary.feature.home.resources.update_card_title_required
import org.sjhstudio.diary.feature.home.resources.update_card_update

/**
 * 홈 목록 맨 위 새 버전 카드(스펙 009, Figma "새 버전 카드 (S1-c)")
 * 백업·의견 카드와 같은 상자. 받는 중·받음(Android)은 같은 카드가 바뀜
 */
@Composable
fun UpdateCard(notice: UpdateNotice, onUpdate: () -> Unit, onLater: () -> Unit, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    // 위아래 12. 백업·의견 카드와 같은 자리
    val place = modifier.padding(vertical = DiarySpacing.x3)
    when (notice) {
        is UpdateNotice.Available -> {
            val first =
                if (notice.failed) {
                    stringResource(Res.string.update_card_failed)
                } else {
                    notice.note?.textFor(Locale.current.language) ?: stringResource(Res.string.update_card_default)
                }
            DiaryStatusBox(
                DiaryIcons.Download,
                stringResource(if (notice.required) Res.string.update_card_title_required else Res.string.update_card_title),
                if (notice.failed) first else "$first\n${stringResource(Res.string.update_card_keep)}",
                place,
                actionLabel = stringResource(Res.string.update_card_update),
                onAction = onUpdate,
                // 꼭 필요한 업데이트는 닫을 수 없음. 앱은 그대로 씀
                secondaryLabel = stringResource(Res.string.update_card_later).takeUnless { notice.required },
                onSecondary = onLater,
                secondaryKind = DiaryButtonKind.Text
            )
        }

        is UpdateNotice.Downloading -> {
            val percent = notice.percent()
            DiaryStatusBox(
                DiaryIcons.Download,
                stringResource(Res.string.update_card_downloading_title),
                if (percent == null) {
                    stringResource(Res.string.update_card_downloading_body_unknown)
                } else {
                    stringResource(Res.string.update_card_downloading_body, percent)
                },
                place,
                progress = notice.progress ?: 0f
            )
        }

        UpdateNotice.Downloaded ->
            DiaryStatusBox(
                DiaryIcons.Download,
                stringResource(Res.string.update_card_downloaded_title),
                "${stringResource(Res.string.update_card_downloaded_body)}\n${stringResource(Res.string.update_card_kept)}",
                place,
                actionLabel = stringResource(Res.string.update_card_restart),
                onAction = onRestart,
                secondaryLabel = stringResource(Res.string.update_card_later),
                onSecondary = onLater,
                secondaryKind = DiaryButtonKind.Text
            )

        UpdateNotice.None -> Unit
    }
}
