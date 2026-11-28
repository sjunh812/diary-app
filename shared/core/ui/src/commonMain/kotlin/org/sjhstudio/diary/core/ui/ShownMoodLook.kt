package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.DrawableResource
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.allDrawableResources

/** 화면에 보이는 기분 테마. Plus가 아니면 기본 */
@Immutable
data class ShownMoodLook(
    val theme: MoodTheme = MoodTheme.DEFAULT,
    val sticker: Boolean = false,
    val hair: ClayHair = ClayHair.DEFAULT,
    val skin: ClaySkin = ClaySkin.DEFAULT,
    val glasses: Boolean = false
) {
    companion object {
        /** 설정 값과 요금제로 보이는 테마를 정함 */
        fun of(look: MoodLook, plusAllowed: Boolean): ShownMoodLook =
            ShownMoodLook(look.shownTheme(plusAllowed), look.sticker, look.hair, look.skin, look.glasses)
    }
}

/** 앱 전체가 같은 테마로 그림. 테마 미리보기는 그 칸 안에서만 바꿔 그림 */
val LocalMoodLook = staticCompositionLocalOf { ShownMoodLook() }

/**
 * 테마 그림(Figma 시안 · 기분 테마 후보의 라이트·다크 판, 72칸을 240px로 내보냄)
 * 기본 테마는 코드로 그려 null
 * 다크 그림이 따로 있는 테마만 다크 판을 씀
 */
internal fun ShownMoodLook.art(mood: Mood, dark: Boolean): DrawableResource? {
    val set =
        when (theme) {
            // 기본은 코드로, 클레이는 겹으로 그림([clayLayers])
            MoodTheme.BASIC, MoodTheme.CLAY -> null

            MoodTheme.SHAPE -> if (sticker) "sticker" else "shape"

            MoodTheme.GOUACHE -> "gouache"

            MoodTheme.LINE -> if (dark) "line_dark" else "line"

            MoodTheme.DOT -> "dot"

            MoodTheme.BUBBLE -> if (dark) "bubble_dark" else "bubble"

            MoodTheme.DOODLE -> if (dark) "doodle_dark" else "doodle"

            MoodTheme.VEGGIE -> "veggie"
        }
    return set?.let { Res.allDrawableResources["mood_${it}_${mood.id}"] }
}

/** 테마 그림 칸은 기본 얼굴 원보다 큼. Figma 테마 줄에서 기본 얼굴 23, 그림 칸 28 */
internal const val MOOD_ART_SCALE = 28f / 23f

/**
 * 테마 그림이 얼굴 칸 밖으로 한쪽에 넘치는 폭
 * 얼굴을 늘어놓을 때 사이에 두 배를 더해야 그림끼리 보이는 간격이 맞음
 */
fun moodArtBleed(face: Dp): Dp = face * (MOOD_ART_SCALE - 1f) / 2

/** 클레이 얼굴 한 겹. [tint]가 있으면 흰 그림에 그 색을 곱함 */
internal data class ClayLayer(val art: DrawableResource, val tint: Tint?)

internal enum class Tint { HAIR, SKIN, NOSE }

/** 뒤 머리 → 얼굴 피부 → 코 → 앞머리 → 표정 → 안경 차례. 머리 모양마다 없는 겹은 건너뜀 */
internal fun ShownMoodLook.clayLayers(mood: Mood): List<ClayLayer> {
    val all = Res.allDrawableResources
    return listOfNotNull(
        all["clay_back_${hair.id}"]?.let { ClayLayer(it, Tint.HAIR) },
        all["clay_skin_${hair.id}"]?.let { ClayLayer(it, Tint.SKIN) },
        all["clay_nose"]?.let { ClayLayer(it, Tint.NOSE) },
        all["clay_front_${hair.id}"]?.let { ClayLayer(it, Tint.HAIR) },
        all["clay_face_${mood.id}"]?.let { ClayLayer(it, null) },
        // 쿨은 표정에 선글라스가 있어 안경을 겹치지 않음
        all["clay_glasses"]?.takeIf { glasses && mood != Mood.COOL }?.let { ClayLayer(it, null) }
    )
}
