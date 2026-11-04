package org.sjhstudio.diary.feature.settings

import org.jetbrains.compose.resources.StringResource
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.feature.settings.resources.Res
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

internal fun ThemeMode.label(): StringResource = when (this) {
    ThemeMode.SYSTEM -> Res.string.theme_system
    ThemeMode.LIGHT -> Res.string.theme_light
    ThemeMode.DARK -> Res.string.theme_dark
}

/** 설정 목록의 잠금 줄 값. 꺼져 있으면 "꺼짐", 켜져 있으면 다시 잠그는 때. */
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
