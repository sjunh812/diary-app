package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow

/**
 * 일기 줄의 사진 칸(`docs/design.md` 사진 칸, Day One): 1장 꽉, 2장 위아래, 3장 큰 칸 + 둘, 4장 이상 2×2와 `+N`.
 * [photos]는 앱 저장소 파일의 절대경로이고, 찾을 수 없는 사진은 null이다(그 자리를 남겨 보인다).
 * 칸의 크기와 모서리는 부르는 쪽(DiaryEntryRow)이 정한다.
 * [more]는 이 칸 뒤에 더 있는 사진 수다. 카드처럼 칸을 나눠 그리는 쪽이 마지막 칸에 넘긴다.
 */
@Composable
fun DiaryPhotoCell(photos: List<String?>, modifier: Modifier = Modifier, more: Int = 0) {
    if (photos.isEmpty()) return
    val extra = if (photos.size > MAX_TILES) photos.size - MAX_TILES + 1 else more
    Box(modifier.fillMaxSize()) {
        when (photos.size) {
            1 -> Photo(photos[0], Modifier.fillMaxSize())

            2 -> Column(Modifier.fillMaxSize()) { photos.forEach { Photo(it, Modifier.weight(1f)) } }

            3 ->
                Row(Modifier.fillMaxSize()) {
                    Photo(photos[0], Modifier.weight(1f))
                    Column(Modifier.weight(1f)) { photos.drop(1).forEach { Photo(it, Modifier.weight(1f)) } }
                }

            else ->
                Column(Modifier.fillMaxSize()) {
                    for (row in 0..1) {
                        Row(Modifier.weight(1f)) { for (col in 0..1) Photo(photos[row * 2 + col], Modifier.weight(1f)) }
                    }
                }
        }
        if (extra > 0) {
            BasicText(
                "+$extra",
                style = DiaryTheme.typography.micro.copy(color = DiaryTheme.colors.onAccent),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(DiarySpacing.x1)
                        .clip(DiaryShapes.r8)
                        .background(DiaryTheme.colors.overlay)
                        .padding(horizontal = DiarySpacing.x1, vertical = DiarySpacing.x0_5)
            )
        }
    }
}

/** 한 칸에 그리는 사진 수. */
private const val MAX_TILES = 4

@Composable
private fun Photo(path: String?, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(DiaryTheme.colors.sunken), contentAlignment = Alignment.Center) {
        if (path == EntryRow.PHOTO_PENDING) {
            // 받는 중인 사진은 잃어버린 사진처럼 보이지 않게 한다.
            DiaryIcon(DiaryIcons.Image, contentDescription = null, tint = DiaryTheme.colors.textTertiary, size = DiaryIconSize.i20)
        } else if (path == null) {
            DiaryIcon(DiaryIcons.ImageOff, contentDescription = null, tint = DiaryTheme.colors.textTertiary, size = DiaryIconSize.i16)
        } else {
            // 목록 칸은 원본이 아니라 썸네일을 읽는다(FR-036, ThumbnailFetcher).
            AsyncImage(
                model = Thumbnail(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
