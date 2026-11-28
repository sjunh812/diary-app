package org.sjhstudio.diary.app.backup

import kotlinx.coroutines.flow.first
import org.sjhstudio.diary.core.backup.ManifestSettings
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeColor
import org.sjhstudio.diary.core.model.ThemeHex
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 백업에 담는 설정과 복원할 때 적용하는 설정
 * 잠금과 백업 설정은 바꾸지 않음(contracts/merge-rules.md "설정")
 */
class BackupSettingsBridge(
    private val settings: SettingsRepository,
    private val lockEnabled: suspend () -> Boolean,
    private val reschedule: suspend () -> Unit
) {
    suspend fun current(): ManifestSettings {
        val now = settings.settings.first()
        return ManifestSettings(
            font = now.font.id,
            bodyTextSize = now.bodyTextSize.id,
            themeMode = now.themeMode.id,
            reminderEnabled = now.reminderEnabled,
            reminderMinuteOfDay = now.reminderMinuteOfDay,
            listStyle = now.list.listStyle.id,
            favoritesOnly = now.list.favoritesOnly,
            previewLength = now.list.previewLength.id,
            showListFormatting = now.list.showFormatting,
            askLocationAgain = now.askLocationAgain,
            lockEnabled = lockEnabled(),
            // 한 번도 고르지 않았어도 라벤더를 적음. 다른 기기에 복원하면 라벤더로 돌아오게
            themeColor = now.theme.colorId ?: ThemeColor.DEFAULT.id,
            customThemeColor = now.theme.customHex,
            // 기분 테마도 한 번도 고르지 않았으면 기본을 적음
            moodTheme = now.moodLook.themeId ?: MoodTheme.DEFAULT.id,
            clayHair = now.moodLook.clayHair,
            claySkin = now.moodLook.claySkin,
            clayGlasses = now.moodLook.clayGlasses,
            shapeSticker = now.moodLook.shapeSticker
        )
    }

    /** 모르는 값은 건너뜀. 알림은 적용 뒤 다시 예약 */
    suspend fun apply(backup: ManifestSettings) {
        DiaryFont.fromId(backup.font)?.let { settings.setFont(it) }
        BodyTextSize.fromId(backup.bodyTextSize)?.let { settings.setBodyTextSize(it) }
        ThemeMode.fromId(backup.themeMode)?.let { settings.setThemeMode(it) }
        applyTheme(backup)
        applyMoodLook(backup)
        ListStyle.idOrNull(backup.listStyle)?.let { settings.setListStyle(it) }
        PreviewLength.idOrNull(backup.previewLength)?.let { settings.setPreviewLength(it) }
        backup.showListFormatting?.let { settings.setShowListFormatting(it) }
        backup.askLocationAgain?.let { settings.setAskLocationAgain(it) }
        val minute = backup.reminderMinuteOfDay
        val enabled = backup.reminderEnabled
        if (enabled != null && minute != null) settings.setReminder(enabled, minute)
        reschedule()
    }

    /**
     * 테마를 한 번에 씀. 나눠 쓰면 앱이 잠깐 다른 색으로 칠해짐
     * 직접 고른 색은 값이 바를 때만 기억하고, 고른 색은 아는 이름일 때만 바꿈. 옛 백업처럼 칸이 없으면 이 기기 값 그대로
     */
    private suspend fun applyTheme(backup: ManifestSettings) {
        val rgb = ThemeHex.parse(backup.customThemeColor)
        val color = ThemeColor.fromId(backup.themeColor)
        if (rgb == null && color == null) return
        settings.updateTheme { now ->
            val remembered = rgb?.let(ThemeHex::format) ?: now.customHex
            val shown = when {
                color == null -> now.colorId

                // 직접 고른 색은 백업에 값이 함께 있을 때만
                color == ThemeColor.CUSTOM && rgb == null -> now.colorId

                else -> color.id
            }
            ThemeChoice(colorId = shown, customHex = remembered)
        }
    }

    /**
     * 기분 테마와 클레이 꼴을 한 번에 씀
     * 아는 값만 바꾸고 옛 백업처럼 칸이 없거나 모르는 값이면 그 칸은 이 기기 값 그대로
     * Plus가 아닌 기기에 Plus 테마가 와도 그대로 두고, 보이는 테마는 Plus 꾸미기 되돌리기가 정함
     */
    private suspend fun applyMoodLook(backup: ManifestSettings) {
        val theme = MoodTheme.fromId(backup.moodTheme)
        val hair = ClayHair.fromId(backup.clayHair)
        val skin = ClaySkin.fromId(backup.claySkin)
        settings.updateMoodLook { now ->
            now.copy(
                themeId = theme?.id ?: now.themeId,
                clayHair = hair?.id ?: now.clayHair,
                claySkin = skin?.id ?: now.claySkin,
                clayGlasses = backup.clayGlasses ?: now.clayGlasses,
                shapeSticker = backup.shapeSticker ?: now.shapeSticker
            )
        }
    }
}
