package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.ui.DiaryLottie
import org.sjhstudio.diary.feature.welcome.resources.Res

private const val WRITE_MILLIS = 4_000
private const val BACKUP_MILLIS = 3_200

/** 쓰는 그림. 기분을 고르고, 글을 쓰고, 저장한다. 글자가 없어 언어를 타지 않는다. */
@Composable
internal fun WriteArt(active: Boolean, modifier: Modifier = Modifier) = DiaryLottie(
    { Res.readBytes("files/onboarding_write.json") },
    WRITE_MILLIS,
    active,
    modifier.size(DiaryArtSize.width, DiaryArtSize.height)
)

/** 백업 그림. 휴대폰의 글과 사진이 내 드라이브로 올라간다. 휴대폰 쪽 기록은 그대로 남는다. */
@Composable
internal fun BackupArt(active: Boolean, modifier: Modifier = Modifier) = DiaryLottie(
    { Res.readBytes("files/onboarding_backup.json") },
    BACKUP_MILLIS,
    active,
    modifier.size(DiaryArtSize.width, DiaryArtSize.height)
)
