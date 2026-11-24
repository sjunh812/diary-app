package org.sjhstudio.diary.feature.settings

import org.jetbrains.compose.resources.StringResource
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.list_length_five
import org.sjhstudio.diary.feature.settings.resources.list_length_full
import org.sjhstudio.diary.feature.settings.resources.list_length_three
import org.sjhstudio.diary.feature.settings.resources.list_style_card
import org.sjhstudio.diary.feature.settings.resources.list_style_feed
import org.sjhstudio.diary.feature.settings.resources.list_style_photo
import org.sjhstudio.diary.feature.settings.resources.lock_value_cold_start
import org.sjhstudio.diary.feature.settings.resources.lock_value_five_minutes
import org.sjhstudio.diary.feature.settings.resources.lock_value_immediately
import org.sjhstudio.diary.feature.settings.resources.lock_value_off
import org.sjhstudio.diary.feature.settings.resources.lock_value_one_minute
import org.sjhstudio.diary.feature.settings.resources.text_size_large
import org.sjhstudio.diary.feature.settings.resources.text_size_medium
import org.sjhstudio.diary.feature.settings.resources.text_size_small
import org.sjhstudio.diary.feature.settings.resources.text_size_xlarge
import org.sjhstudio.diary.feature.settings.resources.text_size_xxlarge
import org.sjhstudio.diary.feature.settings.resources.theme_color_black
import org.sjhstudio.diary.feature.settings.resources.theme_color_forest
import org.sjhstudio.diary.feature.settings.resources.theme_color_lavender
import org.sjhstudio.diary.feature.settings.resources.theme_color_maple
import org.sjhstudio.diary.feature.settings.resources.theme_color_plum
import org.sjhstudio.diary.feature.settings.resources.theme_color_rose
import org.sjhstudio.diary.feature.settings.resources.theme_color_sea
import org.sjhstudio.diary.feature.settings.resources.theme_custom_value
import org.sjhstudio.diary.feature.settings.resources.theme_dark
import org.sjhstudio.diary.feature.settings.resources.theme_light
import org.sjhstudio.diary.feature.settings.resources.theme_system

internal fun BodyTextSize.label(): StringResource = when (this) {
    BodyTextSize.SMALL -> Res.string.text_size_small
    BodyTextSize.MEDIUM -> Res.string.text_size_medium
    BodyTextSize.LARGE -> Res.string.text_size_large
    BodyTextSize.XLARGE -> Res.string.text_size_xlarge
    BodyTextSize.XXLARGE -> Res.string.text_size_xxlarge
}

internal fun ListStyle.label(): StringResource = when (this) {
    ListStyle.FEED -> Res.string.list_style_feed
    ListStyle.CARD -> Res.string.list_style_card
    ListStyle.PHOTO_GRID -> Res.string.list_style_photo
}

internal fun PreviewLength.label(): StringResource = when (this) {
    PreviewLength.THREE_LINES -> Res.string.list_length_three
    PreviewLength.FIVE_LINES -> Res.string.list_length_five
    PreviewLength.FULL -> Res.string.list_length_full
}

internal fun ThemeMode.label(): StringResource = when (this) {
    ThemeMode.SYSTEM -> Res.string.theme_system
    ThemeMode.LIGHT -> Res.string.theme_light
    ThemeMode.DARK -> Res.string.theme_dark
}

/** 테마 색 이름. 설정 줄 값에서는 직접 고른 색을 "직접 고른 색"으로 */
internal fun ThemeColor.label(): StringResource = when (this) {
    ThemeColor.LAVENDER -> Res.string.theme_color_lavender
    ThemeColor.PLUM -> Res.string.theme_color_plum
    ThemeColor.ROSE -> Res.string.theme_color_rose
    ThemeColor.MAPLE -> Res.string.theme_color_maple
    ThemeColor.FOREST -> Res.string.theme_color_forest
    ThemeColor.SEA -> Res.string.theme_color_sea
    ThemeColor.BLACK -> Res.string.theme_color_black
    ThemeColor.CUSTOM -> Res.string.theme_custom_value
}

/**
 * 설정 목록의 잠금 줄 값
 * 꺼져 있으면 "꺼짐", 켜져 있으면 다시 잠그는 때
 */
internal fun LockSettings.label(): StringResource = if (!enabled) {
    Res.string.lock_value_off
} else {
    when (relockAfter) {
        RelockAfter.IMMEDIATELY -> Res.string.lock_value_immediately
        RelockAfter.ONE_MINUTE -> Res.string.lock_value_one_minute
        RelockAfter.FIVE_MINUTES -> Res.string.lock_value_five_minutes
        RelockAfter.COLD_START -> Res.string.lock_value_cold_start
    }
}
