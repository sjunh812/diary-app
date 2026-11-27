package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.DrawableResource
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.allDrawableResources

/** 화면에 보이는 기분 테마. Plus가 아니면 기본 */
@Immutable
data class ShownMoodLook(val theme: MoodTheme = MoodTheme.DEFAULT, val sticker: Boolean = false)

/** 앱 전체가 같은 테마로 그림. 테마 미리보기는 그 칸 안에서만 바꿔 그림 */
val LocalMoodLook = staticCompositionLocalOf { ShownMoodLook() }

/**
 * 테마 그림(Figma 시안 · 기분 테마 후보의 라이트·다크 판을 3배로 내보냄)
 * 기본 테마는 코드로 그려 null
 * 다크 그림이 따로 있는 테마만 다크 판을 씀
 */
internal fun ShownMoodLook.art(mood: Mood, dark: Boolean): DrawableResource? {
    val set =
        when (theme) {
            MoodTheme.BASIC -> return null
            MoodTheme.SHAPE -> if (sticker) "sticker" else "shape"
            MoodTheme.GOUACHE -> "gouache"
            MoodTheme.LINE -> if (dark) "line_dark" else "line"
            MoodTheme.DOT -> "dot"
            MoodTheme.BUBBLE -> if (dark) "bubble_dark" else "bubble"
            MoodTheme.DOODLE -> if (dark) "doodle_dark" else "doodle"
            MoodTheme.VEGGIE -> "veggie"
            MoodTheme.CLAY -> "clay"
        }
    return Res.allDrawableResources["mood_${set}_${mood.id}"]
}

/** 테마 그림 칸은 기본 얼굴 원보다 큼. Figma 테마 줄에서 기본 얼굴 23, 그림 칸 28 */
internal const val MOOD_ART_SCALE = 28f / 23f
