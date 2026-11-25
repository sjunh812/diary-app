package org.sjhstudio.diary.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.data.percent
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.update_card_default
import org.sjhstudio.diary.feature.home.resources.update_card_downloaded_body
import org.sjhstudio.diary.feature.home.resources.update_card_downloaded_title
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_body
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_body_unknown
import org.sjhstudio.diary.feature.home.resources.update_card_downloading_title
import org.sjhstudio.diary.feature.home.resources.update_card_failed
import org.sjhstudio.diary.feature.home.resources.update_card_keep_required
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
    // 받기를 시작하거나 다 받으면 버튼 줄이 생기고 사라져, 아래 목록이 덜컥 움직이지 않게 높이만 부드럽게
    val resize = if (rememberReduceMotion()) {
        modifier
    } else {
        modifier.animateContentSize(
            tween(DiaryMotion.QUICK_MILLIS, easing = DiaryMotion.decelerate)
        )
    }
    val place = resize.padding(vertical = DiarySpacing.x3)
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
                // 닫을 수 없는 카드만 할 일을 분명히 부탁함
                // 일반 카드는 요약 한 줄이 업데이트할 까닭
                if (notice.required && !notice.failed) {
                    "$first\n${stringResource(Res.string.update_card_keep_required)}"
                } else {
                    first
                },
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

        is UpdateNotice.Downloaded ->
            DiaryStatusBox(
                DiaryIcons.RotateCw,
                stringResource(Res.string.update_card_downloaded_title),
                "${stringResource(Res.string.update_card_downloaded_body)}\n${stringResource(Res.string.update_card_kept)}",
                place,
                actionLabel = stringResource(Res.string.update_card_restart),
                onAction = onRestart,
                // 꼭 필요한 업데이트는 받은 뒤에도 닫을 수 없음
                secondaryLabel = stringResource(Res.string.update_card_later).takeUnless { notice.required },
                onSecondary = onLater,
                secondaryKind = DiaryButtonKind.Text
            )

        UpdateNotice.None -> Unit
    }
}
