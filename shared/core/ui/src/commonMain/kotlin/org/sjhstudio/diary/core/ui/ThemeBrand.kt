package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.sjhstudio.diary.core.designsystem.theme.DiaryBrand
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor

/** 정한 색의 토큰. [ThemeColor.CUSTOM]은 정한 값이 없어 라벤더 */
fun ThemeColor.presetBrand(): DiaryBrand = when (this) {
    ThemeColor.LAVENDER, ThemeColor.CUSTOM -> DiaryBrand.Lavender
    ThemeColor.PLUM -> DiaryBrand.Plum
    ThemeColor.ROSE -> DiaryBrand.Rose
    ThemeColor.MAPLE -> DiaryBrand.Maple
    ThemeColor.FOREST -> DiaryBrand.Forest
    ThemeColor.SEA -> DiaryBrand.Sea
    ThemeColor.BLACK -> DiaryBrand.Black
}

/**
 * 지금 보이는 테마 색(FR-018)
 * 직접 고른 색은 [customAllowed](Plus)이고 값이 바를 때만. 아니면 라벤더로 보이고 저장값은 그대로
 */
fun ThemeChoice.shownColor(customAllowed: Boolean): ThemeColor =
    if (color == ThemeColor.CUSTOM && !(customAllowed && customRgb != null)) ThemeColor.DEFAULT else color

/** 앱이 쓸 토큰. 직접 고른 색은 이때 한 번 계산 */
fun ThemeChoice.brand(customAllowed: Boolean): DiaryBrand {
    val shown = shownColor(customAllowed)
    val rgb = customRgb
    return if (shown == ThemeColor.CUSTOM && rgb != null) DiaryBrand.custom(rgb) else shown.presetBrand()
}

/** 고른 값이 바뀔 때만 다시 계산 */
@Composable
fun rememberThemeBrand(choice: ThemeChoice, customAllowed: Boolean): DiaryBrand =
    remember(choice.colorId, choice.customHex, customAllowed) { choice.brand(customAllowed) }
